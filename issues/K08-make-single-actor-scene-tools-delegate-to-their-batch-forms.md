<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Keep the single-Actor Scene tools, but make each one check and apply through the same functions as its batch form.

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** CMD-16 (pairs part; the description constants landed in b8aaa38b1)  ·  **Depends on:** K02a (order only)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/scenes/PlaceActor.kt`, `PlaceActors.kt`, `SetActorValues.kt`, `SetActorsValues.kt`, `SetActorState.kt`, `SetActorsState.kt`, `RemoveActor.kt`, `RemoveActors.kt`, `RestoreActor.kt`, `RestoreActors.kt`, new `commands/scenes/ActorBatches.kt`, tests `commands/scenes/BatchActorCommandsTest.kt`, `commands/scenes/SceneCommandsTest.kt`, `commands/scenes/ActorValueCommandsTest.kt`

## Problem

Five pairs are registered as tools and implemented twice: `place_actor`/`place_actors`, `set_actor_values`/`set_actors_values`, `set_actor_state`/`set_actors_state`, `remove_actor`/`remove_actors`, `restore_actor`/`restore_actors`. Each single form repeats its batch form's checks and Scene edit, and they already drifted: `RestoreActor.validate` checks the state with

```kotlin
        !SceneDocument.isValid(SceneDocument.format(listOf(SceneActorEntry(input.typeId, input.state)))) -> Rejection(Res.string.error_command_scene_content_invalid)
```

while `RestoreActors.validate` uses

```kotlin
                    SceneDocument.stateObject(restored.state) == null -> Rejection(Res.string.error_command_scene_content_invalid)
```

## Fix

The user decided (decisions.md, "Keep single and batch Scene tools; singles delegate internally"): no tool-surface change (names, descriptions, input schemas, results, and undo titles stay byte-identical).

1. `commands/scenes/ActorBatches.kt` (internal): one function per pair, used by both, e.g.
   - `CommandContext.validatePlacements(scene, placements: List<PlaceActors.Placement>): Rejection?` and `placedActors(actors, placements): List<SceneActorEntry>` (the ID assignment of `PlaceActors.apply`);
   - `validateActorChanges(scene, changes: List<ActorValueChanges>)` and `withChanges(actors, changes): Pair<List<SceneActorEntry>, List<SetActorsState.ActorState>>`;
   - `validateStates(scene, states)` / `withStates(...)`; `validateRemovals` / `withoutActors(...)` returning the removed `(index, actor)` pairs; `validateRestorations` / `withRestored(...)`.
   The batch commands' bodies move there unchanged.
2. Each single command keeps its `name`, `description`, `title`, `Input`, `target`, `mergeKey` (`SetActorValues` merges typing), and its own inverse type (so undo merging and titles don't change: `SetActorValues`' inverse stays `SetActorState`, `PlaceActor`'s stays `RemoveActor`, `RemoveActor`'s stays `RestoreActor`), but its `validate` and `apply` call the batch functions with a one-element list and map the result back (the single Actor's ID into `result = {"actorId": …}`).
3. The repeated-ID and empty-list checks (`validateActorList`) can't trigger for one element; keep them in the batch path only.
4. The drift: use the batch form's check (`SceneDocument.stateObject(state) == null`) for both. Check what `SceneDocument.isValid(format(...))` rejected that `stateObject` doesn't (a state that is an object but whose Actor entry fails another document rule, e.g. an empty `typeId`); if there is such a case, add it to the shared check so neither form accepts less than before, and add a test for it.
5. Keep every file under the size limits; the batch files shrink.

## Tests

- Existing single and batch command tests keep passing unchanged (they pin the behaviour).
- `BatchActorCommandsTest.singleAndBatchFormsAgree`: for each pair, the same change through the single and through the batch form (one element) on two copies of a Scene leaves byte-identical Scene files and equivalent results; the same invalid input (missing Scene, missing Actor, bad state, taken ID, non-finite number) gives the same rejection from both.
- `restoreActorRejectsWhatRestoreActorsRejects`: the state case of point 4.
- `CommandRegistryTest` and `JsonSchemaTest` stay green without edits (the tool surface didn't move).
