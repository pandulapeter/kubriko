<!--
  This file is part of Kubriko.
  Copyright (c) Pandula Péter 2025-2026.
  https://github.com/pandulapeter/kubriko

  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one at
  https://mozilla.org/MPL/2.0/.
-->
# game-space-squadron

A vertical-scrolling space shooter where the player controls a ship to dodge and shoot down alien ships, collecting power-ups to unlock spread-fire and shield pickups to restore health.

## Plugins used

- **collision** (`CollisionManager`): ship vs. aliens, bullets vs. ships/aliens, collectables vs. ship — all with `CircleCollisionMask` or `BoxCollisionMask`.
- **keyboard-input** (`KeyboardInputManager`): arrow keys move `ShipDestination`; Spacebar fires.
- **pointer-input** (`PointerInputManager`, `isActiveAboveViewport = true`): relative mouse movement drives `ShipDestination`; any pointer press fires bullets. Uses `tryToMoveHoveringPointer` to re-center the cursor after each movement event so the ship doesn't get stuck at an edge.
- **particles** (`ParticleManager`): `BulletParticleState` trails behind every bullet (continuous emission); `ExplosionParticleState` bursts 100 particles on each explosion.
- **shaders** (`ShaderManager`): `GalaxyShader` (custom SKSL, credits Birdmachine on Shadertoy) renders the animated starfield background in a separate Kubriko instance.
- **sprites** (`SpriteManager`): `AnimatedSprite` for `Ship` (32 frames), `AlienShip` (91 frames), `PowerUp`, and `Shield` sprite sheets.
- **audio-playback** (`MusicManager`, `SoundManager`): background music plus per-event SFX (shoot, explosion, collect, button hover/click).
- **persistence** (`PersistenceManager`): high score persisted under `"kubrikoSpaceSquadron"`.

## Key Actor types

- **`Ship`** (`Visible`, `Dynamic`, `Group`, `KeyboardInputAware`, `PointerInputAware`, `Collidable`, `Unique`): player ship. Uses `ShipAnimationWrapper` (inner class) to animate the sprite based on horizontal movement direction — the sprite steps forward when banking and steps back when straightening up. Groups with `ShipDestination`.
- **`ShipDestination`** (`Positionable`, `PointerInputAware`, `KeyboardInputAware`, `Dynamic`): invisible point actor that is the target the ship smoothly interpolates towards. Decouples input from physics so movement feels analog regardless of input type.
- **`AlienShip`** (`Visible`, `Dynamic`, `Collidable`, `CollisionDetector`): scrolls downward at a speed scaled by viewport height. Fires at the player ship with random timing. Spawns `PowerUp` or `Shield` with low probability on death. Resets to a random X position above the viewport when it exits the bottom.
- **`Bullet` / `BulletPlayer` / `BulletEnemy`**: abstract `Bullet` base (`Visible`, `Dynamic`, `ParticleEmitter`, `CollisionDetector`) with direction/speed params. Player bullets travel upward; enemy bullets aim at the current ship position using `directionTowards`.
- **`Explosion`** (`ParticleEmitter<ExplosionParticleState>`): burst emitter that removes itself once emission is complete. Also increments score.
- **`PowerUp` / `Shield`**: both extend `Collectable` (base class for animated collectables that drift downward). `PowerUp` grants 6 additional spread shots; `Shield` restores 2 health points.
- **`CameraShakeEffect`** (`Dynamic`): short-lived actor that randomises `ViewportManager.scaleFactor` and `CameraShakeManager.rotation` on every frame for a screen-shake duration, then removes itself.
- **`GalaxyShader`** (`Shader`, `Dynamic`, `Unique`): time-driven SKSL shader rendering the scrolling galaxy. Runs in a separate `backgroundKubriko` instance.

## Game state management

Two `Kubriko` instances run side-by-side:
- `backgroundKubriko`: contains the `GalaxyShader` background and shared audio/sprite managers. Controlled by a dedicated `backgroundStateManager` so the background can keep scrolling during the pause menu.
- `kubriko` (foreground): contains all gameplay actors, collision, input, particles.

`GameplayManager` exposes `isGameOver: StateFlow<Boolean>`. When `true`, the menu overlay becomes visible. `speedMultiplier` and `scaleMultiplier` are derived from viewport size via `map`, so the game scales to all screen sizes without per-frame allocation.

`ScoreManager` persists high score: an `onEach` coroutine in `onInitialize` auto-saves whenever the score exceeds the stored high score.

`CameraShakeManager` holds a `rotation: StateFlow<Float>` that the `SpaceSquadronGame` composable applies as a `Modifier.rotate(...)` on the background viewport — the shake is a Compose-layer effect, not a camera translate.

## Input handling

- **Mouse/touch**: `ShipDestination.onPointerOffsetChanged` uses relative delta (current minus previous position), multiplied by `POINTER_SENSITIVITY = 2f`, to update the destination. Every other event is skipped using the `shouldMoveShip` toggle to absorb the synthetic re-centering move that follows.
- **Keyboard**: `ShipDestination.handleActiveKeys` uses `KeyboardDirectionState` (from the keyboard-input plugin extension) and normalizes diagonal movement to prevent faster diagonal speed. `Ship.handleActiveKeys` fires on Spacebar.
- Shooting is rate-limited to 250 ms via `metadataManager.activeRuntimeInMilliseconds` comparison.

## Non-obvious implementation choices

- `ActorManager` is created with `shouldUpdateActorsWhileNotRunning = true` and `shouldPutFarAwayActorsToSleep = false` so that actors continue scaling correctly during resize events while the game is paused, and so off-screen alien ships do not sleep and miss their reset logic.
- All per-frame scaling of actors is driven by `gameplayManager.scaleMultiplier.value` (derived from `(height + width) / 3000f`) so the game looks correct on both portrait phones and landscape monitors.
- `LoadingManager` exposes a `@Composable fun isGameLoaded()` that tracks fonts, icons, images, strings, music, and sprites together; the game composable gates the foreground viewport behind this flag to prevent a flash of unstyled content.
- `PointerInputManager` is constructed with `isActiveAboveViewport = true` so pointer input is captured even when overlapping Compose UI (e.g. score display).
