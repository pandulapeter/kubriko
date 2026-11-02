<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Remember with each undo step a hash of the types and Scenes it touches, and drop the step instead of applying it when one of them changed outside Kubriko Creator since.

**Challenged:** amended — a Scene that exists but can't be read right now (locked by the running game on Windows) is not a change: it gets no hash and isn't compared; the hashes of all targets are taken in one IO hop; a test reloads through the real `BlockTypeStore`, since the fake repository keeps the same objects and can't show a lossy round trip dropping steps after Creator's own writes.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Findings:** CMD-3  ·  **Depends on:** K02b (uses `WriteCommand.targets`, composites)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/CommandExecutor.kt`, `commands/HistoryEntry.kt`, `commands/CommandContext.kt`, `commands/CommandTarget.kt` (KDoc only, if needed), `creator/src/main/composeResources/values/strings.xml`, tests `commands/CommandExecutorTest.kt`, `commands/RestoringChangesTest.kt`, `creator/CLAUDE.md` (commands section)

## Problem

The history is only cleared by `Applied.isRewritingFiles` (DiscardChanges, UpgradeProject):

```kotlin
        if (applied.isRewritingFiles) _history.value = CommandHistory()
```

An outside change (an IntelliJ edit, `git checkout`/`pull` in a terminal, a hand-edited Scene) reaches `WorkspaceController.onProjectChanged` → `blockTypes?.reloadFromDisk()` / `sceneEditing.onScenesChangedOnDisk()`, with no history interaction. Inverses address by index and carry full old values, e.g. `commands/actorTypes/EditBlock.kt`:

```kotlin
        val previous = definition.events[input.eventIndex].blocks[input.blockIndex]
        context.requireBlockTypes.put(definition.withBlockReplaced(input.eventIndex, input.blockIndex, input.block))
        return Applied(inverse = Invocation(EditBlock, input.copy(block = previous)))
```

`validateRestoring` only checks that the indices still exist, so after an outside edit that reorders Blocks, undo overwrites an unrelated Block; the inverse of a type deletion writes back a whole stale definition. The executor KDoc ("A step that no longer applies (because the project changed outside Kubriko Creator, for example) is dropped from the history") holds only when an index vanished.

## Fix

Option (c) of the verification (exact, local to the executor, robust to every outside source); alternatives were clearing all history on any outside change (loses undo after every IntelliJ save) or diffing reloads in `WorkspaceController`.

1. `HistoryEntry` gets `val contentHashes: Map<CommandTarget, Int> = emptyMap()`: for each type or Scene the entry's `revert` and `reapply` touch (`WriteCommand.targets`, from K02b, normalized: `CommandTarget.BlockType(name)` without event/block indices, `CommandTarget.Scene(name)`), a hash of its content right after the change was made (or stepped). `VersionControl`, `Game`, and `Project` targets get no hash (not checked, as today).
2. `CommandContext.contentHashOf(target: CommandTarget): Int?` (internal, suspend):
   - `BlockType(name)` → `blockTypes?.get(name).hashCode()` (a deleted type hashes as `null.hashCode()`, 0, so "still deleted" matches). Read on the calling (main) thread; definitions are immutable data classes.
   - `Scene(name)` → on IO: `0` when the Scene doesn't exist (`!scenes.exists(name)`, so "still deleted" matches), its text's hash when `scenes.read(name)` returns it (as `SceneStore` caches it; the IO hop because `read` may stat or read the file), and `null` when it exists but can't be read (`cached` swallows the `IOException` of a file the running game holds open on Windows): a null hash, stored or current, is not compared, so a momentary lock doesn't drop the step for good (its apply then fails or succeeds on its own). Make it `contentHashesOf(targets: Collection<CommandTarget>): Map<CommandTarget, Int?>`, one IO hop for all Scene targets of an entry (a composite can have one per Scene).
   - otherwise null.
3. `CommandExecutor`: after a successful `apply` in `execute` and `step`, compute the hashes for the new entry's targets (`entry.revert` and `entry.reapply` targets, union) and store them. When `record` merges into the previous entry, the merged entry takes the new hashes. In `step`, before `validate`: recompute the hashes of the entry's keys; if any differs, drop the entry (`_history.value = remaining`) and return `CommandOutcome.Rejected(Rejection(Res.string.error_command_changed_outside, name))`, `name` being the changed target's type or Scene name (`Rejection` arguments are plain strings). New string `error_command_changed_outside`: "That step can't be undone or redone any more, since %1$s was changed outside Kubriko Creator."
4. Own changes keep the hashes consistent: every later own change to the same type or Scene is undone before an earlier entry is reached (LIFO), and inverses restore exact content. Where an inverse doesn't restore byte-identical Scene text, the entry would be dropped spuriously; check `SceneDocument.format` is deterministic for the restored Actors (it is a pure function of the Actor list) and cover it with the tests below. Generated code and regeneration don't change definitions, so they don't affect the hashes.
5. `undoTurn` calls `step`, so an AI turn's Undo stops at a step changed outside, as at any other rejection.
6. The cost: one `hashCode()` of a definition (a tree walk of small data classes) and, for Scene targets, a cached text hash, per change and per step, on top of the change itself. No per-frame or per-keystroke loop beyond what the command already does; typing merges keep one entry.
7. Executor KDoc for `step`: "A step that no longer applies, or whose type or Scene changed outside Kubriko Creator since, is dropped from the history." `creator/CLAUDE.md` commands section: the same in one sentence.

## Tests

`CommandExecutorTest` (real `BlockTypesController` over `FakeBlockTypeRepository`, real `SceneStore` over a temp folder):
- `anOutsideChangeDropsTheStepsOfThatType`: `EditBlock` on `Ball` through `execute`; then `repository.stored["Ball"] = reordered` and `blockTypes.reload()` → `step(isUndo = true)` is `Rejected(error_command_changed_outside)`, the entry is gone, `blockTypes["Ball"] == reordered`.
- `anUnchangedReloadKeepsUndoWorking`: the same, but the stored type equals the in-memory one → undo succeeds.
- `anOutsideChangeToAnotherTypeKeepsTheStep`: edit `Ball`, change `Wall` outside → undo of `Ball`'s step succeeds.
- `aSceneChangedOutsideDropsItsSteps`: `SetActorValues` on Scene `level`, then rewrite `level.json` by hand (different text, new modification time) → undo rejected and dropped.
- `ownChangesDontInvalidateEarlierSteps`: three edits of `Ball` and two of Scene `level`, interleaved; undo all five, redo all five → every step succeeds and the final state equals the state after the five edits.
- `aReloadOfCreatorsOwnSaveKeepsUndoWorking`: a real `BlockTypesController` over a real `BlockTypeStore` in a temp folder, one `EditBlock` (and one `SetAppearance`, `AddProperty`, `AddEvent` with a Kotlin Block) through `execute`, `awaitWrites()`, then `reload()` as the watcher does after an unrelated outside change → undo succeeds for each (the definitions read back equal the ones in memory; if one doesn't, fix the round trip, don't weaken the check).
- `aLockedSceneDoesntDropTheStep`: give `SceneStore` an unreadable file for `level` for one call (K02a's/K06's hooks, or a read hook) → the step isn't dropped as changed outside.
- `RestoringChangesTest`: the existing round trips still pass.
