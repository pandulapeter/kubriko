<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Keep a type write that fails queued with everything after it, retry it with backoff, report what failed by name, and reload from disk only when no type change was queued during the read.

**Challenged:** amended — the batch being written counts as unwritten (an in-flight list), so awaitWrites and reload no longer see "nothing pending" mid-write; a failed batch keeps its Overwrites; awaitWrites tracks the last fully written generation; builds on 54aa67a82's finally; WorkspaceControllerTest added to Files.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Findings:** INF-2 = ARCH-3 = CMD-6 (pipeline part), CMD-7  ·  **Depends on:** K12 (soft: transient Windows refusals no longer reach this path)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/BlockTypesController.kt`, `workspace/PendingWrite.kt`, new `workspace/FailedWrite.kt`, `workspace/WorkspaceController.kt` (the `onSaveFailed` callback only), `creator/src/main/composeResources/values/strings.xml`, tests `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/BlockTypesControllerTest.kt`, `testing/FakeBlockTypeRepository.kt` (where T03 moved it; keep its contract test passing), `commands/CommandTesting.kt` (the `blockTypesController` helper's callback), `workspace/WorkspaceControllerTest.kt` (or the file T03 moved `aFailedActorTypeSaveIsReportedInTheConsole` to: it imports `status_actor_type_not_saved`, which this plan removes), `creator/CLAUDE.md` (the `BlockTypesController` description in the `workspace` row; grep for `awaitWrites`, since that file is being restructured)

## Problem

`workspace/BlockTypesController.kt`, `writePending()`:

```kotlin
        if (pendingWrites.isEmpty()) return
        val batch = pendingWrites.toList()
        pendingWrites.clear()
        ...
                try {
                    PendingWrite.withoutOverwrittenSaves(batch).forEach { write ->
                        when (write) {
                            is PendingWrite.Save -> repository.save(write.definition)
                            is PendingWrite.Delete -> repository.delete(write.name)
                            ...
                        }
                    }
                    if (canRegenerate) { repository.regenerate(...).also { buildFileChanges = syncBuildFiles() } } else { ... }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    null
                }
        ...
        } finally {
            pendingWriteCount.update { it - batch.size }
        }
        if (regenerated == null) {
            onSaveFailed()
```

- The batch is taken off the queue before it is written; the first failing write aborts the rest of the batch, the regeneration, and the build file sync, and nothing is queued again. Memory and disk then disagree until the user happens to change the same type again; if they never do, the edit is lost when Creator closes.
- A failed `Delete` leaves the file, so the next `reload()` (watcher) brings the deleted type back.
- `rename` queues `Save(new)`, the referencing types' saves, then `Delete(old)`; a failure part-way leaves a partial rename on disk.
- The report is `WorkspaceController`'s `onSaveFailed = { appendToConsole(ConsoleEntry.Status(Res.string.status_actor_type_not_saved, isError = true)) }`, `"An Actor type couldn't be saved."`: no name, no reason, wrong for Manager types, folders, and generated code.
- CMD-7: `reload()` checks `pendingWriteCount.value > 0` before and after the load on IO. A write queued *and finished* during the load also leaves the count at 0, so the older loaded types replace newer in-memory ones and a `Regenerate` is queued from them, reverting the user's edit in memory.

Threading: `scope` is the workspace scope (Main/EDT); `pendingWrites`, the flags, `queue()`, and every callback run on that one thread, only the repository calls inside `withContext(ioDispatcher)` run elsewhere. Keep it so: no new locks, no state touched from IO.

## Fix

1. **New `workspace/FailedWrite.kt`**: `internal data class FailedWrite(val subject: Subject, val reason: String)` with `sealed interface Subject { data class Type(val name: String); data class Folder(val path: String); data object GeneratedCode }`. KDoc: what the write pipeline couldn't write, and why (the exception's first message line, as `BlockTypeStore.reasonOf` does; reuse a shared helper if one exists, otherwise `exception.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: exception::class.simpleName.orEmpty()`).
2. **`PendingWrite`** gets `val subject: FailedWrite.Subject?` (Save/Delete → `Type(name)`, folders → `Folder(path)`, `Regenerate`/`Overwrite` → null).
3. **`writePending()`** becomes stop-at-first-failure:
   - On Main: `val batch = PendingWrite.withoutOverwrittenSaves(pendingWrites.toList()); pendingWrites.clear(); inFlight = batch`; remember `haveTypesChanged`. `private var inFlight: List<PendingWrite> = emptyList()` holds the batch until it is back on Main (cleared in a `finally`, success or not). **The in-flight batch is unwritten**: everything below that asks "is anything pending" (`reloadFromDisk`, `reload`, the deferred reload, the count, `unsavedNames`) asks `hasUnwritten = pendingWrites.isNotEmpty() || inFlight.isNotEmpty()`, never `pendingWrites` alone. Today's `pendingWriteCount` gives that for free (decremented only after the IO block); dropping it without `inFlight` would let `reload()` start during a batch, read the old file of a type being saved, and put it over the newer one in memory (the CMD-7 revert, made more likely), and let `awaitWrites()` return before the batch is on disk.
   - On IO: write `batch` in order; return `BatchResult(writtenCount, failure: Pair<PendingWrite?, Exception>?, regenerated, buildFileChanges)`. Regenerate and sync only when every write succeeded (and `canRegenerate`); an exception from `regenerate`/`syncBuildFiles` is a failure with subject `GeneratedCode` and the writes stay written.
   - Back on Main, on failure: put the unwritten tail (`batch.drop(writtenCount)`, plus every `Overwrite` of `batch.take(writtenCount)`, plus a `Regenerate` when only regeneration failed) back at the **front** of `pendingWrites`. `Overwrite` and `Regenerate` are no-ops in the write loop and take effect only in the regeneration, which a failed batch skipped, so an `Overwrite` before the failing write must survive or "discard changes made by hand" is silently lost (`pendingWrites.addAll(0, tail)`; writes queued meanwhile stay after it, and the next `withoutOverwrittenSaves` merges them), restore `havePendingTypesChanged ||= haveTypesChanged`, set `_failure.value = FailedWrite(...)`, report through `onSaveFailed(failure)` only when it differs from the previous failure (same subject and reason → one report), and schedule a retry.
   - On success after a failure: `_failure.value = null`, call `onSavedAgain()`.
   - Keep INF-1's loop guard and the deferred reload exactly where 54aa67a82 put it: in the `finally` around the reporting callbacks (`onSaveFailed`/`onSavedAgain`/`onSaved` and the leftover reports all go inside that `try`), condition `isReloadRequested && !hasUnwritten`. So the reload request is honoured only once nothing is unwritten; while a write keeps failing, an outside change to the types is read only after it succeeds (and the retried write wins over an outside change to the same file, as a pending write does today). Say so in the `reloadFromDisk` KDoc.
4. **Counting**: replace `pendingWriteCount` with `unwrittenCount: MutableStateFlow<Int>` = `pendingWrites.size + inFlight.size`, updated in `queue()`, when a batch is taken, and when it comes back (it counts writes not yet on disk, failed and in-flight ones included; K01b's `awaitWritten` waits for it to reach 0). Add `val failure: StateFlow<FailedWrite?>` (public to the package; K01b uses it) and `fun unsavedNames(): List<String>`: the distinct `Type` subjects of `inFlight + pendingWrites`, sorted.
5. **Retry**: a `private var retryJob: Job?`. After a failure, `retryJob = scope.launch { delay(RETRY_DELAYS[min(attempt, last)]); writeRequests.trySend(Unit) }` with `RETRY_DELAYS = longArrayOf(2_000, 10_000, 30_000)` (30 s repeating), `attempt` counting consecutive failures and reset on success. `queue()` cancels `retryJob` (the new change triggers an attempt at once). The retry is cancelled with the scope on close; a loop iteration that throws stays guarded by INF-1's catch.
6. **`awaitWrites()`** keeps its `suspend fun awaitWrites()` signature in this plan but must not hang while blocked: it returns once every write queued before the call is on disk **or** the latest attempt that included all of them failed. Implement with generations: `private var queuedGeneration = 0L` (incremented in `queue()`), `private var takenGeneration = 0L` (the `queuedGeneration` captured when a batch is taken), `private val attempted = MutableStateFlow(0L)` (set to `takenGeneration` after every batch, failed or not) and `private var writtenGeneration = 0L` (set to `takenGeneration` only after a batch that wrote and regenerated everything). `awaitWrites()`: `val wanted = queuedGeneration; attempted.first { it >= wanted }` with no `pendingWrites.isEmpty()` shortcut (it would return during an in-flight batch; when nothing was queued since the last batch, `attempted` already equals `wanted`, and before the first batch it waits for the initial load and regeneration, as `SceneEditingController.awaitWrites`' KDoc expects). A requeued tail doesn't raise `queuedGeneration`, so the retry's batch covers the same generation. K01b returns `writtenGeneration >= wanted` from it.
7. **CMD-7, `reload()`**: add `private var typeGeneration = 0L`, incremented in `queue()` only for writes that change definitions or folders (`haveTypesChanged` or a folder write; not `Regenerate`/`Overwrite`). `reload()`:
   ```kotlin
   repeat(RELOAD_ATTEMPTS) {               // 5
       if (hasUnwritten) { isReloadRequested = true; return }
       val generation = typeGeneration
       val loaded = (load on IO, as now; on exception return)
       if (generation == typeGeneration && pendingWrites.none { it.subject is FailedWrite.Subject.Type || it.subject is FailedWrite.Subject.Folder }) { apply as now; return }
   }
   isReloadRequested = true
   ```
   A `Regenerate` queued during the load no longer makes the reload skip, so `DiscardChanges`' `reloadTypes()` lands its types (the KDoc's promise); a type change queued during the load makes it read again. `reloadFromDisk()` stays `if (pending) isReloadRequested = true else scope.launch { reload() }`.
8. **Constructor**: `onSaveFailed: (FailedWrite) -> Unit` and new `onSavedAgain: () -> Unit = {}`. `WorkspaceController` reports:
   - `Type(name)` → `status_block_type_not_saved` "%1$s couldn't be saved: %2$s"
   - `Folder(path)` → `status_type_folder_not_saved` "The folder %1$s couldn't be changed: %2$s"
   - `GeneratedCode` → `status_generated_code_not_written` "The generated code couldn't be written: %1$s"
   - `onSavedAgain` → `status_block_types_saved_again` "Kubriko Creator could write its changes again." (not an error)
   Remove `status_actor_type_not_saved` from strings.xml and its import. Keep `WorkspaceController.kt` under the 500-line limit (`check_sizes.py`); if it would cross, move the four reports into a private `reportFailedWrite(FailedWrite)` function rather than inline lambdas.
9. KDoc of `BlockTypesController`: replace the batch paragraph's end with "A write that fails stays queued with everything after it, in order (so a renamed type's old file is deleted only once the new ones are written, and a deleted type isn't read back), and is tried again with the next change, or after a while; nothing is regenerated until it succeeds." and `reload`'s KDoc: "…returning once they are in memory, unless a change of a type was queued while they were read, which is then read again."
10. `creator/CLAUDE.md`: in the `BlockTypesController` description, add the failure behaviour in one sentence (stays queued, retried, named in the console, nothing regenerated meanwhile). Don't touch other sections (F9 is restructuring the file).

## Tests

`FakeBlockTypeRepository` gains `val failingNames = mutableSetOf<String>()` (save/delete of such a name throws `IOException("Locked.")`) and `var regenerateFailure` already exists. Keep `isFailing` working. In `BlockTypesControllerTest` (virtual time, `writeDelayInMilliseconds = 100`):
- `aFailedSaveKeepsItAndTheRestQueued`: `put(a); put(b); put(c)` with `failingNames = {"b"}`; advance → `writes` = `[save a]`, no `regenerate`, one `onSaveFailed` with `Type("b")` and reason `Locked.`; clear `failingNames`, advance 2 s → `writes` ends `[save b, save c, regenerate]`, `onSavedAgain` called once.
- `theSameFailureIsReportedOnce`: keep `b` failing across three retries (advance 2 s + 10 s + 30 s) → one report.
- `aNewChangeRetriesAtOnce`: after a failure, `put(d)` → attempt within `writeDelayInMilliseconds`, not after 2 s.
- `aFailedDeleteIsNotReadBack`: `delete("Ball")` failing, then `reloadFromDisk()` (the fake still has Ball) → `controller["Ball"] == null` after advancing; after recovery the reload runs and Ball stays deleted on disk and in memory.
- `aRenameDeletesTheOldFileLast`: rename Ball→Puck with `failingNames = {"Puck"}` → `writes` has no `delete Ball`; after recovery `[save Puck, …, delete Ball]` in that order.
- `aFailedRegenerationIsRetried`: `regenerateFailure` set once → `GeneratedCode` reported, saves not repeated, `regenerate` attempted again on retry.
- `awaitWritesReturnsWhileBlocked`: with a failing save, `withTimeout(1_000) { awaitWrites() }` returns (virtual time).
- CMD-7, `aTypeWrittenDuringTheReloadIsNotOverwritten`: an `ioDispatcher` that is a `StandardTestDispatcher`, a fake whose `load()` suspends? (`load` is blocking; use a `CountDownLatch`-free approach: give the fake `onLoad: () -> Unit` that calls `controller.put(edited)` the first time, simulating a write queued during the load) → after `reload()` the in-memory type is `edited`, and `load` ran twice.
- `aRegenerationQueuedDuringTheReloadDoesNotStopIt`: `onLoad` calls `controller.regenerate()` → the reloaded types are applied in the first round.
- `anOverwriteBeforeAFailedSaveIsKept`: `discardChangesMadeByHand("Ball")`, then `put(wall)` with `failingNames = {"Wall"}` in the same batch; recover → `writes` ends `[save Wall, regenerate overwriting Ball]`.
- `aReloadDuringABatchWaitsForIt`: an `ioDispatcher` that is a `StandardTestDispatcher`; `put(ball.copy(width = 3f))`, let the batch start but not finish, change `repository.stored["Ball"]` to the old Ball and call `reloadFromDisk()` → after advancing, `controller["Ball"]!!.width == 3f` and the reload ran after the batch; `awaitWrites()` started mid-batch returns only after the batch's `save Ball`.
- Existing tests that assert `failedCount` keep passing with the new callback type; `WorkspaceControllerTest.aFailedActorTypeSaveIsReportedInTheConsole` asserts `status_block_type_not_saved` with `["Player", "Disk full."]` (the initial regeneration's failure comes before `clearConsole()`).
