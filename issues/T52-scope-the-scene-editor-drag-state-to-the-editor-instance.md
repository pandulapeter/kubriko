# Replace the file-level drag state globals in ModifierExtensions.kt with a per-editor ActorDragState and pure drag math

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Planned
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/extensions/ModifierExtensions.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/extensions/ActorDragState.kt (new), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/EditorUserInterface.kt, tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/extensions/ActorDragMathTest.kt (new)

## Problem
`extensions/ModifierExtensions.kt:36-41` keeps the drag gesture's state in process-wide top-level `var`s shared by
`handleMouseClick` and `handleMouseDrag`:
```kotlin
private var startOffset: SceneOffset? = null
private var isDragging = false
private var dragStartMouseSceneOffset = SceneOffset.Zero
private var dragStartScale = Scale.Unit
private var dragStartRotation = AngleRadians.Zero
private var dragStartPointerAngle = AngleRadians.Zero
```
Two embedded editors (the contract allows several `SceneEditor(...)` Composables) share one drag; state survives an editor being
closed mid-drag. The scale/rotation math (`handleMouseDrag`, the `Scale` and `Rotate` branches, ~l.133-152) is inline in a pointer
lambda and untested.

## Fix
- New `internal class ActorDragState` (in `extensions/ActorDragState.kt`) with those six fields as `var`s (plain fields, written and
  read only from pointer callbacks on the UI thread — no StateFlow, no allocation per event).
- `handleMouseClick(...)` and `handleMouseDrag(...)` take `dragState: ActorDragState` and use its fields in place of the globals.
- `EditorUserInterface` (or `SceneViewport`, if T15 landed) creates `val dragState = remember { ActorDragState() }` and passes it to both.
- Extract the math as pure internal functions next to the state:
  `draggedScale(startScale: Scale, mouseDelta: SceneOffset, size: SceneSize, minimumScale: Float = MINIMUM_INTERACTIVE_SCALE): Scale`
  (the current per-axis expression, including the `width > 0f` / `height > 0f` guards) and
  `draggedRotation(startRotation: AngleRadians, startPointerAngle: AngleRadians, currentPointerAngle: AngleRadians) = startRotation + (currentPointerAngle - startPointerAngle)`.
  Both are value-class arithmetic; no allocation. Move `private const val MINIMUM_INTERACTIVE_SCALE` into `ActorDragState.kt`
  with them (keep it `private` there unless `ModifierExtensions.kt` still needs it).

## Behaviour
One editor: unchanged. Several editors: each drags independently (the bug the globals allow). The modifier chain gets one more
captured, remembered object, so pointer-input keys are unchanged across recompositions.

## Public API
None (internal).

## Tests
`ActorDragMathTest`: `draggedScale(Scale.Unit, SceneOffset(50.sceneUnit, (-200).sceneUnit), SceneSize(100.sceneUnit, 100.sceneUnit))`
→ horizontal 1.5, vertical clamped to 0.05; zero width keeps the start scale on that axis; `draggedRotation` adds the pointer delta.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
In the Scene Editor, Translate/Scale/Rotate-drag a box actor (T/S/R): identical feel; a click without drag still selects; right-click
still deletes; shift-drag pans.
