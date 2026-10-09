# Re-measure the display frame interval while the frame loop sleeps between ticks, and forget it when the timeline restarts

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** all (every `KubrikoViewport` with a throttled `TargetFrameRate`)
**Challenged:** sound
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/FrameTickScheduler.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/implementation/FrameTickSchedulerTest.kt`, `engine/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:engine`. No public API change: `FrameTickScheduler` is internal, and the fix
restores the documented rates of `TargetFrameRate.DisplayDivider` / `Limit`.

## Problem

`FrameTickScheduler` sizes the loop's sleeps by `displayFrameInterval`, which is only ever measured from two display
frames awaited back to back:

```kotlin
val frameDelta = (frameTimeInMilliseconds - lastFrameTime).toInt()
lastFrameTime = frameTimeInMilliseconds
if (!hasSkippedDisplayFrames && frameDelta > 0) {
    displayFrameInterval = frameDelta.toFloat()
}
```

Once a throttled target settles into sleeping between ticks, *every* awaited frame follows a sleep, so the interval is
never measured again — and nothing else clears it (`onResumed()` and the `RE_ANCHOR` branch reset `lastFrameTime`,
`phaseInMilliseconds` and `displayFramesSinceTick`, not `displayFrameInterval`). Whatever value was last measured
before the loop started sleeping is used for good, and `DisplayDivider` also counts frames by it:

```kotlin
is TargetFrameRate.DisplayDivider -> {
    displayFramesSinceTick += if (hasSkippedDisplayFrames) {
        (frameDelta / displayFrameInterval).roundToInt().coerceAtLeast(1)
    } else { 1 }
```

So one wrong measurement locks the rate in:

- **A late frame right before the first sleep.** `DisplayDivider(2)` on a 60 Hz panel: frame 0 anchors, frame 1
  arrives on time, frame 2 is a missed vsync delivered ~33 ms late (a GC pause, the first heavy composition). The
  frame-1→2 delta (~50 ms) becomes the interval, the tick lands, and from then on the loop sleeps
  `1.5 × 50 = 75 ms`, counts the ~83 ms delta as `round(83/50) = 2` frames, ticks — and stays there: **12 fps instead
  of 30, forever** (until the viewport leaves composition). Verified with a probe test (below): average delta 83.3 ms.
- **The panel's rate changes while sleeping.** The window moves from a 60 Hz to a 120 Hz monitor (desktop), a
  variable-refresh panel switches mode (Android, iOS ProMotion), or a laptop leaves battery saver: `DisplayDivider(2)`
  keeps sleeping by the old 16.7 ms interval and settles at **40 fps instead of 60** (probe: 25.0 ms average delta);
  120→60 Hz settles at 60 fps instead of 30 (reviewer's `sim4.py`). `Limit` mostly recovers because its tolerance
  and phase are time-based, but it still sleeps by the stale interval.

`engine/CLAUDE.md` also says "A target at the panel's own rate never sleeps, which is also where Android's panel hint
leaves a `Limit`", which is only true when the panel has a mode equal to the target: the hint picks the slowest mode
that *covers* the target, so `Limit(30)` on a 60/120 Hz panel runs at 60 Hz and sleeps every other frame.

## Fix

In `FrameTickScheduler` (all primitives, nothing allocated per frame):

1. **Forget the interval when the timeline restarts.** Set `displayFrameInterval = 0f` in the `RE_ANCHOR` branch of
   `onFrame` and in `onResumed()`. `sleepBeforeNextFrame` already returns 0 while it is 0, so the loop awaits
   frames back to back until it has measured the panel again (the second frame after the anchor).
2. **Re-measure periodically.** Add `private var intervalMeasuredAtInMilliseconds = -1L` (the frame time of the last
   back-to-back measurement), set it next to `displayFrameInterval = frameDelta.toFloat()` and reset it to `-1L` with
   the interval. In `sleepBeforeNextFrame`, return 0 (await the next frame without sleeping) when
   `lastFrameTime - intervalMeasuredAtInMilliseconds >= INTERVAL_REFRESH_PERIOD_IN_MILLISECONDS` (1 000 ms): the next
   frame is then awaited back to back and refreshes the interval. Cost: about one extra awaited (redrawn) display
   frame per second while throttled; ticks are unaffected because `onFrame`'s tick decision is unchanged.
3. Update the KDoc of `displayFrameInterval` ("only the ones that aren't keep it current") to say it is re-measured
   at least once a second and cleared when the timeline restarts.

Options considered: (a) only resetting on re-anchor/resume — does not fix the late-frame or monitor-change cases,
since neither re-anchors; (c) keeping the minimum of several back-to-back deltas — still never re-measures while
sleeping, so a 60→120 Hz change is missed. **Recommended: 1 + 2** (the reviewer's option b). A Python model of
the fix (scratch `writer-E/fix_sim.py`) settles every scenario on the right rate: late frame → 33.3 ms, 60→120 Hz →
16.7 ms, 120→60 Hz → 33.3 ms, 60→144 Hz → 13.9 ms, `Limit(30)`/`Limit(10)` unchanged; awaited frames per tick rise
from ~1.03 to ~1.07.

In `engine/CLAUDE.md` → Tick Dispatch: say the interval is the delta of the last two frames awaited back to back,
re-measured by skipping one sleep at least once a second and cleared on a re-anchor or resume; replace "A target at
the panel's own rate never sleeps, which is also where Android's panel hint leaves a `Limit`" with "A target at the
panel's own rate never sleeps (Android's panel hint gets a `Limit` there when the panel has a mode at that rate)".

## Tests

In `FrameTickSchedulerTest`, add a helper that models a real vsync grid on a continuous clock rather than frame
indices (the existing `tickingFramesWithSleeps` steps an index, so it can model neither a late frame nor a rate
change):

```kotlin
/** Runs the loop on a continuous clock against a vsync grid whose rate may change, returning the emitted deltas. */
private fun tickDeltasWithSleeps(
    targetFrameRate: TargetFrameRate,
    tickCount: Int,
    refreshRateAt: (timeInMilliseconds: Double) -> Double,
    lateFrameIndex: Int = -1,
    lateByInMilliseconds: Double = 0.0,
): List<Int> {
    val scheduler = FrameTickScheduler()
    val deltas = ArrayList<Int>()
    var now = 0.0
    var frameIndex = 0
    while (deltas.size < tickCount) {
        val sleep = scheduler.sleepBeforeNextFrame(now.roundToLong(), targetFrameRate)
        if (sleep > 0L) {
            scheduler.onSlept(sleep)
            now += sleep
        }
        val period = 1000.0 / refreshRateAt(now)
        var time = if (frameIndex == 0) 0.0 else (floor(now / period + 1e-9) + 1) * period  // first vsync after now
        if (frameIndex == lateFrameIndex) time += lateByInMilliseconds
        frameIndex++
        val result = scheduler.frame(time.roundToLong(), targetFrameRate)
        now = time
        if (result >= 0) deltas.add(result)
    }
    return deltas
}
```

and two tests, asserting on the average of the last 30 deltas (within 1 ms):

- `displayDividerRecoversFromALateFrameBeforeItStartsSleeping` — `DisplayDivider(2)`, 120 ticks at a constant 60 Hz,
  `lateFrameIndex = 2`, `lateByInMilliseconds = 33.3` → ≈ 33.3 ms.
- `displayDividerFollowsARefreshRateChangeWhileSleeping` — `DisplayDivider(2)`, 200 ticks, 60 Hz before 300 ms and
  120 Hz after → ≈ 16.7 ms.

Both were run as an untracked probe at 401298a3 and **fail today** (averages 83.3 ms and 25.0 ms); they pass with
the fix per the model above. Also add `aReAnchorForgetsTheDisplayFrameInterval`: two back-to-back frames 8 ms apart,
a frame 3 s later (`RE_ANCHOR`), then `sleepBeforeNextFrame(now, DisplayDivider(4))` returns 0; and adjust nothing in
the existing tests (all of them still hold: their sleeps start within the first second, and none spans a re-anchor).

## Manual check

Desktop with two monitors at different refresh rates (60 and 120/144 Hz): run the Showcase, open the isometric
graphics demo with the debug menu's FPS counter visible on the 60 Hz screen and leave it idle for 2 s (its
`ControlOverlayManager` then switches to `IDLE_TARGET_FRAME_RATE = TargetFrameRate.DisplayDivider(2)`), drag the
window to the faster screen and let go — the FPS counter should settle at half the new rate within about a second
(today it stays near 40 fps on 120 Hz).
