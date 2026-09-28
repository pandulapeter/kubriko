# Document that `SceneOffset.toOffset` converts a scene-space vector and ignores the camera

**Challenged:** amended — `Offset.div(Scale)` returns an `Offset`, not a `SceneOffset`, and `[Offset.div]` resolves ambiguously against Compose's member `div(Float)`; the pointer now names the full expression. Shares `SceneOffsetExtensions.kt` with 03: land after 03.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneOffsetExtensions.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/OffsetExtensions.kt`

## Problem

`SceneOffsetExtensions.kt` documents both `toOffset` overloads as converting to "a screen [Offset]":

```kotlin
/**
 * Converts this [SceneOffset] to a screen [Offset].
 */
fun SceneOffset.toOffset(viewportManager: ViewportManager): Offset = toOffset(
    viewportScaleFactor = (viewportManager as ViewportManagerImpl).currentScaleFactor(),
)

/**
 * Converts this [SceneOffset] to a screen [Offset] using the given [viewportScaleFactor].
 */
fun SceneOffset.toOffset(
    viewportScaleFactor: Scale,
): Offset = Offset(
    x = x.raw * viewportScaleFactor.horizontal,
    y = y.raw * viewportScaleFactor.vertical,
)
```

The code only multiplies by the scale factor: it converts a *displacement* (a distance/direction in scene units) into pixels, and ignores the camera position and the viewport size. Read next to its apparent inverse in `OffsetExtensions.kt`, which *is* a position conversion —

```kotlin
/**
 * Converts this screen [Offset] to a [SceneOffset].
 *
 * @param viewportManager The [ViewportManager] used for conversion.
 */
fun Offset.toSceneOffset(viewportManager: ViewportManager): SceneOffset = toSceneOffset(
    viewportCenter = viewportManager.cameraPosition.value,
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = (viewportManager as ViewportManagerImpl).currentScaleFactor(),
)
```

— a consumer will reasonably call `actor.body.position.toOffset(viewportManager)` expecting the actor's on-screen pixel position and get a value that is wrong by the camera offset and the viewport half-size. `toSceneOffset(...).toOffset(...)` does not round-trip a position, only a difference of two positions (which is exactly what `ViewportContractTest.screenToSceneMapsTheVisibleArea` and `SceneOffsetExtensionsTest.screenToSceneConversionsMapTheCenterAndPreserveDeltas` check, and what the test asserting `toOffset` is unchanged by `setCameraPosition` pins down).

The only in-repo caller already uses it as a vector — `examples/game-annoyed-penguins/.../actors/slingshot/Slingshot.kt`: `(targetCameraPosition - cameraPosition).toOffset(viewportManager) * 0.025f`. `../Tesselar` does not call `toOffset` or `toSceneOffset`. The `documentation/*.md` guides and `engine/README.md` never mention `toOffset`; `GETTING_STARTED_07.md` and `_11.md` use `toSceneOffset` correctly for a pointer position, so no Markdown change is needed.

Siblings: `Size.toSceneSize` (both overloads, `SizeExtensions.kt`) converts a size by scale only, which is correct for a size and already says so — no change. `Offset.toSceneOffset` is correct but should name itself a position conversion so the contrast is visible from either side.

## Fix

Docs only; no code, signature or behaviour change. In `SceneOffsetExtensions.kt`:

```kotlin
/**
 * Converts this scene-space vector (a distance or direction, such as the difference of two positions) to screen pixels,
 * applying only the viewport's current scale factor.
 *
 * This is not a position conversion: the camera position and the viewport size are ignored, so it does not give an
 * actor's on-screen location, and it is not the inverse of [Offset.toSceneOffset].
 */
fun SceneOffset.toOffset(viewportManager: ViewportManager): Offset = …

/**
 * Converts this scene-space vector to screen pixels by multiplying it with [viewportScaleFactor].
 * Like the [ViewportManager] overload, it ignores the camera position; see there.
 */
fun SceneOffset.toOffset(
    viewportScaleFactor: Scale,
): Offset = …
```

In `OffsetExtensions.kt`, make both `toSceneOffset` KDocs say they convert a screen *position* (pixels from the viewport's top-left corner) to a scene *position*, taking the camera position, viewport size and scale factor into account, and show how to convert a pixel vector instead (`Offset.div(Scale)` in the same file returns an `Offset`, so wrap it in `SceneOffset(...)`, as `ViewportManagerImpl.addToCameraPosition` does; don't link `[Offset.div]`, which KDoc resolves to Compose's member `div(Float)`):

```kotlin
/**
 * Converts this screen position (in pixels, relative to the viewport's top-left corner) to the scene position it shows,
 * using the camera position, viewport size and scale factor of [viewportManager].
 * To convert a pixel distance or direction instead, divide it by the scale factor: `SceneOffset(pixels / viewportManager.scaleFactor.value)`.
 *
 * @param viewportManager The [ViewportManager] used for conversion.
 */
```

and the explicit-parameter overload likewise ("Converts this screen position … to the scene position it shows."), keeping its `@param` lines.

## Tests

None: a KDoc-only change. The existing `ViewportContractTest.screenToSceneMapsTheVisibleArea` already pins the documented behaviour (camera-independent `toOffset`, delta round-trip).

## Manual check

None.
