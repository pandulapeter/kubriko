# Detect Wallbreaker's cleared level from a brick count kept on the tick thread, and add the shaders and UI manager only once

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (the race needs a multi-threaded dispatcher: Android, Desktop, iOS; the actor growth happens everywhere)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/managers/GameplayManager.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Ball.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/Brick.kt`, `examples/game-wallbreaker/CLAUDE.md`

## Problem

Reviewed at `0008d027`. Two defects in the same level lifecycle.

**1. The last brick can fail to clear the level (soft-lock).** `GameplayManager.onInitialize` (~line 64) decides "level cleared" in a collector that runs on the Kubriko scope, which is `Dispatchers.Default`:

```kotlin
scoreManager.score
    .onEach { if (actorManager.allActors.value.isNotEmpty() && actorManager.allActors.value.filterIsInstance<Brick>().isEmpty()) onLevelCleared() }
    .launchIn(scope)
```

`Ball.onCollisionDetected` (~lines 181–188, tick thread) does

```kotlin
actorManager.remove(collidable)
actorManager.add(BrickPopEffect(...))
scoreManager.incrementScore()
```

`remove` only enqueues; `ActorManagerImpl`'s batch loop (another `Dispatchers.Default` coroutine) publishes the new `allActors` later. When the collector resumes before the batch loop has published, it still sees the last brick, does nothing, and the score never changes again (there is nothing left to hit). The ball bounces forever in an empty field; the level never advances until the player restarts.

A second, rarer path: `restartGame()` issues `remove(bricks + ball + paddle)`, then `initializeScene()` issues `add(...)`, then `resetScore()`. If the batch loop publishes the removal before the addition is enqueued and the collector runs in between, it sees no bricks and fires a spurious `onLevelCleared()` (level-cleared sound, hues re-randomised, a second `Ball` added).

The same stale-list window also lets a brick that is pending removal be hit a second time on the next frame (the `when` in `onCollisionDetected` has branches that leave the direction unchanged), which double-counts the score.

**2. `allActors` grows on every level clear and restart.** `GameplayManager` is a `Group` whose `actors` is

```kotlin
override val actors by lazy {
    bricks + listOf(
        paddle,
        SmoothPixelationShader(),
        VignetteShader(),
        ChromaticAberrationShader(),
        uiManager,
    )
}
```

and `initializeScene()` re-adds the whole group each level: `actorManager.add(listOf(Ball(paddle), this))`. The three shaders are not `Unique` and are never removed, so at HEAD each level clear or restart appends three more copies of the same instances to `allActors` (they only stay invisible because `ShaderManagerImpl` de-duplicates by `shaderState`). The engine lane may start ignoring additions of actors already present (plan 03); the fix below must not depend on either behaviour, so it stops re-adding persistent actors instead.

## Fix

In `Brick.kt`: add `var isDestroyed = false` (internal class, plain property).

In `GameplayManager.kt`:

1. Drop `Unique` and `Group` from the class declaration and delete the `actors` lazy property. Nothing else in the module reads `GameplayManager` from the actor list (check with `grep -rn "GameplayManager" examples/game-wallbreaker`; `Ball` and `UIManager` resolve it as a Manager).
2. Delete the `scoreManager.score.onEach { ... }.launchIn(scope)` collector and the `scoreManager` delegate if nothing else in the file uses it (`restartGame` still calls `scoreManager.resetScore()`, so keep it).
3. Add `private var destroyedBrickCount = 0`.
4. `onInitialize` adds the persistent actors once, then starts the level:

   ```kotlin
   override fun onInitialize(kubriko: Kubriko) {
       actorManager.add(
           paddle,
           SmoothPixelationShader(),
           VignetteShader(),
           ChromaticAberrationShader(),
           uiManager,
       )
       startLevel()
   }
   ```

5. Rename `initializeScene()` to `startLevel()` and make it add only the per-level actors:

   ```kotlin
   private fun startLevel() {
       _isGameOver.value = false
       destroyedBrickCount = 0
       bricks.forEach {
           it.isDestroyed = false
           it.randomizeHue()
       }
       actorManager.add(bricks + Ball(paddle))
   }
   ```

6. Add the entry point the ball calls, on the tick thread:

   ```kotlin
   fun onBrickDestroyed() {
       destroyedBrickCount++
       if (destroyedBrickCount == bricks.size) onLevelCleared()
   }
   ```

7. `onLevelCleared()` removes only the ball (the paddle stays in the scene), then starts the next level:

   ```kotlin
   private fun onLevelCleared() {
       audioManager.playLevelClearedSoundEffect()
       actorManager.remove(actorManager.allActors.value.filterIsInstance<Ball>())
       startLevel()
   }
   ```

8. `restartGame()` removes the bricks and ball but not the paddle, then starts the level:

   ```kotlin
   actorManager.remove(bricks + actorManager.allActors.value.filterIsInstance<Ball>())
   startLevel()
   scoreManager.resetScore()
   resumeGame()
   ```

   Removing a brick that was already destroyed is a no-op. Removing and re-adding the same brick instances is correct both at HEAD and under the engine lane's planned batch rules (they end up present either way; `Brick` has no lifecycle callbacks).

9. Remove the now-unused imports (`Group`, `Unique`, `launchIn`, `onEach`; keep `MutableStateFlow`/`asStateFlow`).

In `Ball.kt`, `onCollisionDetected`:

- Ignore bricks that are already destroyed and bail out when nothing is left to hit:

  ```kotlin
  val collidable = collidables.filterIsInstance<Paddle>().firstOrNull()
      ?: collidables.filterIsInstance<Brick>().filterNot { it.isDestroyed }.minByOrNull { it.body.position.distanceTo(body.position) }
      ?: return
  ```

  and turn the existing `.let { collidable -> ... }` body into straight-line code using this `collidable` (keep the trailing sound-effect block inside the `if (state == State.LAUNCHED)`; an early `return` before anything was hit is fine because both sound flags are still false).
- In the `if (collidable is Brick)` branch, set `collidable.isDestroyed = true` before `actorManager.remove(collidable)`, and call `gameManager.onBrickDestroyed()` right after `scoreManager.incrementScore()`.

`onCollisionDetected` runs on the tick thread, and `restartGame`/`resumeGame` run on the main thread, which is the tick thread for the default `TickSource.viewportFrames()` on every platform — so `destroyedBrickCount` and `isDestroyed` need no synchronisation.

`examples/game-wallbreaker/CLAUDE.md`:
- Line ~19 (shaders bullet): "added to the actor list via `GameplayManager.actors`" → "added once by `GameplayManager.onInitialize`".
- Line ~27 (`Brick`): mention `isDestroyed`, set by the ball on the hit so a brick awaiting removal cannot be hit twice.
- Line ~39: `GameplayManager` no longer implements `Unique`/`Group`; it adds the paddle, shaders and `UIManager` once and re-adds only the bricks and a new `Ball` per level.
- Line ~41: level completion is counted on the tick thread — `Ball` calls `GameplayManager.onBrickDestroyed()`, which starts the next level when every brick of the grid has been destroyed.
- Line ~53: `restartGame` removes the bricks and the ball (the paddle, shaders and `UIManager` stay).

## Tests

None: the examples have no test source sets, and the logic lives in actors and Managers wired to a running `Kubriko`. The fix removes the cross-thread read instead of narrowing it, so there is nothing timing-dependent left to test.

## Manual check

Showcase → Wallbreaker, on Desktop (JVM) and Android:
1. Clear a full level several times in a row (temporarily shrinking `bricks` to `(-1..0)` × `(-1..0)` in a scratch build makes this quick; do not commit it). Every time the last brick pops, the level-cleared sound plays and a fresh grid appears with the ball back on the paddle.
2. Lose, press Restart (and press Space on the game-over menu): no level-cleared sound, the grid appears once, the score resets to 0.
3. With the debug menu enabled, open the actor inspector after two level clears and a restart: the actor count is the same as after the first level start (bricks + ball + paddle + three shaders + `UIManager` + transient `BrickPopEffect`s), not growing by three per level.
