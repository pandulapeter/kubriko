# Move the viewport resize and aspect-ratio scale logic out of InternalViewport into ViewportManagerImpl

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManagerImpl.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/manager/ViewportContractTest.kt`

Runs after E06 (same `ViewportManagerImpl.kt`).

## Problem

The Composable decides how the scale factor follows the viewport size, a pure computation that belongs to the
manager and that no test can reach from a `Modifier.onSizeChanged` lambda (`InternalViewport.kt:253-271`):

```kotlin
.onSizeChanged { intSize ->
    val widthPx = intSize.width.toFloat()
    val heightPx = intSize.height.toFloat()
    kubrikoImpl.viewportManager.run {
        updateSize(Size(widthPx, heightPx))
        scaleFactorMultiplier.update {
            when (val mode = aspectRatioMode) {
                ViewportManager.AspectRatioMode.Dynamic -> Scale.Unit
                is ViewportManager.AspectRatioMode.FitHorizontal -> (widthPx / mode.width.raw).let { Scale(it, it) }
                is ViewportManager.AspectRatioMode.FitVertical -> (heightPx / mode.height.raw).let { Scale(it, it) }
                is ViewportManager.AspectRatioMode.Fixed -> (widthPx / mode.width.raw).let { Scale(it, it) }
                is ViewportManager.AspectRatioMode.Stretched -> Scale(
                    horizontal = widthPx / mode.size.width.raw,
                    vertical = heightPx / mode.size.height.raw,
                )
            }
        }
    }
}
```

(Inside `run`, `aspectRatioMode` is `ViewportManagerImpl.aspectRatioMode`.)

## Fix

1. Add to `ViewportManagerImpl`, next to `updateSize`, the same statements in the same order:
   ```kotlin
   fun onViewportSizeChanged(widthPx: Float, heightPx: Float) {
       updateSize(Size(widthPx, heightPx))
       scaleFactorMultiplier.update {
           when (val mode = aspectRatioMode) {
               AspectRatioMode.Dynamic -> Scale.Unit
               is AspectRatioMode.FitHorizontal -> (widthPx / mode.width.raw).let { Scale(it, it) }
               is AspectRatioMode.FitVertical -> (heightPx / mode.height.raw).let { Scale(it, it) }
               is AspectRatioMode.Fixed -> (widthPx / mode.width.raw).let { Scale(it, it) }
               is AspectRatioMode.Stretched -> Scale(
                   horizontal = widthPx / mode.size.width.raw,
                   vertical = heightPx / mode.size.height.raw,
               )
           }
       }
   }
   ```
   (`AspectRatioMode` resolves as the nested type of the supertype `ViewportManager`; write
   `ViewportManager.AspectRatioMode` if the compiler asks.) Keep `updateSize` — tests and `ActorTestHarness` call it.
2. In `InternalViewport.kt` the lambda becomes
   `.onSizeChanged { intSize -> kubrikoImpl.viewportManager.onViewportSizeChanged(intSize.width.toFloat(), intSize.height.toFloat()) }`.
3. Trim imports of `InternalViewport.kt` that become unused: `androidx.compose.ui.geometry.Size`,
   `com.pandulapeter.kubriko.types.Scale`, `kotlinx.coroutines.flow.update` (keep `ViewportManager`, still used by the
   aspect-ratio `when` of the inner `Box`). `ViewportManagerImpl.kt` already imports `Size`, `Scale` and `update`.

## Behaviour
Unchanged: the same two state updates in the same order on the same (main) thread.

## Public API
None (`ViewportManagerImpl` is internal).

## Tests
Add to `ViewportContractTest` (uses its `withViewport(viewportManager)` helper and `assertClose`):
`onViewportSizeChangedScalesByAspectRatioMode` — for `Dynamic`, `FitHorizontal(400f.sceneUnit)`,
`FitVertical(300f.sceneUnit)`, `Fixed(ratio, width = 400f.sceneUnit, …)` and `Stretched(SceneSize(400, 300))`, call
`onViewportSizeChanged(800f, 600f)` and assert `size.value == Size(800f, 600f)` and `currentScaleFactor()` equals the
raw scale (1 after `setScaleFactor(1f)`) times `Unit`, `2`, `2`, `2`, and `(2, 2)` respectively; then with
`Stretched(SceneSize(400, 200))` assert `(2, 3)`, which only the per-axis branch produces. Check the
`AspectRatioMode` constructors in `ViewportManager.kt` for the exact parameter names before writing it.

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:desktopTest`

## Manual check
none (the Showcase demos with fixed and fit aspect ratios still scale when the window is resized — covered by the
test)
