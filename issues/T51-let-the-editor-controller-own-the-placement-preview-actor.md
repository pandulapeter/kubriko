# Let EditorController derive and own the placement preview actor instead of OverlayManager assigning it

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** desktop  ·  **Class:** Planned
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/overlay/OverlayManager.kt, tools/scene-editor/CLAUDE.md
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`EditorController.previewOverlayActor` is a public `var` (`EditorController.kt:154` at 70de96c6) that another class writes:
```kotlin
// OverlayManager.onInitialize, OverlayManager.kt:50-59
        combine(
            editorController.selectedUpdatableActor,
            editorController.selectedTypeId,
        ) { (selectedInstance, _), selectedTypeId ->
            selectedInstance to selectedTypeId
        }.onEach { (_, selectedTypeId) ->
            editorController.previewOverlayActor = selectedTypeId?.let {
                editorController.serializationManager.getMetadata(selectedTypeId)?.instantiate?.invoke(SceneOffset.Zero)?.restore()
            }
        }.launchIn(scope)
```
- It uses only `selectedTypeId`, but re-instantiates a preview on every `selectedUpdatableActor` emission — every selection change
  and every property/drag tick (`notifySelectedActorUpdate` flips the trigger), allocating and restoring a fresh actor each time.
- "Place the same type again" works only through that coincidence: `onLeftClick` places the preview and sets
  `previewOverlayActor = null`, then `selectActor(it)` emits, and the combine builds a new preview.
- The `var` is written on the overlay Kubriko's scope, read on the UI thread (`isPlacingNewInstance`, `onLeftClick`) and in the
  overlay tick (`OverlayManager.update` writes `previewOverlayActor?.body?.position` every tick, :72) with no defined ordering.

## Decision
Should the preview survive a placement?
- (a) **Keep today's behaviour (recommended):** after placing, a fresh preview of the selected type follows the mouse again, so the
  type can be placed repeatedly; Escape / clicking the type again ends placement.
- (b) Placing ends placement: the type stays selected but no preview until it is re-selected.

## Fix
- `EditorController` owns `private var _previewActor: Editable<*>?` (main-thread state, like `sceneActors`) exposed read-only
  (`val previewOverlayActor: Editable<*>? get() = _previewActor`).
- `selectActorType(typeId)` recomputes it from the resulting `selectedTypeId` (`instantiatePreview(typeId)`: the same
  `getMetadata(...)?.instantiate?.invoke(SceneOffset.Zero)?.restore()` expression). With option (a), `onLeftClick` re-instantiates
  right after placing; with (b) it sets null.
- `reset()` / `loadMap` keep the type and preview as today (verify: today they do not touch `selectedTypeId`).
- Delete the `combine(...).launchIn(scope)` block in `OverlayManager.onInitialize` (keep `kubriko.get<ActorManager>().add(this)`).
  `OverlayManager.update` keeps moving the preview to the snapped mouse position.
- If the EditorController split (T53) lands first, this lives in its selection/preview holder instead.
- `tools/scene-editor/CLAUDE.md` "Placement preview" (the `EditorController` section's bullet, line 33): say who owns it and when it is created.

## Behaviour
Option (a): unchanged for the user; the preview is no longer rebuilt on selection/property changes (it is not added to the scene, so
its identity is invisible). Threading: the preview is now written only on the main thread; the overlay tick still reads it
unsynchronised as today — the challenger should confirm the overlay Kubriko ticks on the main thread (`TickSource.viewportFrames()`).

## Public API
None (internal).

## Tests
If a pure helper results (e.g. `instantiatePreview` taking the metadata lookup), test it with a fake lookup; otherwise none.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
Select a type: the preview follows the mouse; click to place it; with (a) a new preview appears and a second click places another;
select a placed actor while a type is selected and drag it — no preview flicker; deselect the type — the preview disappears.
