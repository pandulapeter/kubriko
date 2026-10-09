# Replace the two private copies of DrawTransform.transformViewport in the scene editor with one internal extension

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/overlay/OverlayManager.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/GridOverlay.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/extensions/DrawTransformExtensions.kt (new)

## Problem
`OverlayManager.kt:161-175` and `GridOverlay.kt:106-120` each declare the same private member extension:
```kotlin
    private fun DrawTransform.transformViewport(
        viewportCenter: SceneOffset,
        shiftedViewportOffset: SceneOffset,
        viewportScaleFactor: Scale,
    ) {
        translate(left = shiftedViewportOffset.x.raw, top = shiftedViewportOffset.y.raw)
        scale(scaleX = viewportScaleFactor.horizontal, scaleY = viewportScaleFactor.vertical, pivot = viewportCenter.raw)
    }
```
(verified identical). A third copy in `tools/debug-menu/.../DebugMenuManager.kt` and an internal one in the engine are in
other modules and stay out of scope.

## Fix
Create `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/extensions/DrawTransformExtensions.kt` (package `...implementation.extensions`, MPL-2.0 header) with the function
as a top-level `internal fun DrawTransform.transformViewport(...)`, body copied verbatim. Delete both private copies and
import `com.pandulapeter.kubriko.sceneEditor.implementation.extensions.transformViewport` in both files; drop imports left unused
(`DrawTransform`, `Scale` in each file — check). No name clash in the package (`extensions/` holds only
`ModifierExtensions.kt` and `PointBodyExtensions.kt`).

## Behaviour
Same function, same call sites; a top-level function call instead of a member call allocates nothing extra.

## Public API
None (internal).

## Tests
The existing ones (it needs a real `DrawTransform`; not unit-testable without Skia).

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
Open the Scene Editor (e.g. from `demo-performance` on desktop with `showcase.isSceneEditorEnabled=true`): grid lines and the
selection highlight still line up with the actors while panning and zooming.
