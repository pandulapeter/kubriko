# Scale the Space Squadron aliens' fire chance by elapsed time instead of rolling once per tick

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** all (visible on Web/Wasm and slow Android devices)
**Challenged:** sound
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/AlienShip.kt`,
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/AlienFireChance.kt` (new),
`examples/game-space-squadron/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/AlienFireChanceTest.kt` (new),
`examples/game-space-squadron/CLAUDE.md`

Ships in no published artifact (`examples/game-space-squadron` is part of the Showcase).

## Problem

`AlienShip.update` rolls a 1-in-80 die on every tick:

```kotlin
if (body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) && Random.nextInt(80) == 0 && stateManager.isRunning.value) {
    val currentTimestamp = metadataManager.activeRuntimeInMilliseconds.value
    val timeSinceLastShot = currentTimestamp - lastShotTimestamp
    if (timeSinceLastShot > 200 && !gameplayManager.isGameOver.value) {
```

so the average time between shots is 80 ticks — 1.33 s at 60 ticks per second, 2.67 s at 30. Every other movement in
the class is delta-scaled (`SPEED * deltaTimeInMilliseconds`, the shrink, the sprite step), so on a device or browser
tab that cannot sustain the default `TargetFrameRate.Limit(60)` (Wasm under load, low-end Android, a 50 Hz panel) the
aliens move at the intended speed but shoot proportionally less, making the game easier exactly where it already runs
worse; raising the target rate would make them shoot more.

## Fix

Roll against a probability proportional to the tick's duration, keeping today's 60 Hz average:

- New `actors/AlienFireChance.kt` with
  `internal fun shouldAlienAttemptShot(deltaTimeInMilliseconds: Int, random: Random = Random) = random.nextFloat() < deltaTimeInMilliseconds / AVERAGE_ALIEN_SHOT_INTERVAL_IN_MILLISECONDS`
  and `private const val AVERAGE_ALIEN_SHOT_INTERVAL_IN_MILLISECONDS = 1333f` (80 ticks × 16.67 ms). KDoc-free is
  fine (internal, unpublished); one short line saying it keeps the old 1-in-80-per-60-Hz-tick average is enough.
- In `AlienShip.update` replace `Random.nextInt(80) == 0` with `shouldAlienAttemptShot(deltaTimeInMilliseconds)`. The
  200 ms cooldown after it stays as it is. `AlienShip.kt` keeps its `kotlin.random.Random` import (the sprite frame,
  power-up rolls and `resetPosition` still use it).

No allocation; `nextFloat()` replaces `nextInt(80)`.

In `examples/game-space-squadron/CLAUDE.md`, the `AlienShip` bullet's "Fires at the player ship with random timing"
becomes "Fires at the player ship with random timing, on average every 1.33 s regardless of the tick rate (at most once
per 200 ms)".

## Tests

New `AlienFireChanceTest` in `examples/game-space-squadron/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/`
(the module's `desktopTest` source set exists — `implementation/BackNavigationTest.kt`):

- `attemptsPerSecondDoNotDependOnTheTickRate`: with `Random(42)`, count attempts over 10 000 000 simulated ms in 8 ms
  ticks and, with `Random(43)`, in 33 ms ticks; both counts are within 3 % of 10 000 000 / 1333 ≈ 7 502. (Seeded, so
  deterministic; the expected spread at that sample size is ~1 %.)
- `zeroDeltaNeverAttempts`: `shouldAlienAttemptShot(0, Random(1))` is false for 1 000 calls.

## Manual check

Play Space Squadron in a browser tab with DevTools CPU throttling (4–6×) and on Desktop; the aliens should fire about
as often (by wall-clock time) in both.
