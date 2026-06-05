<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# game-blockys-journey

A top-down isometric-style world where "Blocky" (a character) walks autonomously through a scene. Marked as **unfinished** (a disclaimer is shown at runtime). The scene is loaded from a single JSON file (`world.json`).

## Plugins used

- `audio-playback` — `MusicManager` + `SoundManager` for background music and SFX.
- `pointer-input` — `PointerInputManager` (`isActiveAboveViewport = true`).
- `keyboard-input` — `KeyboardInputManager`.
- `shaders` — `ShaderManager` for the pause-screen `RippleShader` (built-in collection).
- `sprites` — `SpriteManager` for directional sprite sheets (8 directions × sprite sheet).
- `persistence` — `PersistenceManager` (file `kubrikoBlockysJourney`) for user preferences.
- `serialization` — Scene deserialized from `world.json` via `EditableMetadata`/`SerializationManager`. `Blocky` and `Block` implement `Editable` + `Serializable`.

## Architecture: two Kubriko instances

`backgroundKubriko` runs loading, serialization, and audio preloading independently of the main instance. The `SerializationManager` and `SpriteManager` are both shared between instances so the level can be loaded and prerendered before the main game starts.

`ViewportManager` uses `FitVertical(1440.sceneUnit)`.

`StateManager.shouldAutoStart = false` — the game requires explicit user action to start.

## Key actors

| Actor | Traits | Notes |
|---|---|---|
| `Blocky` | `Visible`, `Dynamic`, `Editable` | Moves autonomously: advances in its current `Direction` at constant speed, then a `Timer(650 ms)` rotates direction clockwise to the next of 8 compass headings. `drawingOrder = -body.position.y.raw` for isometric depth sorting. |
| `Block` | `Visible`, `Editable` | Static environment tile. Also depth-sorted with `drawingOrder = -body.position.y.raw`. |

## Isometric depth sorting

Both `Blocky` and `Block` use `drawingOrder = -body.position.y.raw`. In Kubriko, lower `drawingOrder` values are painted on top, so more-negative (further south/down in scene space) means drawn later and appearing in front, matching isometric convention.

## Blocky movement

`Blocky` has 8 `Direction` enum entries, each pairing a `DrawableResource` (sprite sheet for that facing) with an `AngleRadians` angle. Position advances each tick:
```
body.position += SceneOffset(+Speed * cos(angle), -Speed * sin(angle)) * deltaTime
```
The `turningTimer` fires every 650 ms and shifts to `nextDirectionClockwise`. No player control of Blocky's movement exists yet (unfinished game).

`AnimatedSprite` drives per-direction animation with `frameSize = 256×256`, 40 frames per sheet, 8 frames per row at 60 fps.

## Loading

`LoadingManager` coordinates all resource loading and waits until fonts, strings, icons, images, audio, sprites, and the scene JSON are all ready before `isLoadingDone` is set. The font (`medieval_sharp`) is loaded via `preloadedFont` inside `LoadingManager.Composable()` because font loading requires a composition context.

## Pause visual

When `stateManager.isRunning` is `false`, a `RippleShader` is added to the actor list. The shader state is advanced manually in `GameplayManager.onUpdate` while not running (the engine suspends updates when `isRunning` is false).
