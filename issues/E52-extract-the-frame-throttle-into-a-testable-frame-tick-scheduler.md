# Extract the viewport's frame-throttle algorithm into an internal FrameTickScheduler with synthetic-timestamp tests

**Kind:** refactor  ·  **Severity:** high  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/FrameTickScheduler.kt` (new)
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/implementation/FrameTickSchedulerTest.kt` (new)
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/performance/HotPathAllocationTest.kt`
- `engine/CLAUDE.md` (Tick Dispatch)

Depends on E07 and E08 (both edit `InternalViewport.kt`; quoted below as after them) and E11 (comment conversions in
the same file).

## Problem

The most intricate timing code in the engine is ~150 lines of mutable locals and a hoisted lambda inside one
`LaunchedEffect` of `InternalViewport` (`InternalViewport.kt:80-232` at 2480325f): the `Limit` subtract-interval
accumulator with half-a-display-frame tolerance, `DisplayDivider` counting the frames a delta spans after a sleep,
the display-interval estimate taken only from frames awaited back to back, the > 2 s gap re-anchor
(`MAXIMUM_FRAME_GAP_IN_MILLISECONDS + lastSleepInMilliseconds`), the catch-up guard, the pause re-anchor and the
sleep sizing:

```kotlin
var lastFrameTime = -1L
var lastProcessedFrameTime = -1L
var phaseInMilliseconds = 0f
var displayFramesSinceTick = 0
var displayFrameInterval = 0f
var hasSkippedDisplayFrames = false
var lastSleepInMilliseconds = 0L
val loopStartTimeMark = TimeSource.Monotonic.markNow()
var lastFrameProcessedAtInMilliseconds = 0L
val onFrame: (Long) -> Unit = { frameTimeInNanoseconds -> ... }     // :110-186
while (isActive) {
    ...                                                            // gate, suspended gate, then:
    if (lastFrameTime != -1L && displayFrameInterval > 0f) {
        val displayFramesUntilTick = when (val targetFrameRate = kubrikoImpl.viewportManager.targetFrameRate.value) { ... }
        if (displayFramesUntilTick >= 2) { ... delay(sleepInMilliseconds) }
    }
    withFrameNanos(onFrame)
}
```

`engine/CLAUDE.md` spends its longest paragraph describing these rules (quantization at 60 Hz yielding 40 fps without
the tolerance, 60 fps on 90 Hz drifting to 45 without the remainder, sleeps sized off the panel interval), yet none of
them is under test: a test can only reach them through a real Compose frame clock.

## Fix

Move the state and decisions, unchanged, into `internal class FrameTickScheduler` in
`implementation/FrameTickScheduler.kt`. Recommended shape (pure, no Compose, no coroutines, no clock of its own):

```kotlin
internal class FrameTickScheduler {
    /** Processes one display frame; returns the delta to emit as a tick, or NO_TICK. */
    fun onFrame(frameTimeInMilliseconds: Long, processedAtInMilliseconds: Long, canTick: Boolean, targetFrameRate: TargetFrameRate): Int
    /** What the loop should sleep before awaiting the next frame: 0 when it should await right away. */
    fun sleepBeforeNextFrame(nowInMilliseconds: Long, targetFrameRate: TargetFrameRate): Long
    /** Called after a sleep actually happened. */
    fun onSlept(sleepInMilliseconds: Long)
    /** The re-anchor the loop does after the suspended gate opens. */
    fun onResumed()
    /** The re-anchor that the first frame and a > 2 s gap trigger. */
    val didReanchor: Boolean   // or a return code from onFrame, so the loop calls metadataManager.onUpdateInternal(0)
}
```

- Every `var` of the `LaunchedEffect` becomes a private field; the bodies of `onFrame`, the sleep computation and the
  resume block move statement for statement. `MAXIMUM_FRAME_GAP_IN_MILLISECONDS` moves with them.
  `NANOSECONDS_PER_MILLISECOND` stays in `InternalViewport.kt` (the conversion stays at the Compose boundary).
- Return codes are primitives (`Int` delta with a sentinel such as `-1` for "no tick", a separate `Int` code for the
  re-anchor) — never a boxed or allocated result.
- The clock stays outside: `InternalViewport` keeps `loopStartTimeMark = TimeSource.Monotonic.markNow()` and passes
  `loopStartTimeMark.elapsedNow().inWholeMilliseconds` in; so does the tick itself
  (`viewportTickSource.tick(delta)`), the `metadataManager.onUpdateInternal(0)` on a re-anchor, the gate
  (`isTickingAllowed`, E08), the suspended `combine(...).first { it }` gate and `delay`.
- `InternalViewport`'s loop is left with: create one scheduler per `LaunchedEffect`, the gate, `delay` of what the
  scheduler says, and a hoisted `onFrame` lambda that converts nanoseconds, calls the scheduler and emits. The lambda
  stays hoisted (it still captures the scheduler, once).

Option: keep the scheduler's methods as they are but let it own `canTick`'s inputs (the tick source, viewport and
state managers) — less plumbing, but no longer pure; not recommended.

## Behaviour
Unchanged: same arithmetic in the same order on the same values. Watch two subtleties while moving:
`hasSkippedDisplayFrames` and `lastSleepInMilliseconds` are reset at the end of every processed frame, and the pause
branch (`canTick == false` with a frame) re-anchors `lastProcessedFrameTime`/phase/divider but not `lastFrameTime`,
whereas the suspended-gate resume resets `lastFrameTime = -1L` too. Both must survive exactly.

## Public API
None (`FrameTickScheduler` is internal; `InternalViewport`'s signature and facade are untouched).

## Tests
`implementation/FrameTickSchedulerTest` (desktopTest, pure), driving synthetic timestamps:
- `DisplayDefault` at 60/120 Hz ticks every frame with the frame delta;
- `Limit(60)` on a 60 Hz grid (16/17 ms jitter) ticks every frame — pins the half-frame tolerance (a full-interval
  rule yields ~40 fps);
- `Limit(60)` on 90 Hz averages 60 ticks per simulated second and `Limit(30)` on 120 Hz 30 — pins the kept remainder;
- `DisplayDivider(2)` on 120 Hz ticks every second frame, also across a simulated sleep spanning three frames;
- a 3 s gap re-anchors (no tick, re-anchor code) while a sleep-extended gap under 2 s + the sleep does not;
- `sleepBeforeNextFrame` for `Limit(30)` on 120 Hz is ~(4 - 0.5) × 8.33 ms minus the processing time, and 0 for a
  target at the panel's own rate;
- the catch-up guard collapses a backlog after a long frame instead of emitting a burst.
Add `frameTickSchedulerDoesNotAllocate` to `HotPathAllocationTest` (`measureAllocatedBytesPerRun` over a few hundred
`onFrame`/`sleepBeforeNextFrame` calls; budget 0 like the `Timer` case).

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileKotlinWasmJs :engine:compileKotlinIosSimulatorArm64 :engine:compileAndroidMain :engine:desktopTest`

`engine/CLAUDE.md` Tick Dispatch, same commit: "Update-rate throttling is applied in `InternalViewport`…" →
"…is decided by `FrameTickScheduler` (fed by `InternalViewport`'s frame loop)…", and "The loop holds a handful of
captured primitives (…)" → "The scheduler holds a handful of primitives (…)"; the rest of the paragraph stays.

## Manual check
On a 60 Hz and a 120 Hz display (desktop, and an Android device with a variable-refresh panel), the Showcase's frame
rate menu still reaches each offered `Limit` and `DisplayDivider` target (debug menu FPS readout), and a backgrounded
then restored app resumes without a jump.
