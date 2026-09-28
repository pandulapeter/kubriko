# Clamp the physics tick delta before adding it to the fixed-timestep accumulator

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `plugin-physics`
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsManagerImpl.kt`, `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/PhysicsAccumulatorTest.kt` (new)

## Problem

`PhysicsManagerImpl.onUpdate` (at 0008d027):

```kotlin
accumulatedTimeInMilliseconds += deltaTimeInMilliseconds
...
if (accumulatedTimeInMilliseconds >= FIXED_TIME_STEP_IN_MILLISECONDS) {
    accumulatedTimeInMilliseconds = 0
}
```

`accumulatedTimeInMilliseconds` is an `Int`. With a remainder carried from the previous tick (anything in `1..15`), a delta near `Int.MAX_VALUE` overflows it to a large negative number; the `while (accumulated >= 16 ...)` loop then never runs and the spiral-of-death guard never fires, so **the simulation is frozen** until ~2^31 ms of ticks have refilled it. Verified with a probe: `tick(20)` then `tick(Int.MAX_VALUE)` leaves the accumulator at `-2147483485` and a moving body does not move in the next ten 16 ms ticks. Only reachable with a custom `TickSource` (or `TickSource.manual()`) that emits a garbage delta, hence low severity — but the result is a silent permanent freeze.

## Fix

Clamp before adding: `accumulatedTimeInMilliseconds += minOf(deltaTimeInMilliseconds, FIXED_TIME_STEP_IN_MILLISECONDS * (MAXIMUM_SUB_STEPS_PER_TICK + 1))`. The bound must be `(MAXIMUM_SUB_STEPS_PER_TICK + 1)` steps, not `MAXIMUM_SUB_STEPS_PER_TICK`: any delta at or above it already runs the full 8 sub-steps and then has its backlog dropped by the guard, so the clamped value produces the same steps and the same reset — the simulation is bit-identical for every delta. (A bound of exactly 8 steps would leave the sub-16 ms remainder in the accumulator instead of dropping it, which changes results.) Name the bound as a `const val` in the companion if that reads better.

## Tests

`PhysicsAccumulatorTest` in `desktopTest`: `Kubriko.newInstance(ActorManager.newInstance(shouldComposeLayers = false, shouldPutFarAwayActorsToSleep = false), PhysicsManager.newInstance(initialGravity = SceneOffset.Zero), tickSource = TickSource.manual())`, `start()`, add one `RigidBody` actor with a `CircleCollisionMask` and `isAffectedByGravity = false`. Set its `physicsBody.velocity` to `(10, 0)` and tick 16 ms at a time until its `physicsBody.position` changes (poll, 2 s timeout — this waits for the async actor registration). Then `tick(20)`, `tick(Int.MAX_VALUE)`, record the position, tick 16 ms ten times and assert the position changed. Dispose at the end.

Run `./gradlew :plugins:physics:desktopTest`.

## Manual check

None — not reachable through the viewport tick source.
