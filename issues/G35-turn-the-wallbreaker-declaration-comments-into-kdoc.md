# Turn the Wallbreaker `Ball`/`Paddle` declaration comments into KDoc and drop the fixed-bug history.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Ball.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Paddle.kt`

## Problem
- `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Ball.kt:95–99` documents `fun snapToPaddle()` with a `//` block that ends in history: "…which previously made the paddle appear to move one frame ahead of the ball at low frame rates."
- `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Paddle.kt:61–62` (`// On desktop after each detected movement we programmatically move the cursor…` above `private var shouldMovePaddle`) and :71–72 (`// The Ball glues itself to the paddle…` above `fun attachBall`) document declarations with `//`.

The code style documents declarations with KDoc only, and keeps a regression guard to the constraint, not the history.

## Fix
- `Ball.snapToPaddle`:
  ```kotlin
  /**
   * Keeps a ball that is waiting for launch on the paddle's x. The paddle calls this at the end of its own update (and the
   * ball from its update), so within a frame the ball always reflects the paddle's final position, whatever the actor
   * update order.
   */
  ```
- `Paddle.shouldMovePaddle`: `/** On desktop the cursor is moved back to the centre after each movement; this flag filters out the event that move causes. */`
- `Paddle.attachBall`: `/** Remembers the ball waiting for launch so the paddle can re-sync it at the end of its own update and the two move together within a frame. */`
- Leave the statement-level `//` comments inside function bodies (Paddle.kt:110–111, :174–175) as they are.

## Behaviour
Comments only.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:game-wallbreaker:compileKotlinDesktop`

## Manual check
none
