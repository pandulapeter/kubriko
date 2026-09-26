# Performance work in progress

Handoff notes for the performance pass on branch `claude/trusting-pascal-er6vql`, driven by Tesselar (the
game in `pandulapeter/tesselar`, same branch name there, which holds its own `PERFORMANCE_PLAN.md`). Delete each
item once it is done, and this file once it is empty.

**Constraints:** no visible quality loss, no per-frame allocation, public API unchanged.

## 0. Review status

An adversarial review of `d8f7ee0`, `746068f` and `1249795` is **done**. Its two confirmed findings are handled:
- `8bfecd6` fixes the gamepad poll boxing a focus direction every tick.
- Android no longer caching generative fills as layers is documented in `plugins/shaders/CLAUDE.md`, and Tesselar
  caches its paused scene itself.

`8bfecd6` itself hasn't been reviewed.

## 1. Build and verify (nothing on this branch has been compiled yet)

The cloud session that wrote these commits could not reach Google Maven, so every change was verified by reading
source only. Run `./gradlew build` and the showcase on every platform, then check:

- **`229d8b2` (frame pacing):** a `Limit` target below the panel rate should sleep between ticks, and the achieved
  rate must still match the target. Check 60 on 60/90/120 Hz, 45, and 30.
- **`d8f7ee0` (plugin-shaders).** Plain `Shader`s are drawn as a direct fill from a draw modifier instead of a layer
  `RenderEffect`, and `UniformCache` skips re-sending unchanged uniforms.
  - Compare the showcase's FogShader, GalaxyShader and shader-animation demos, and Tesselar's water, clouds and
    rain, against `main`. Opaque output must be identical; translucent output may differ by at most 1/255 per
    channel.
  - Check nesting: an outer generative shader replaces the layer, and an inner one is what an outer
    `ContentShader` reads.
  - Check the fallback on Android below 13.
- **`746068f` (plugin-gamepad-input).** The focus-navigation frame loop sleeps while no navigation input is held
  and while the window is unfocused. Check:
  - quick taps of SOUTH and EAST are neither missed nor doubled;
  - a held direction repeats as before;
  - a menu opened under a held button doesn't act on it;
  - focus loss and regain behave correctly.

  Known trade-off: a press shorter than about one poll interval that is released before the next display frame
  can be missed. If that shows up, latch press edges in `onUpdate` and consume them in the frame.
- **`1249795` (engine).** Check:
  - layer canvases are keyed and live in their own restart scope, so z-order still follows `layerIndices`;
  - headless `ActorManager`s keep constant layer and overlay lists;
  - `InternalViewport` calls `withFrameNanos` with the hoisted lambda (millisecond conversion inside).

- **Pointer input** (`ce67ad2`, container only): every pointer event now
  reaches actors once. Check Tesselar's wheel zoom on desktop and web. The engine used to report each wheel turn
  twice, and Tesselar's `isWheelZoomHandled` only swallowed the first, so a notch zoomed by Tesselar's step plus
  the engine's own. It should now zoom by Tesselar's step alone, as its docs intend, which feels smaller per notch.

Then release, and bump the version in Tesselar.

## 2. Audit findings not implemented yet

These were found by reading code and not yet adversarially verified; re-check each before implementing.

1. **`SyncStateFlow.value` boxes `Scale`/`SceneOffset` on every read**
   (`engine/.../implementation/SyncStateFlow.kt`). The engine reads `scaleFactor` once per layer per frame, even for
   layers holding only overlays. Proposed fixes:
   - Compute the transform only for layers with `Visible` actors.
   - Add an internal unboxed `currentScaleFactor()`.
   - Memoize the result by input identity.
2. **`MetadataManagerImpl` boxes two `Long`s per tick** into `totalRuntimeInMilliseconds` /
   `activeRuntimeInMilliseconds`, which nothing collects in Tesselar. Keeping `.value` and collection semantics
   identical needs care. Optional.
3. **iOS copies every triangle batch into bucket mirrors** (`engine/src/iosMain/.../TriangleMesh.ios.kt`), although
   the native draw takes explicit counts. The fix is a pinned fast path like `WasmTriangleBridge`. It couples to
   Skiko's ABI, so only do it if the saving, roughly 0.2-0.5 ms per frame, is worth the upkeep.
4. **`slidingMovement` allocates a `CollisionResult` per overlapping obstacle per iteration**
   (`plugins/collision/.../CollisionMaskExtensions.kt`). Track the deepest overlap in scratch floats instead,
   keeping the public functions bit-identical.
