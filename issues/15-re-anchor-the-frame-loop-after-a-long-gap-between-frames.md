# Re-anchor the viewport frame loop after a long gap between display frames

**Challenged:** amended — the plan told games that want to simulate time away to read `MetadataManager.totalRuntimeInMilliseconds`, but that counter is the sum of emitted deltas and so stops including the gap after this fix; the advice now points at a wall clock, and the KDoc/docs say the skipped time is not added to the runtime counters.

**Decision needed:** with `TickSource.viewportFrames(shouldPauseOnFocusLoss = false)`, returning from the background emits the entire time away as one tick delta (a 10-minute background gives `update(600000)`). Treat a gap far above the display interval as a resume and restart the timeline? — recommended: yes, for gaps above 2 seconds; games that want to simulate time away must measure it with their own wall clock (e.g. `TimeSource.Monotonic` or the platform's lifecycle callbacks) — not with `MetadataManager.totalRuntimeInMilliseconds`/`activeRuntimeInMilliseconds`, which are sums of the emitted deltas and so will not include the skipped gap either.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Android, iOS, web (backgrounded tabs), desktop (minimized windows)  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt` (KDoc of `viewportFrames`), `engine/CLAUDE.md` (Tick Dispatch), `documentation/TICK_SOURCE.md` (Viewport frames)

Apply after plans 12-14 (same file).

## Problem

In `InternalViewport`'s `onFrame` (0008d027 ~98-157) every emitting branch passes the time since the last tick:

```kotlin
TargetFrameRate.DisplayDefault -> {
    viewportTickSource.tick((frameTimeInMilliseconds - lastProcessedFrameTime).toInt())
    lastProcessedFrameTime = frameTimeInMilliseconds
}
```

The timeline is only re-anchored when the gate closes (unfocused, stopped, unsized). With `shouldPauseOnFocusLoss = false` the gate stays open while the app is in the background, but no display frames arrive, so the first frame after returning carries the whole absence: physics bodies tunnel, timers fire once with a huge overshoot, `Limit` collapses its phase but still emits the full delta. `shouldPauseOnFocusLoss = false` exists for games that keep running when the window merely loses focus (e.g. a desktop window behind another), not for simulating minutes of absence in one step.

## Fix

In `onFrame`, right after computing `frameDelta`, if `frameDelta > MAXIMUM_FRAME_GAP_IN_MILLISECONDS` (a new `private const val` = `2000`), handle the frame like the first one: set `lastFrameTime` and `lastProcessedFrameTime` to `frameTimeInMilliseconds`, reset `phaseInMilliseconds` and `displayFramesSinceTick`, call `kubrikoImpl.metadataManager.onUpdateInternal(0)` as the first-frame branch does, and skip ticking on that frame; do not let it update `displayFrameInterval`. The loop's own throttling sleeps must not trip it: `Limit(1)` sleeps up to ~1 s and a large `DisplayDivider` (e.g. 200 on a 60 Hz panel) over 3 s. So keep the length of the last sleep in a captured `var lastSleepInMilliseconds` (0 when the frame was awaited without sleeping, i.e. `hasSkippedDisplayFrames == false`) and re-anchor only when `frameDelta > MAXIMUM_FRAME_GAP_IN_MILLISECONDS + lastSleepInMilliseconds`.

No allocation: primitives only.

**Alternative A:** clamp the delta instead (emit `min(delta, 2000)`), which keeps ticking on that frame but lies about elapsed time by a bounded amount. **Alternative B:** keep the behaviour and document it on `viewportFrames(shouldPauseOnFocusLoss)`.

KDoc of `TickSource.viewportFrames(shouldPauseOnFocusLoss)`: "A gap of more than two seconds between display frames (the app was in the background) restarts the timeline instead of being emitted as one delta; that time is not added to `MetadataManager.totalRuntimeInMilliseconds` either." Docs: `engine/CLAUDE.md` → *Tick Dispatch* (one sentence next to the existing "Resuming: anchor the timeline" behaviour); `documentation/TICK_SOURCE.md` → *Viewport frames*.

## Tests

No unit test: the logic lives inside the `withFrameNanos` loop of a Composable. (If the executor extracts the per-frame decision into a small internal pure class — not required — test that a 600 000 ms frame gap yields no tick and the following 16 ms frame yields a 16 ms tick.)

## Manual check

Android: in an example started with `viewportFrames(shouldPauseOnFocusLoss = false)` (temporarily change one, e.g. the Physics demo), background the app for 30 s and return — bodies must not jump. Desktop: minimize the window for 30 s, same check.
