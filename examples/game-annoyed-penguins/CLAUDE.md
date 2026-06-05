<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# game-annoyed-penguins

Angry-Birds-style physics launcher. The player pulls back a slingshot to launch penguins at destructible block structures. Three levels loaded from JSON scene files. Win condition: collect all stars in the level (stars are actors placed in the scene).

## Plugins used

- `physics` — `PhysicsManager` drives all rigid-body simulation. `simulationSpeed` starts at `0f` and is set to `1f` on the first penguin launch so the scene is frozen before firing.
- `collision` — `CollisionManager` detects hits between penguin, blocks, and ground.
- `audio-playback` — `MusicManager` + `SoundManager` for music and SFX.
- `pointer-input` — `PointerInputManager` (`isActiveAboveViewport = true`) for slingshot drag and camera pan/zoom.
- `keyboard-input` — `KeyboardInputManager` (available but minimal use in this game).
- `shaders` — Two `ShaderManager` instances: one for the background `FogShader` (Perlin-noise animated fog, credits: deusnovus/Shadertoy), one for the foreground `GradualBlurShader` applied when the game is paused.
- `sprites` — `SpriteManager` for all sprite images.
- `persistence` — `PersistenceManager` (file `kubrikoAnnoyedPenguins`) for user preferences.
- `serialization` — Levels are JSON scene files (`level_1.json` – `level_3.json`) deserialized via `EditableMetadata`/`SerializationManager`. All editable actors (`DestructibleBlock`, `Ground`, `Slingshot`, `Star`) implement `Editable` + `Serializable`.

## Architecture: two Kubriko instances

`backgroundKubriko` runs the animated `FogShader` independently so the background animates even while the main game is paused. `sharedMusicManager`, `sharedSoundManager`, and `sharedSpriteManager` are created once and registered into both instances so resources are shared.

`ViewportManager` uses `FitVertical(1440.sceneUnit)`, minimum scale `0.25f`. `shouldPutFarAwayActorsToSleep = false` on the main `ActorManager` because the play area is small enough that all actors must stay active.

## Key actors

| Actor | Traits | Notes |
|---|---|---|
| `Slingshot` | `Visible`, `Dynamic`, `PointerInputAware`, `RigidBody`, `Unique`, `Editable` | Owns the aiming and launch logic; `density = 0f` (static body). Handles camera pan on free drag, pinch zoom via `onPointerZoom`. Animates initial zoom-out on level load. |
| `ActiveFakePenguin` | `BlinkingPenguin`, `Dynamic`, `Unique` | Visual placeholder on the slingshot during aiming. When `isVisible` is set to `false`, a real physics `Penguin` is spawned with the computed impulse. |
| `WaitingFakePenguin` | `BlinkingPenguin`, `Unique` | Static visual of the "next" penguin waiting beside the slingshot. |
| `Penguin` | `BlinkingPenguin`, `RigidBody`, `CollisionDetector` | The physics projectile. On first `update()` it applies the launch impulse and sets `simulationSpeed = 1f`. `shouldBeFollowedByCamera` causes the camera to track it; resets to false after 2 s of no collision. |
| `DestructiblePhysicsObject` / `DestructibleBlock` | `Visible`, `Dynamic`, `RigidBody`, `CollisionDetector`, `Editable` | Physics rigid bodies that play a crash SFX on high-velocity impact. Removed when they fall off the bottom of the scene. |
| `Ground` | Static `RigidBody`, `Editable` | Zero-density physics body; acts as the floor. |
| `Star` | `Visible`, `Editable` | Non-physics collectible; picked up via collision mask overlap checked in `GameplayManager.onUpdate`. |

## Game state management

`GameplayManager` owns level loading and the win condition. Level JSON is read with `Res.readBytes("files/scenes/<name>")` and deserialized by `serializationManager.deserializeActors()`. When all stars are collected, a 600 ms fade-out timer triggers `currentLevel = null` which clears the scene.

`stateManager.shouldAutoStart = false`; gameplay is started explicitly. When paused, `GradualBlurShader` is added to the actor list. The shader is manually updated in `GameplayManager.onUpdate` when `isRunning` is false (because the engine's own update is suspended).

## Input handling

Mouse / touch: `Slingshot` consumes drag input when `aimingPointerId` is set (pointer in slingshot bounds). Free drag with no active aiming pans the camera via `viewportManager.addToCameraPosition`. Pinch zoom via `onPointerZoom`.

The `ActiveFakePenguin` position is clamped by a downward-angle penalty (`downwardFactor`) so the player cannot aim straight down. Maximum pull radius is `650.sceneUnit`.
