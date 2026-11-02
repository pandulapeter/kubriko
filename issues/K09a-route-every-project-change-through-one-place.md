<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Route Creator's own writes and the watcher's changes through one ProjectChanges router, fed by typed changes SceneStore records itself, so a Scene edit no longer rescans the project and git refreshes after Creator's own writes.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** INF-16 (routing part), ARCH-2 (routing part), CMD-19, CMD-10, INF-11, INF-10 (part b: the double refresh)  ·  **Depends on:** K01a (the `onSaved`/`onSaveFailed` callbacks it routes), K02a (`SceneStore.writeAll`), K06 (`SceneStore.refresh` and the name index), K08 (order only: it edits the same `commands/scenes` files)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/ProjectChanges.kt`, new `scenes/SceneChange.kt`, `scenes/SceneStore.kt`, `workspace/WorkspaceController.kt`, `commands/CommandContext.kt`, `commands/CommandExecutor.kt`, `commands/scenes/SceneValidation.kt`, `commands/scenes/CreateScene.kt`, `commands/scenes/DeleteScene.kt`, `commands/scenes/SceneRenaming.kt`, `commands/actorTypes/RenameActorType.kt`, `commands/actorTypes/PropertySceneValues.kt`, `commands/folders/FolderOperations.kt`, `git/GitController.kt`, tests new `workspace/ProjectChangesTest.kt`, `scenes/SceneStoreTest.kt`, `git/GitControllerTest.kt`, `commands/CommandTesting.kt`, `commands/CommandExecutorTest.kt`, `workspace/WorkspaceControllerTest.kt` (or T03's split), `creator/CLAUDE.md` (the `workspace` row: "routing watcher changes")

Lane H's H01/H02/H03 also edit `WorkspaceController.kt` in phase 2 (selection, presenter, `close()`); the merge of lanes H and K needs a hand on that file.

## Problem

Change routing lives in three places with different sets, in `workspace/WorkspaceController.kt`:

```kotlin
            onSaved = { haveTypesChanged ->
                refreshResources()
                if (haveTypesChanged) {
                    sceneEditing.markOutdated()
                    gameBuilds.onCodeChanged()
                }
            },
...
            onScenesChanged = { names ->
                refreshResources()
                sceneEditing.onScenesChanged(names)
                blockTypes?.regenerate()
                gameBuilds.onCodeChanged()
            },
...
    fun onProjectChanged(changes: Set<ProjectChange>) {
        ...
        if (ProjectChange.SCENES in changes || ProjectChange.SOURCES in changes) {
            refreshResources()
            gameBuilds.onCodeChanged()
        }
        git.refresh()
    }
```

- CMD-10: every Scene command (a value typed into the inspector, every drag end of `move_actor`) rescans the whole project tree (`refreshResources` → `ResourceScanner.scan` + `scenes.folders()`), queues a regeneration, and, while a game plays, a hot-reload build. The Scene set didn't change; only the open Scene and, possibly, its music matter.
- INF-10(b): the regeneration the Scene command queues ends in `onSaved`, which calls `refreshResources()` again: two full walks per edit.
- INF-11: `git.refresh()` runs only for the watcher's changes, which skip Creator's own writes (`OwnWrites`), so an open Version control panel never shows Creator's own saves; and `GitController.refresh()` launches a full `status()` + `history()` per call, unconflated:
  ```kotlin
    fun refresh() {
        scope.launch {
            if (!perform { repository -> repository?.let(::readState) ?: GitState.NoRepository }) report(...)
        }
    }
  ```
- CMD-19: every command that writes Scenes must remember to call `context.onScenesChanged`/`onSceneRenamed` (`writeScene`, `RenameActorType`, `changeSceneStates`, `CreateScene`, `DeleteScene`, `renameScene`, `createFolder`/`deleteFolder` with `onScenesChanged(emptyList())`). All do today; nothing makes the next one.

## Fix

1. **`scenes/SceneChange.kt`**: `internal sealed interface SceneChange { data class Written(val name: String, val isNew: Boolean); data class Deleted(val name: String); data class Moved(val oldName: String, val newName: String); data object FoldersChanged }`.
2. **`SceneStore` records its own changes.** Every mutating call (`write`, `writeAll`, `move`, `delete`, `createFolder`, `deleteFolder`, `changeStates`, `renameTypeId`) appends to a `ConcurrentLinkedQueue<SceneChange>` (they run on IO). `fun takeChanges(): List<SceneChange>` drains it. Restores made by `writeAll`'s rollback record nothing (the Scene is as it was).
3. **The executor delivers them.** `CommandExecutor` gets `onSceneChanges: (List<SceneChange>) -> Unit = {}`; at the end of `execute` and `step` (in a `finally`, still inside the lock, on Main), `context.scenes.takeChanges().takeIf { it.isNotEmpty() }?.let(onSceneChanges)`. Only commands write Scenes (`@EditorMutation` and `CommandRegistryTest.onlyCommandsChangeTheProject` guarantee it), so this is complete. Remove `CommandContext.onScenesChanged` and `onSceneRenamed` and every manual call listed above (`writeScene` keeps only the write; `renameScene` keeps its console report).
4. **`workspace/ProjectChanges.kt`**, the one routing table, main-thread confined, with the parts it drives passed as small function parameters (so `ProjectChangesTest` drives it with recording lambdas):
   | Change | Resources | Scene editing | Regenerate | Game builds | Git |
   |---|---|---|---|---|---|
   | Scene `Written(isNew = false)` | — | `onScenesChanged(names)` | yes (cheap: `BlockTypeStore` skips when types and Scene music are unchanged) | `onCodeChanged()` | refresh |
   | Scene `Written(isNew = true)`, `Deleted`, `Moved`, `FoldersChanged` | refresh | `onScenesChanged`, `onSceneRenamed` for `Moved` | yes | `onCodeChanged()` | refresh |
   | Types batch saved (`onSaved(haveTypesChanged)`) | — (see below) | `markOutdated()` if types changed | — | `onCodeChanged()` if types changed | refresh |
   | Watcher: the existing `onProjectChanged` rows | as today | as today | as today | as today | refresh |
   - Resources after a type batch: `onSaved` refreshes today. Find out why with `git log -L '/onSaved = /,+8:creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt'`; if it is only for Scene-triggered regenerations (the double refresh) drop it; if something in the resource tree does depend on type saves (e.g. a type's sprite folder), keep it but through the coalesced refresh below.
   - **Coalesced resource refresh**: `refreshResources()` becomes a request: a `Channel<Unit>(CONFLATED)` read by one loop in the workspace scope that runs the scan (and K06's `scenes.refresh()`) on IO; at most one scan runs and one waits, however many changes arrive. Keep `LatestRequests` only if the loop alone doesn't make it redundant (it does; remove it from here if so). The loop guards each iteration like INF-1's loops (catch non-cancellation exceptions, report through the scope's handler, keep looping).
   - `WorkspaceController` keeps `onProjectChanged(changes)` as a one-line delegation (tests and the watcher call it), and wires `onSaved`, `onSceneChanges`, and the watcher to the router.
5. **`GitController.refresh()` conflated**: a `Channel<Unit>(CONFLATED)` and one loop in its scope reading status and history under `operations`, guarded per iteration; `refresh()` just sends. `commit`/`discard`/`startVersionControl` stay direct (they return results).
6. `creator/CLAUDE.md` `workspace` row: "routing watcher changes" → "`ProjectChanges` (one table routing Creator's own writes and the watcher's changes: a Scene edit refreshes only the Scene view, a new or removed Scene the resource tree too, and every change the Version control panel)".

## Tests

- `ProjectChangesTest` (recording lambdas): one test per row of the table, asserting exactly which calls happen; a `SetActorValues`-like `Written(isNew = false)` doesn't refresh resources; ten `Written` changes in one turn cause one resource scan (coalescing, virtual time); a watcher `SOURCES` change still marks the Scene outdated and refreshes the upgrade check.
- `SceneStoreTest.mutationsRecordTheirChanges`: `write` new/existing, `move`, `delete`, `createFolder`, `changeStates` → `takeChanges()` returns the matching `SceneChange`s in order and then nothing; a failing `writeAll` (K02a's hook) records nothing.
- `CommandExecutorTest.sceneChangesAreDeliveredOnce`: `execute(CreateScene)` → `onSceneChanges` gets `[Written("a", isNew = true)]`; an undo → `[Deleted("a")]`.
- `GitControllerTest.refreshesAreConflated`: `FakeGitRepository.statusCalls` (from K07, or add it here) after ten `refresh()` calls in one turn is at most 2.
- `WorkspaceControllerTest.creatorsOwnSaveRefreshesGit`: a type `put` with an open repository → after the write delay, `statusCalls` rose.
- Existing command tests that passed `onScenesChanged`/`onSceneRenamed` to `commandContext(...)` assert through `scenes.takeChanges()` instead.
