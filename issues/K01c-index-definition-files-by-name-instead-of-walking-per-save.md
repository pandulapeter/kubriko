<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Find a type's definition files through an index that load builds and save and delete keep, instead of walking the definitions folder on every write.

**Challenged:** amended — the repository's calls are not one at a time (a watcher `reload()`'s `load()` can run on IO while a batch's `save`/`delete` does), so the index is guarded by synchronization instead of an assumed confinement.

**Kind:** performance  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** ACT-9 = ARCH-10 (part a)  ·  **Depends on:** K01a (same test fake; order only)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockTypeStore.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockTypeStoreTest.kt` (find with `ls creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors`), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/benchmark/CreatorBenchmarkTest.kt` (measurement only)

Lane G's G06 (validate Property names at load) also edits `BlockTypeStore.load`; merge the two by hand if both are in flight.

## Problem

`actors/BlockTypeStore.kt`:

```kotlin
        // A type moved to another folder leaves its old file behind otherwise.
        definitionFiles(definition.kind).filter { it.nameWithoutExtension == definition.name && it != file && it !in unreadableFiles }.forEach { it.delete(); ownWrites.record(it) }
...
    override fun delete(name: String) {
        BlockTypeKind.entries.forEach { kind -> definitionFiles(kind).filter { it.nameWithoutExtension == name && it !in unreadableFiles }.forEach { it.delete(); ownWrites.record(it) } }
...
    private fun definitionFiles(kind: BlockTypeKind) = definitionsDirectory(kind).walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
```

Every save walks the definitions folder of its kind and every delete walks both. Renaming a type that many others reference saves them all in one batch: O(n·m) file-system walks on the writer.

Drop this plan if `CreatorBenchmarkTest` (add a case: save 200 types, each in a project of 200 definition files) shows that batch under 50 ms; then commit only the benchmark case and report the measurement instead of the index.

## Fix

- `private val filesByName = HashMap<BlockTypeKind, HashMap<String, MutableSet<File>>>()`, `private var isIndexed = false`, guarded by the store itself: `load`, `save`, and `delete` are `@Synchronized` (they all run on IO). They are not called one at a time today: `BlockTypesController.reload()` (the watcher's, or `DiscardChanges`') runs `repository.load()` on IO while a write batch that started during that load runs `save`/`delete` on another IO thread, so a plain `HashMap` mutated by both would race (a rebuild losing a save's entry, or a `ConcurrentModificationException`). Holding the lock across `load`'s walk only delays a concurrent save by that walk.
- `load()` (which walks anyway) rebuilds the index from what it found, unreadable files included, and sets `isIndexed`.
- `definitionFilesNamed(kind, name): Set<File>`: from the index when indexed, otherwise the walk as now (filtered by name), so a store used before `load()` behaves as today.
- `save`: delete the files of the same name other than `file` from the index set (as the walk did), then add `file`. `delete`: the indexed files of `name` for both kinds, then drop them from the index.
- `createFolder`/`deleteFolder` don't touch definition files; leave them.
- External changes: the watcher's `reloadFromDisk` → `load()` rebuilds the index. A file of the same name copied into another folder between a load and a save is no longer deleted by that save; the next load then reports both files as the same name held twice (`BlockTypeFileProblem`), which is visible, not silent. Say so in the KDoc of `save`.

## Tests

In `BlockTypeStoreTest` (real temporary folders):
- `aMovedTypeLeavesNoOldFile`: save `Ball` in folder `a`, then in folder `b` → only `b/Ball.json` exists (the existing behaviour, now through the index).
- `deleteRemovesEveryFileOfTheName`: after `load()` of a project with `Ball.json` in two kinds' folders (one actor, one manager), `delete("Ball")` removes both.
- `aStoreUsedBeforeLoadingStillFindsOldFiles`: no `load()`, an existing `old/Ball.json` on disk, `save(Ball in new)` → the old file is deleted (fallback walk).
- `anUnreadableFileIsNeverDeleted`: the existing guarantee, with the index.
