<!--
  This file is part of Kubriko.
  Copyright (c) Pandula Péter 2025-2026.
  https://github.com/pandulapeter/kubriko

  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one at
  https://mozilla.org/MPL/2.0/.
-->
# game-wallbreaker

A classic Breakout-style game where the player controls a paddle to keep a ball in play and destroy all bricks; the ball speed increases with score, and the level resets with new random brick colors when all bricks are cleared.

## Plugins used

- **collision** (`CollisionManager`): `Ball` (as `CollisionDetector`) listens for collisions with `Brick` and `Paddle` (both `Collidable`), using `CircleCollisionMask` for the ball and `BoxCollisionMask` for bricks and paddle.
- **keyboard-input** (`KeyboardInputManager`): left/right arrow keys move the paddle; Spacebar launches the ball from the positioning state.
- **pointer-input** (`PointerInputManager`, `isActiveAboveViewport = true`): relative mouse movement controls paddle X position. Uses `tryToMoveHoveringPointer` to re-center the cursor after each delta so the paddle does not get clamped at the viewport edge.
- **shaders** (`ShaderManager`): three built-in shaders (`SmoothPixelationShader`, `VignetteShader`, `ChromaticAberrationShader`) are added to the actor list via `GameplayManager.actors`. Background uses a custom `FogShader` (SKSL, credits deusnovus on Shadertoy) in a separate `backgroundKubriko`.
- **audio-playback** (`MusicManager`, `SoundManager`): background music plus SFX for paddle hit, brick pop, edge bounce, level cleared, game over.
- **persistence** (`PersistenceManager`): high score persisted under `"kubrikoWallbreaker"`.

## Key Actor types

- **`Paddle`** (`Visible`, `Collidable`, `PointerInputAware`, `KeyboardInputAware`, `Dynamic`, `Unique`): horizontal-only movement. Pointer input uses relative deltas multiplied by `POINTER_SPEED_MULTIPLIER = 1.5f` and the same cursor-recentering trick as Space Squadron.
- **`Ball`** (`Visible`, `Dynamic`, `CollisionDetector`, `PointerInputAware`, `KeyboardInputAware`, `Unique`): four-state machine: `UNINITIALIZED → POSITIONING → LAUNCHED → GAME_OVER`. In `POSITIONING` the ball follows the paddle's X. On launch, direction is `(baseSpeedX=1, baseSpeedY=-1)`. Speed = `InitialSpeed(0.6) + ScoreIncrement(0.005) * score`, capped at `MaximumSpeed(1.8)`. When hitting the bottom edge, transitions to `GAME_OVER` and calls `GameplayManager.onGameOver()`.
- **`Brick`** (`Visible`, `Collidable`): static 100×40 scene-unit rectangle. Has a random HSV hue; `randomizeHue()` is called at the start of each level.
- **`BrickPopEffect`** (`Visible`, `Dynamic`): short-lived actor that scales down from the destroyed brick's position while fading out. Also applies a brief `ViewportManager.setScaleFactor` wobble per frame as a screen-shake substitute — no separate shake manager.
- **`FogShader`** (`Shader`, `Dynamic`, `Unique`): time-driven SKSL fog shader running in `backgroundKubriko`.

## Game state management

Two `Kubriko` instances run side-by-side:
- `backgroundKubriko`: contains `FogShader` and shared audio managers; has no `StateManager` so it always runs.
- `kubriko` (foreground): `StateManager` is created with `shouldAutoStart = false`; the game starts paused and only runs while `isRunning`.

`ViewportManager` uses `AspectRatioMode.Fixed(ratio = 1f, width = 1200.sceneUnit)` — the viewport is always square, keeping the brick grid layout consistent across all screen sizes.

`GameplayManager` itself implements `Manager`, `Unique`, and `Group` so it can own the pre-allocated `bricks` list and inject the shaders as `actors`. The brick grid (10 columns × 10 rows) is created once at construction time and reused across levels; only `randomizeHue()` is called per level to avoid reallocation.

Level completion is detected by a coroutine in `onInitialize` that observes `ScoreManager.score` and checks whether any `Brick` actors remain.

## Input handling

- **Mouse/touch**: `Paddle.onPointerOffsetChanged` uses relative delta with the cursor-recentering pattern (same as Space Squadron). The `shouldMovePaddle` boolean alternates every event to filter the synthetic cursor-center event.
- **Keyboard**: `Paddle.handleActiveKeys` uses `activeKeys.hasLeft` / `hasRight` extensions from the keyboard-input plugin; XOR prevents conflicting simultaneous presses. Movement is `Speed = 2.sceneUnit` per millisecond, applied in `update()`.
- **Ball launch**: pointer release (any pointer) or Spacebar press transitions `POSITIONING → LAUNCHED`.

## Non-obvious implementation choices

- `Ball.onCollisionDetected` resolves the bounce direction geometrically by comparing the ball's position to the `AxisAlignedBoundingBox` corners/edges of the collided object, rather than using angle reflection. This makes the logic deterministic and avoids floating-point drift.
- When `Ball` hits a `Paddle`, the `isCollidingWithPaddle` flag suppresses repeat sound effects for continuous paddle contact during a single bounce.
- `GameplayManager.restartGame` removes bricks, ball, and paddle separately from `allActors` rather than calling `removeAll()`, preserving the `GameplayManager` itself (which is also in the actor list as a `Group`).
- `LoadingManager` tracks both audio and a custom font (`kanit_regular`) before allowing the game to be shown, preventing unstyled text flicker.
- `PointerInputManager` is constructed with `isActiveAboveViewport = true` so the paddle responds to pointer events even when the score overlay is under the pointer.
