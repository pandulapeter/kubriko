# Make the Annoyed Penguins camera follow independent of the tick rate

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** all (visible on Web/Wasm and slow Android devices)
**Challenged:** sound
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/Slingshot.kt`,
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/CameraFollow.kt` (new),
`examples/game-annoyed-penguins/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/CameraFollowTest.kt` (new)

Ships in no published artifact (`examples/game-annoyed-penguins` is part of the Showcase).

## Problem

When no pointer is pressed, `Slingshot.update` eases the camera towards the flying penguin (or the slingshot) by a
fixed fraction of the remaining distance per tick:

```kotlin
// Move the camera automatically to keep the slingshot in focus
val cameraPosition = viewportManager.cameraPosition.value
val targetCameraPosition = activePenguin?.body?.position ?: body.position
if (abs(cameraPosition.x - targetCameraPosition.x).raw > 0 || abs(cameraPosition.y - targetCameraPosition.y).raw > 0) {
    viewportManager.addToCameraPosition(
        (targetCameraPosition - cameraPosition).toOffset(viewportManager) * 0.025f
    )
}
```

The convergence speed is therefore per tick: at 30 ticks per second the camera closes the gap half as fast as at 60,
and lags further behind a penguin whose physics is delta-scaled. Every Showcase example runs at the default
`TargetFrameRate.Limit(60)`, so this shows where the device or browser cannot sustain 60 ticks per second (Wasm under
load, low-end Android, a 50 Hz panel), or if the target rate is ever raised. The initial zoom-out in the same function
is already delta-scaled (`0.0005f * deltaTimeInMilliseconds`).

## Fix

Use the frame-rate-independent form of exponential smoothing, calibrated so 60 Hz behaves as today:

- New `actors/slingshot/CameraFollow.kt` with
  `internal fun cameraFollowFactor(deltaTimeInMilliseconds: Int) = 1f - CAMERA_FOLLOW_RETENTION_PER_REFERENCE_TICK.pow(deltaTimeInMilliseconds / REFERENCE_TICK_IN_MILLISECONDS)`,
  `private const val CAMERA_FOLLOW_RETENTION_PER_REFERENCE_TICK = 0.975f` and
  `private const val REFERENCE_TICK_IN_MILLISECONDS = 1000f / 60` (`kotlin.math.pow`). One short comment line: the
  fraction of the gap closed in a tick, 2.5 % per 60 Hz tick.
- In `Slingshot.update` replace `* 0.025f` with `* cameraFollowFactor(deltaTimeInMilliseconds)`.

Values (probe): 0.0121 at 8 ms, 0.0240 at 16 ms, 0.0255 at 17 ms, 0.0489 at 33 ms; two 8 ms ticks close exactly as
much of the gap as one 16 ms tick. One `pow` per tick, no allocation.

`examples/game-annoyed-penguins/CLAUDE.md` does not describe the follow speed; no doc change needed.

## Tests

New `CameraFollowTest` in `examples/game-annoyed-penguins/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/`
(the module's `desktopTest` source set exists — `implementation/BackNavigationTest.kt`):

- `sixtyHertzTickClosesTwoAndAHalfPercent`: `cameraFollowFactor(17)` is within 0.001 of 0.025 (the old constant).
- `twoShortTicksMatchOneLongTick`: `1 - (1 - f(8))²` equals `f(16)` within 1e-6, and likewise `f(11)` three times vs
  `f(33)`.
- `zeroDeltaDoesNotMoveTheCamera`: `cameraFollowFactor(0)` is 0.

## Manual check

Launch a penguin in a browser tab with DevTools CPU throttling (4–6×) and on Desktop; the camera should trail the
penguin by about the same distance in both, and return to the slingshot at the same pace.
