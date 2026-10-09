# Cap the Wallbreaker ball's step so one long frame cannot carry it through the paddle

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** all (most likely on Web/Wasm and low-end Android)
**Challenged:** sound
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Ball.kt`,
`examples/game-wallbreaker/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/BallStepTest.kt` (new),
`examples/game-wallbreaker/CLAUDE.md`

Ships in no published artifact (`examples/game-wallbreaker` is part of the Showcase).

## Problem

`Ball.update` moves the launched ball by the full tick delta, checks the bottom edge on the *next* position, and leaves
the paddle to `CollisionManager`, which only looks at where the ball ends up after the tick:

```kotlin
val speed = min(InitialSpeed + SpeedIncrement * scoreManager.score.value, MaximumSpeed)
val nextPosition = body.position + SceneOffset(speed * baseSpeedX, speed * baseSpeedY) * deltaTimeInMilliseconds
...
if (nextPosition.y > viewportBottomRight.y) {
    state = State.GAME_OVER
    gameManager.onGameOver()
    audioManager.playGameOverSoundEffect()
}
```

Geometry at HEAD (401298a3): the viewport is `AspectRatioMode.Fixed(ratio = 1f, width = 1200.sceneUnit)`, so its bottom
edge is y = 600. `Paddle` sits at y = 550 with `Height = 40.sceneUnit` and a centred pivot (box 530..570); the ball's
`Radius = 20f.sceneUnit`. The circle overlaps the paddle only while its centre is within y 510..590 — an **80 su window**.
The vertical speed is `speed * baseSpeedY` with `|baseSpeedY| = 1`, capped at `MaximumSpeed = 1.8f.sceneUnit` per ms, so a
single tick longer than 80 / 1.8 ≈ **44.4 ms** can carry the ball from above 510 straight past 590 (or past 600, which
ends the game in the same tick, before collision runs). At the starting speed (0.6) the threshold is ~133 ms. The engine
only re-anchors gaps longer than `MAXIMUM_FRAME_GAP_IN_MILLISECONDS = 2_000L` (`FrameTickScheduler`), so a 45–2000 ms
hitch (GC pause, tab throttling, a Wasm frame spike, a slow Android device) is delivered as one delta. The player loses
a ball that visibly reached the paddle. Bricks (`Brick.Height = 40f.sceneUnit`) have the same 80 su window, so a long
frame can also skip a brick row.

Clamping only the ball's movement delta does not desync anything else: bricks are static, scoring is per hit, the
`Paddle` keeps its own (delta-scaled) movement, `BrickPopEffect` is independent, and nothing derives game state from
the ball's travelled distance. During a hitch the ball simply moves a little less than wall-clock time would suggest.

## Fix

Recommended (option a): clamp the delta the ball moves by.

- In `Ball`'s companion add `const val MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS = 33` (≈ 30 fps; 1.8 × 33 = 59.4 su,
  well inside the 80 su window) and make `MaximumSpeed` and `Radius` `internal` instead of `private` so the test can
  pin the invariant.
- In the `State.LAUNCHED` branch compute the step from the clamped delta:
  `val movementDeltaInMilliseconds = deltaTimeInMilliseconds.coerceAtMost(MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS)` and
  multiply by it instead of `deltaTimeInMilliseconds`. Use `coerceAtMost` rather than `min`: the file already imports
  `com.pandulapeter.kubriko.helpers.extensions.min` for `SceneUnit`.
- Nothing else changes (no allocation, no new state).

Option b — sweep the ball's segment against the paddle and bricks each tick (as Space Squadron's bullet uses a segment
cast) — is exact at any frame time but needs a collision query and resolution that duplicate what `CollisionManager`
and `bounceRegion` already do; not worth it for a demo game. Not recommended.

Update the `Ball` bullet in `examples/game-wallbreaker/CLAUDE.md` (the one ending "Speed = `InitialSpeed(0.6) + ...`,
capped at `MaximumSpeed(1.8)`") with one sentence: a tick moves the ball by at most 33 ms worth of travel, so a long
frame slows it for that frame rather than carrying it past the 80 su paddle or brick window.

## Tests

New `BallStepTest` in `examples/game-wallbreaker/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/`
(the module's `desktopTest` source set already exists — `BallBounceTest` — and has `:tools:test-fixtures` wired by the
convention plugin, though this test needs none of it):

- `maximumStepFitsInsideThePaddleWindow`: `Ball.MaximumSpeed.raw * Ball.MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS` is less
  than `(Paddle.Height + Ball.Radius * 2).raw` (59.4 < 80).
- `maximumStepFitsInsideTheBrickWindow`: the same against `(Brick.Height + Ball.Radius * 2).raw`.

These pin the invariant if someone later raises the speed cap or the clamp. A full `Ball.update` test through a
`newManualKubriko` is not practical: a headless instance has no viewport size, so `Ball.update`'s
`constrainedWithin(viewportTopLeft, viewportBottomRight)` clamps the ball to a zero-size box, and `Ball` needs the
game's `AudioManager` (which starts `MusicManager` playback on focus) and `GameplayManager`.

## Manual check

Nothing in the Showcase changes the tick rate (every game runs at the default `TargetFrameRate.Limit(60)`), so either
run Wallbreaker in a browser with DevTools CPU throttling (6×), or temporarily pass
`initialTargetFrameRate = TargetFrameRate.Limit(15)` (~67 ms ticks) to the game's `ViewportManager.newInstance` on
Desktop. Temporarily raise `InitialSpeed` to `MaximumSpeed` and confirm the ball still bounces off the paddle and
breaks every brick row it reaches instead of passing through. Revert the temporary changes.
