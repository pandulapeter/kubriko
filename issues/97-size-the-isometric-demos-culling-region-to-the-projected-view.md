# Size the isometric demo's culling region to the ground the isometric view actually shows

**Challenged:** amended — `setScaleFactor` is now called only when the computed scale changes (cached in `lastLogicCullingScale`), because calling it every tick boxes a `Scale` per tick, against the zero-per-frame-allocation rule the plan claimed to meet; the projection maths was re-derived from `VolumetricRenderManager.onUpdate` and holds, and the `0.001` minimum is safe (at that scale the square already exceeds the 12 500-unit world).

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (worst on Desktop/Web windows; on phones once zoomed out)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/ControlOverlayManager.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMap.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolder.kt`, `examples/demo-isometric-graphics/CLAUDE.md`

## Problem

Reviewed at `0008d027`. Which actors the isometric view draws is decided by `logicActorManager.visibleActorsWithinViewport` (`VolumetricRenderManager` only creates renderers for holders in that list). The logic viewport is the hidden `KubrikoViewport` inside the 128 dp minimap box (`MiniMap.kt` ~line 73):

```kotlin
KubrikoViewport(
    modifier = Modifier
        .matchParentSize()
        .alpha(0f),
    kubriko = stateHolder.logicKubriko,
)
```

with a fixed scale (`IsometricGraphicsDemoStateHolder.kt` ~line 64):

```kotlin
val logicViewportManager = ViewportManager.newInstance(
    initialScaleFactor = 0.04f,
    ...
)
```

so the culling region is a fixed world-space square of 128 dp / 0.04 — ±1600 world units around the character at density 1 (the ratio below is density-independent, because the isometric viewport's `Dynamic` scene units are pixels too). Nothing ties it to the isometric view's size, zoom or tilt.

Projection check, done from `VolumetricRenderManager.onUpdate` (screen x = (x − y)·zoom·√2, screen y = (x + y)·zoom·√2·tilt/2, camera raised by `FOCUS_HEIGHT`·zoom·√2/0.75 ≈ 64.6 px at the default zoom 0.25): for a 1920×1080 isometric viewport at zoom 0.25, tilt 1, rotation 0, the ground under the bottom-right screen corner is at world offset ≈ (2702, −13) and under the top-right corner ≈ (−352, −3068) — far outside ±1600. About half of each top quadrant and about 30 % of each bottom quadrant of the screen shows ground whose trees, bushes and characters are culled. Tall models make it worse: a ~700-unit tree projects ~330 px upward, so trees rooted just below the bottom edge (where the square ends ~90 px under it) are cut too. Zooming out (down to 0.1) scales every one of those distances by 2.5×, so even a phone in landscape shows empty corners. On top of that the logic `ActorManager` re-culls only every 500 ms (`invisibleActorMinimumRefreshTimeInMillis = 500`), so where the square's edge is on screen, scenery also pops in up to half a second late while walking (maximum speed 0.5 units/ms ⇒ up to 250 units).

## Fix

Drive the logic viewport's scale from the isometric view every isometric tick, and decouple the minimap's drawing scale from it.

1. `IsometricGraphicsDemoStateHolder.kt`: give `logicViewportManager` `minimumScaleFactor = 0.001f` (keep `initialScaleFactor = 0.04f`); `setScaleFactor` clamps to the minimum, and the default 0.2 would pin the scale above what is needed.

2. `ControlOverlayManager.kt`: in `onUpdate`, after the frame-rate logic, call a new private `updateLogicCullingScale()`:

   ```kotlin
   private fun updateLogicCullingScale() {
       val viewportSize = viewportManager.size.value
       val logicViewportSize = logicViewportManager.size.value
       if (viewportSize.isEmpty() || logicViewportSize.isEmpty()) return
       val renderState = volumetricRenderManager.renderState.value
       val isometricScale = viewportManager.scaleFactor.value.horizontal
       val worldUnitInPixels = renderState.zoom * SQRT_2
       val halfWidth = viewportSize.width / (2f * isometricScale)
       val halfHeight = viewportSize.height / (2f * isometricScale) +
           (VolumetricRenderManager.FOCUS_HEIGHT + MAXIMUM_MODEL_HEIGHT) * worldUnitInPixels / 0.75f
       val alongX = halfWidth / worldUnitInPixels
       val alongY = 2f * halfHeight / (worldUnitInPixels * renderState.tilt)
       val radius = sqrt((alongX * alongX + alongY * alongY) / 2f) + MOVEMENT_MARGIN
       val cullingScale = minOf(logicViewportSize.width, logicViewportSize.height) / (2f * radius)
       if (cullingScale != lastLogicCullingScale) {
           lastLogicCullingScale = cullingScale
           logicViewportManager.setScaleFactor(cullingScale)
       }
   }
   ```

   with `private var lastLogicCullingScale = Float.NaN` next to the other private state (`NaN` never equals a
   computed value, so the first tick always applies it).

   - `radius` is the distance from the character to the farthest ground point under any screen corner. It is invariant under `worldRotation`, so the axis-aligned logic square with that half-side covers the view at every rotation.
   - `MAXIMUM_MODEL_HEIGHT` (private const, world units) — the tallest model's top: take the largest `positionZ + sizeZ/2` in `tree.json` and `character.json` × the 1.2 random height factor in `Tree.kt` (≈ 700 at `0008d027`; re-derive, do not copy). The `/ 0.75f` matches the z projection in `VolumetricCuboidRenderer` (`lz / depthEffect * tileHeightMultiplier` = `lz · zoom · √2 / 0.75`, independent of tilt).
   - `MOVEMENT_MARGIN` (private const, world units) — `MainCharacter`'s maximum speed (0.5 units/ms) × the logic re-cull interval (500 ms) = 250, so actors are already in the list when they walk into view between two re-culls.
   - `SQRT_2 = 1.4142135f` as a private const. The computation runs once per tick with primitives only. The
     `lastLogicCullingScale` guard is required, not an optimisation: `setScaleFactor` builds a `Scale` (a value class)
     and hands it to a generic `MutableStateFlow<Scale>`, which boxes it on every call even when the value is
     unchanged. With the guard the steady state (no zoom, tilt or resize) allocates nothing; a boxed `Scale` is only
     created on the ticks where the inputs actually change.
   - A new scale takes effect at the logic `ActorManager`'s next re-cull (≤ 500 ms), which is acceptable for zoom gestures.

3. `MiniMap.kt`: the minimap must keep its look while the logic scale moves. Add `private const val MINI_MAP_SCALE = 0.04f` and use it instead of `stateHolder.logicViewportManager.scaleFactor` for both the sampler call (`scale = MINI_MAP_SCALE`) and the `drawBehind` block (`val scale = MINI_MAP_SCALE`); delete `scaleFactorState` and the `scale > 0f` guards that only protected against the not-yet-initialised flow. Markers of actors outside the 128 dp circle are clipped by the existing `clip(CircleShape)`. Update the comment above the hidden `KubrikoViewport`: the viewport still provides the logic instance's frame loop and pixel size, while its scale is set by `ControlOverlayManager` so that `visibleActorsWithinViewport` covers the isometric view.

4. `examples/demo-isometric-graphics/CLAUDE.md` (~line 29): replace "`logicViewportManager` (scale `0.04`)" with "`logicViewportManager` (scale set every isometric tick by `ControlOverlayManager` so the culling square covers the projected isometric view; the minimap draws at a fixed `0.04`)".

At very low tilt (0.1) or zoom (0.1) the covered area becomes large and most of the world is kept — that is what the screen genuinely shows, so it is the correct result; if it proves too heavy on low-end devices, clamp `radius` to the world's extent from `LogicManager` rather than under-culling.

The minimap sampler now iterates a larger `visibleActorsWithinViewport` every 64 ms; if that shows up in profiling, have `MiniMapSampler.sample` skip actors farther than the minimap radius (64 dp / `MINI_MAP_SCALE` in world units) from the camera before doing any per-marker work.

Note: this module was flattened from the separate Tesselar project; this is one more hand edit in `ui/` to carry over on a future re-sync.

## Tests

None: examples have no test source sets. The formula is a direct inversion of the projection in `VolumetricRenderManager.onUpdate`; the manual check is what confirms it.

## Manual check

**Needs eyes on a screen — this plan was verified by projection math only.** Desktop Showcase, Isometric demo, window maximised on a 1920×1080-or-larger display:
1. Before the fix, note the empty ground (no trees, bushes or characters) in the top-left and top-right regions and the lower corners, and trees popping in at the corners while walking diagonally.
2. After the fix: scenery reaches every screen edge and corner at the default zoom; walking in any direction shows no pop-in at the edges; zooming out to the minimum and tilting fully fills the view within half a second.
3. The minimap looks exactly as before (same zoom level, markers, rotation).
4. Android phone in landscape: pinch-zoom out fully — no empty corners after a moment.
