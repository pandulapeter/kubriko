# Restart the fixed-frequency tick source's timeline after the process was suspended instead of emitting the whole absence as one delta

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** Android, iOS, Web (any platform that freezes or throttles a background process)
**Challenged:** sound
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/FixedFrequencyTickSource.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt` (KDoc of `fixedFrequency`), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/helpers/FixedFrequencyTickSourceTest.kt`, `documentation/TICK_SOURCE.md`, `engine/CLAUDE.md`, root `CLAUDE.md` (TickSource table row)

Ships in `io.github.pandulapeter.kubriko:engine`. **Changes an observable behaviour of the public
`TickSource.fixedFrequency()`** — see Decision.

## Problem

`FixedFrequencyTickSource` emits the measured time since the previous tick, however long it was:

```kotlin
val currentTime = clock.nowInNanoseconds()
emitTick(((clock.nowInNanoseconds() - lastTickTime) / NANOSECONDS_PER_MILLISECOND).toInt())
lastTickTime = currentTime
```

When the process is frozen — an Android app in the cached state, an iOS app suspended in the background, a laptop
lid closed — the coroutine's `delay` simply resumes afterwards, and the monotonic clock (`TimeSource.Monotonic`,
`System.nanoTime` on the JVM/Android, `performance.now()` on the web) has kept running. The first tick afterwards
carries the whole absence: ten minutes away is one `update(600_000)` to every Manager and `Dynamic` actor —
projectiles jump across the level, physics integrates one enormous step, `Timer`s fire. In a hidden browser tab
`setTimeout` is throttled to once per second and, with Chrome's intensive throttling, once per minute, so every tick
is a 1 000 – 60 000 ms delta. `MetadataManager.totalRuntimeInMilliseconds` / `activeRuntimeInMilliseconds` also
absorb the absence.

`TickSource.viewportFrames()` already handles exactly this: a gap of more than two seconds re-anchors the timeline
(KDoc: "A gap of more than two seconds between display frames … restarts the timeline instead of being emitted as one
delta"), so the same game behaves differently depending on which tick source it uses. The only existing test of a
stall, `fallingBehindEmitsOneTickCarryingTheStallThenReSyncs`, pins a 55 ms stall being emitted as one delta, which
is the intended "re-sync" behaviour for short stalls and stays unchanged.

(Minor, same lines: the delta re-reads the clock instead of using `currentTime`, so it can be a few microseconds
longer than the interval it then anchors to; use `currentTime`.)

## Fix

In `onStart()`'s loop, after reading `currentTime`:

```kotlin
val elapsedInNanoseconds = currentTime - lastTickTime
if (elapsedInNanoseconds > targetIntervalInNanoseconds + MAXIMUM_TICK_GAP_IN_NANOSECONDS) {
    emitTick(0)            // the timeline restarts, like the first tick
    nextTickStart = currentTime
} else {
    emitTick((elapsedInNanoseconds / NANOSECONDS_PER_MILLISECOND).toInt())
    nextTickStart += targetIntervalInNanoseconds
    if (...) nextTickStart = currentTime   // existing re-sync check, unchanged
}
lastTickTime = currentTime
```

with `private const val MAXIMUM_TICK_GAP_IN_NANOSECONDS = 2_000_000_000L` (the same two seconds as the viewport
loop's `MAXIMUM_FRAME_GAP_IN_MILLISECONDS`, measured beyond the configured interval so `fixedFrequency(1)` is never
affected by its own spacing). Extend the `fixedFrequency` KDoc in `TickSource.kt` with the sentence the
`viewportFrames` KDoc has ("A gap of more than two seconds beyond the tick interval (the process was suspended) restarts
the timeline: that tick has a delta of `0` instead of the whole absence."), and say the same in
`documentation/TICK_SOURCE.md` → Fixed frequency, the TickSource table row of the root `CLAUDE.md` ("Re-syncs if
behind; a gap over 2 s restarts the timeline with a 0 delta"), and `engine/CLAUDE.md` if it describes
`FixedFrequencyTickSource`.

`fixedRate()` is not affected: it emits the configured interval, never a measured one.

## Decision

Behaviour a consumer can observe changes only for gaps over ~2 s. Options:

- **A (recommended):** re-anchor and emit one tick with delta `0`, as the first tick does — Managers still get an
  `onUpdate` (so e.g. a game that polls in `onUpdate` resumes on time), and no absence reaches the runtime counters.
- **B:** re-anchor and emit no tick for that wake-up (closest to `viewportFrames`, which reports the re-anchor to
  `MetadataManager` only; a custom `TickSource` cannot do that, so this would be one skipped tick).
- **C:** clamp the delta to a maximum (e.g. 2 000 ms) — still a large step, and the cap is arbitrary.
- **D:** keep the behaviour and document that games must clamp the delta themselves.

## Tests

In `FixedFrequencyTickSourceTest` (virtual-time harness already present: `recordDeltas` with its `stall` callback
that advances the fake `TickClock`), add `aGapLongerThanTwoSecondsRestartsTheTimeline`:

```kotlin
val deltas = recordDeltas(virtualMilliseconds = 50) { tickCount, stall ->
    if (tickCount == 3) stall(10 * 60_000)
}
assertEquals(listOf(0, 10, 10, 0, 10, 10, 10), deltas)
```

Today it yields `[0, 10, 10, 600000, 10, 10, 10]`. `fallingBehindEmitsOneTickCarryingTheStallThenReSyncs` (55 ms)
must keep passing unchanged; add a boundary case with a 2 000 ms stall at 100 Hz (2 000 ≤ 10 + 2 000) being emitted
as `2000`.

## Manual check

Android: run a game built on `TickSource.fixedFrequency()` with an actor moving at constant speed, put the app in the
background for a few minutes with Developer options → "Suspend execution for cached apps" enabled, return — the actor continues from where it was instead of jumping.
Web: the same in a background tab for over a minute.
