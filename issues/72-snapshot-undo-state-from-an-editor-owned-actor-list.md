# Snapshot undo state from an editor-owned actor list and restore the selection by a stable actor id

**Challenged:** amended — `addSceneActor` mirrors `ActorManager`'s `Unique` rule (a new `Unique` actor drops the tracked instance of the same class), otherwise placing a second Slingshot/Camera would keep the replaced one in `sceneActors` and save both into the scene file; one test added.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UndoRedoHistory.kt`, `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UndoRedoHistoryTest.kt` (new), `tools/scene-editor/CLAUDE.md`

Lands after plan 71 (both edit `saveScene`/`syncScene`).

## Problem

Undo snapshots, clears and saves read the scene from `allEditableActors`, a `stateIn` on the controller's `Dispatchers.Default` scope over `ActorManager.allActors`:

```kotlin
private val allEditableActors = actorManager.allActors
    .map { it.filterIsInstance<Editable<*>>() }
    .stateIn(this, SharingStarted.Eagerly, emptyList())
...
private fun takeSnapshot() = UndoRedoHistory.SceneSnapshot(
    serializedScene = serializationManager.serializeActors(allEditableActors.value),
    isSceneModified = _isSceneModified.value,
)
...
private fun clearSceneActors() = actorManager.remove(actorManager.allActors.value.filterNot { it in editorActors })
```

`ActorManager.add`/`remove` only enqueue an operation that a background batch processor applies later, and `allEditableActors` follows `allActors` one more hop later. So both lag the editor's own actions:

1. **Duplicates.** Place an actor (queues `Add(A)`) and press Ctrl/Cmd+Z before the batch lands: `restoreSnapshot` → `replaceSceneActors` → `clearSceneActors()` builds its remove list from `allActors`, which does not contain A yet. The queue then runs `Add(A)`, `Remove(others)`, `Add(restored)` — A survives next to the restored scene. Holding Ctrl+Z (key repeat) makes this easy.
2. **Wrong snapshots.** The "current" snapshot pushed onto the redo stack by `performUndo(takeSnapshot())` can miss the actor that was just added, so redo loses it.
3. **Wrong selection.** `restoreSnapshot` (~345-359) re-selects by index in the *current* list and class:
   ```kotlin
   val previousSelectionIndex = previousSelection
       ?.let { allEditableActors.value.indexOf(it) }
       ?.takeIf { it >= 0 }
   val restoredActors = serializationManager.deserializeActors(snapshot.serializedScene)
   replaceSceneActors(restoredActors)
   _selectedActor.update {
       previousSelectionIndex
           ?.let(restoredActors::getOrNull)
           ?.takeIf { restored -> restored::class == previousSelectionType }
   }
   ```
   The restored list is a different point in time. Scene `[A, B, C]`, delete B, select C (index 1 in `[A, C]`), undo → `[A, B, C]`, index 1 is B, same class → **B** is selected and the next property edit changes the wrong actor.

## Fix

1. `EditorController` keeps the scene's actors itself, touched only on the main thread (every entry point — UI callbacks, keyboard actor callbacks, and after plan 71 load/save — runs there):
   ```kotlin
   private val sceneActors = mutableListOf<Editable<*>>()
   private val sceneActorIds = IdentityHashMap<Editable<*>, Long>()
   private var nextSceneActorId = 0L
   ```
   with private helpers `addSceneActor(actor, id = nextSceneActorId++)` and `removeSceneActor(actor)` that update both and call `actorManager.add`/`remove`. Use them in `onLeftClick` (placement), `onRightClick`, `removeSelectedActor`, `replaceSceneActors`.

   `addSceneActor` must mirror `ActorManager`'s `Unique` rule (it keeps only the latest actor per exact class, `a::class`): when the new actor is `Unique`, first drop any tracked actor with the same `::class` from `sceneActors`/`sceneActorIds` (no `actorManager.remove` — the engine replaces it itself). The editor supports `Unique` types (`isTypeUnique`, the "unique" label; `Slingshot` in Annoyed Penguins and `Camera` in the performance demo are `Editable` and `Unique`), and without this the replaced instance would stay in `sceneActors` and be serialized next to its replacement. Put the index lookup in a pure `internal fun indexOfReplacedUnique(actorClasses: List<KClass<*>>, newClass: KClass<*>, isUnique: Boolean): Int` in `UndoRedoHistory.kt` (`-1` when nothing is replaced) so it can be tested; if a restored snapshot or a loaded file contains two actors of one `Unique` class, the same rule leaves only the last, matching what `ActorManager` keeps.
2. `clearSceneActors()` → `actorManager.remove(sceneActors.toList()); sceneActors.clear(); sceneActorIds.clear()`. The queue is ordered, so a still-pending `Add` of any of those actors is applied before this `Remove`. `editorActors` are never in `sceneActors`, so the filter goes away.
3. `UndoRedoHistory.SceneSnapshot` gains `val actorIds: LongArray` (ids in `sceneActors` order). Change it from `data class` to `class` — its equality is never used and an array property makes the generated `equals` misleading.
4. `takeSnapshot()` serializes `sceneActors` and records their ids. `restoreSnapshot`:
   ```kotlin
   val selectedId = _selectedActor.value?.let(sceneActorIds::get)
   val restoredActors = serializationManager.deserializeActors(snapshot.serializedScene)
   val ids = snapshot.actorIds.takeIf { it.size == restoredActors.size }
   clearSceneActors()
   restoredActors.forEachIndexed { index, actor -> addSceneActor(actor, ids?.get(index) ?: nextSceneActorId++) }
   _selectedActor.update { restoredSelectionIndex(selectedId, snapshot.actorIds, restoredActors.size)?.let(restoredActors::get) }
   ```
   Put the pure `internal fun restoredSelectionIndex(selectedId: Long?, snapshotIds: LongArray, restoredCount: Int): Int?` in `UndoRedoHistory.kt`: `null` when nothing was selected, when the sizes differ (the serialization plugin dropped an entry, so positions no longer line up) or when the id is absent. Keep `nextSceneActorId` above any restored id (ids only ever come from it, so reusing them is safe).
5. `saveScene` / `syncScene` serialize `sceneActors` instead of `allEditableActors.value`. `allEditableActors` stays for `filteredAllEditableActors` (UI list only).
6. `tools/scene-editor/CLAUDE.md` → Undo/redo: add "The scene's actors are tracked by the controller (`sceneActors`, main thread only) rather than read back from `ActorManager`, whose batched updates lag; snapshots carry per-actor ids so undo/redo re-selects the same actor."

Behaviour note: `Editable` actors added by a custom manager (not through the editor) are no longer saved into scene files. No manager passed to the editor in this repo adds actors.

## Tests

`UndoRedoHistoryTest` (desktopTest, pure):
- `restoredSelectionIndex`: selected id found → its index; not selected → `null`; id absent (actor was created after the snapshot) → `null`; `restoredCount != snapshotIds.size` → `null`; the `[A, B, C]` / delete B / select C case (ids `[1, 2, 3]`, selected 3) → index 2.
- `indexOfReplacedUnique`: `[A::class, B::class]` + a unique `B::class` → 1; + a non-unique `B::class` → -1; + a unique `C::class` → -1.
- `UndoRedoHistory`: record → undo returns the recorded snapshot and pushes the current one to redo; redo returns it; a new record clears redo; the undo stack keeps at most 25 entries (record 26, undo 25 times, the 26th undo returns `null`); `canUndo`/`canRedo` follow.

Run `./gradlew :tools:scene-editor:desktopTest`.

## Manual check

Desktop, Annoyed Penguins → Scene Editor:
1. Select a type and click quickly to place several actors, then hold Ctrl/Cmd+Z: every placed actor disappears and the actor count in the bottom row returns to the starting value (no leftovers). Redo repeatedly: they come back once each.
2. Place A, B, C; right-click B to delete it; select C; undo: B reappears and **C** stays selected (check the property panel's position values).
3. Place a Slingshot, then a second one elsewhere: the first disappears; save and reopen the file: exactly one Slingshot, at the second position.
