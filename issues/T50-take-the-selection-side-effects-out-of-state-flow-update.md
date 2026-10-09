# Run removeSelectedActor's side effects outside StateFlow.update and read the selection from its source

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Planned
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
Two selection paths in `EditorController` are wrong in ways that depend on timing:

1. Side effects inside a retrying lambda (`EditorController.kt:270-277` at 70de96c6, unchanged since 2480325f):
```kotlin
    fun removeSelectedActor() = _selectedActor.update { selectedActor ->
        selectedActor?.let {
            recordSnapshot()
            removeSceneActor(it)
            markSceneAsModified()
        }
        null
    }
```
`MutableStateFlow.update` re-runs its lambda when a concurrent write wins the compare-and-set, which would record two undo
snapshots and remove twice. Today every writer of `_selectedActor` happens to run on the main thread (clicks, the
`KeyboardInputListener` tick, `loadMap`'s `launch(Dispatchers.Main)`), so it does not retry in practice — but nothing enforces it.

2. Reads of a lagging copy. `selectedUpdatableActor` is `combine(_selectedActor, triggerActorUpdate).stateIn(this, Eagerly, ...)`
on this scope's `Dispatchers.Default`, so its `.value` trails `_selectedActor` by a dispatch. Yet decisions read it:
```kotlin
    fun selectActor(actor: Editable<*>) {
        pendingPropertyEditKey = null
        _selectedActor.update {
            if (selectedUpdatableActor.value.first == actor) null else actor   // ignores the lambda's own value
        }
    }
    fun getSelectedActor() = selectedUpdatableActor.value.first            // :212; used by navigateBack, isPlacingNewInstance, drag
```
(`selectActor` is :259-268) and `onLeftClick` (:218-257) (`selectedUpdatableActor.value.first.let { currentSelectedActor -> ... }`). Two clicks on the same actor in quick
succession can both see the stale "not selected" and leave it selected instead of toggling; a click on empty space right after a
deselect can deselect again instead of placing the preview; Escape right after a selection can skip "deselect actor".

## Fix
- `fun removeSelectedActor() { val actor = _selectedActor.value ?: return; recordSnapshot(); removeSceneActor(actor); markSceneAsModified(); _selectedActor.value = null }`
- `selectActor`: `_selectedActor.update { current -> if (current == actor) null else actor }`.
- `fun getSelectedActor() = _selectedActor.value`, and `onLeftClick` reads `_selectedActor.value`.
- Keep `selectedUpdatableActor` for the UI and `OverlayManager` (they observe; a dispatch of lag is fine there).

## Behaviour
Bug fix: decisions use the current selection instead of the last value the derived flow published; delete records exactly one undo
step. In ordinary (slow) use nothing visibly changes. Threading: the reads move from a Default-dispatched copy to the source
`MutableStateFlow`, which is the value the writers just set on the main thread — check that no caller relied on the lag (the
challenger should trace `handleMouseClick`'s Press → Release → `onLeftClick` order in `extensions/ModifierExtensions.kt` (`handleMouseClick`, :44): Press reads
`getSelectedActor()` to arm a drag; with the fix, a Release that selects an actor and the next Press see it immediately, which is the
intended behaviour).

## Public API
None (internal).

## Tests
No unit test today: `EditorController` needs a full Kubriko with Serialization/Persistence/Keyboard managers. If the
EditorController split (T53) lands first, cover the toggle and the single undo step on the extracted selection/scene holders instead.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
In the Scene Editor: double-click an actor quickly (ends deselected, as a toggle), right-click the selected actor then Ctrl+Z
(restored with one undo), select then immediately press Escape (deselects).
