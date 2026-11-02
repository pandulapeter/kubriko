<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Retry the move of writeTextAtomically about five times with a short backoff when Windows refuses it for a moment.

**Challenged:** amended — Windows reports a sharing violation (`ERROR_SHARING_VIOLATION`, e.g. an antivirus scanning the just-written temporary file) as a plain `FileSystemException`, not `AccessDeniedException` (`sun.nio.fs.WindowsException` maps only `ERROR_ACCESS_DENIED` to that), so the retry also covers exceptions whose class is exactly `FileSystemException`; its subclasses (`NoSuchFileException`, `FileAlreadyExistsException`, …) still fail at once.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Windows (code runs everywhere)
**Findings:** INF-9 (part c, AccessDenied)  ·  **Depends on:** none (lane K's first plan: K01a's failure path builds on it)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/foundation/AtomicFileWrites.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/foundation/AtomicFileWritesTest.kt`

## Problem

`foundation/AtomicFileWrites.kt` moves the temporary file over the target once:

```kotlin
        temporary.writeText(text)
        try {
            Files.move(path, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(path, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        temporary.delete()
    }
```

On Windows, `Files.move(..., REPLACE_EXISTING)` throws `java.nio.file.AccessDeniedException` while another process holds the target open without `FILE_SHARE_DELETE` (IntelliJ's indexer, an antivirus scan, the running game reading a Scene). That lasts milliseconds, but today it turns into a failed write: a type definition the write pipeline reports as not saved, a Scene command rejected, settings silently not saved. Every caller of `writeTextAtomically` (BlockTypeStore, SceneStore, SceneDescriptions, KubrikoBuildFiles, the JSON stores) is affected.

## Fix

The user decided (decisions.md, "Windows move retry: about 5 tries with backoff").

- Extract the move into an internal function with an injectable mover, for tests:
  ```kotlin
  internal fun moveReplacing(from: Path, to: Path, move: (Path, Path, Array<CopyOption>) -> Unit = { source, target, options -> Files.move(source, target, *options) }, sleep: (Long) -> Unit = Thread::sleep)
  ```
  It tries `ATOMIC_MOVE + REPLACE_EXISTING`, falling back to `REPLACE_EXISTING` alone on `AtomicMoveNotSupportedException` (remember the fallback for the later tries so it isn't attempted atomically again), and on a refusal (`AccessDeniedException`, or an exception whose class is exactly `java.nio.file.FileSystemException`: `exception is AccessDeniedException || exception.javaClass == FileSystemException::class.java`) sleeps `10L shl attempt` ms (10, 20, 40, 80) and tries again, five tries in all (≈150 ms worst case). After the fifth refusal it rethrows the last one. Any other `IOException`, `FileSystemException` subclasses included, is rethrown at once. A rename within one folder needs no space, so a full disk shows up in `temporary.writeText`, before the move, and is never retried; a permanent refusal (a read-only target) costs the 150 ms once per write.
- `writeTextAtomically` calls `moveReplacing(path, target.toPath())`; the `finally { temporary.delete() }` stays, so a final failure leaves no temporary file.
- The function blocks (it already did); every caller already runs it off the main thread (the settings stores at startup excepted, where 150 ms once is acceptable). Windows raises `AccessDeniedException` for `ERROR_ACCESS_DENIED` (the target held open without `FILE_SHARE_DELETE`) and a plain `FileSystemException` with a localized "being used by another process" reason for `ERROR_SHARING_VIOLATION`; match the class, never the message.
- KDoc of `writeTextAtomically`: add "A move Windows refuses because another program has the file open is tried again a few times first." Keep `moveReplacing` internal with a one-line KDoc.
- `SceneStore.move` (a plain `Files.move` without replace, which returns false on failure) is left alone here; a Scene rename that fails is reported and can be retried by the user.

## Tests

In `AtomicFileWritesTest` (pure, no Windows needed, through the `move`/`sleep` parameters):
- `aMoveRefusedTwiceIsTriedAgain`: a mover that throws `AccessDeniedException` twice and then calls `Files.move`; the target holds the new text, the recorded sleeps are `[10, 20]`, and no `*.creator-write.tmp` file is left in the folder.
- `aMoveRefusedFiveTimesFails`: always `AccessDeniedException`; `assertFailsWith<AccessDeniedException>`, five attempts, sleeps `[10, 20, 40, 80]`, the old target content unchanged, no temporary file left (call `writeTextAtomically` through an internal overload, or test `moveReplacing` plus a separate check that `writeTextAtomically` deletes the temporary file on any thrown exception).
- `aSharingViolationIsTriedAgain`: a mover throwing `FileSystemException(from.toString(), null, "The process cannot access the file because it is being used by another process")` once, then moving → the target holds the new text, sleeps `[10]`.
- `otherFailuresAreNotRetried`: a mover throwing `IOException("disk full")` is called once, and one throwing `NoSuchFileException` too.
- `aFileSystemWithoutAtomicMovesFallsBackOnce`: first call throws `AtomicMoveNotSupportedException`, then `AccessDeniedException`, then succeeds; the later calls don't pass `ATOMIC_MOVE`.
