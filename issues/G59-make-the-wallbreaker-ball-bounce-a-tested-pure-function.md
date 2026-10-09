# Make the Wallbreaker ball bounce a pure, tested function (and decide the top-right corner case).

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Ball.kt`, new `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/BallBounce.kt`, new `examples/game-wallbreaker/src/desktopTest/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/BallBounceTest.kt`, `examples/game-wallbreaker/CLAUDE.md`
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`Ball.onCollisionDetected` resolves the bounce with an eight-branch `when` over the ball's position against the collided object's `axisAlignedBoundingBox` (`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Ball.kt:193–249`), mutating `baseSpeedX` / `baseSpeedY`. It is the core rule of the game and has no test. Reading it:
- **Top-right corner** sets `baseSpeedX = -1; baseSpeedY = -1` — the same as top-left. Every other corner sends the ball away from the brick (bottom-right is `+1, +1`), so top-right was most likely meant to be `+1, -1`.
- Positions exactly on an edge (`==` left/right/top/bottom) match no branch, so the direction is left unchanged.

## Fix
- `internal fun bounceDirection(ballX: Float, ballY: Float, left: Float, top: Float, right: Float, bottom: Float, currentX: Int, currentY: Int): Long` (or two small functions returning `Int`, or writing into the ball's fields) reproducing the `when` exactly — no allocation per collision (no `Pair`; pack into a `Long` or return the eight-way region as an `Int` constant and let `Ball` apply it).
- `Ball` calls it in place of the `when`.
- `BallBounceTest`: one case per region plus the on-edge cases, pinning today's behaviour — the top-right test asserts `-1, -1` with a comment pointing at the decision below.

## Decision
Top-right corner: **fix to `+1, -1` (recommended)** as a separate `Kind: bug` commit after the extraction (the test changes with it), or keep `-1, -1` as intended behaviour. Also decide whether on-edge hits should count as the side they touch (recommended: leave as is unless play shows balls passing through).

## Behaviour
The extraction is behaviour-preserving; the tests prove it. The decision, if taken, changes the bounce off a brick's or the paddle's top-right corner.

## Public API
None.

## Tests
`BallBounceTest` (pure function, desktopTest).

## Verify
`./gradlew :examples:game-wallbreaker:desktopTest`

## Manual check
Play a few levels and aim at brick corners; with the bug fix, a ball hitting a top-right corner bounces up and to the right.
