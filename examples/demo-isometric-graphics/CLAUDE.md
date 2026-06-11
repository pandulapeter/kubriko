<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# demo-isometric-graphics CLAUDE.md

## What this demo demonstrates

A real-time isometric 3D world composed entirely of **cuboids** (boxes with per-face color or
texture), rendered on top of the engine's Cartesian coordinate system using a dual-`Kubriko`-instance
architecture. This module is a flattened port of the standalone **Tesselar** game — the model/region
editors are dropped and all of its modules (renderer, gameplay, UI) are collapsed into this one
module under `implementation/`.

## Entry point and state

`IsometricGraphicsDemo` (`@Composable`, top-level package) renders `IsometricGraphicsContent` plus
the standard Showcase `InfoPanel`. All state lives in `IsometricGraphicsDemoStateHolderImpl`
(`implementation/`), which owns **two** `Kubriko` instances and is created/disposed like every other
demo's state holder:

- **`logicKubriko`** (`LOG_TAG_LOGIC = "IsometricGraphicsLogic"`) — drives game logic in plain
  Cartesian space: the `ActorManager` (with `invisibleActorMinimumRefreshTimeInMillis = 500`),
  `logicViewportManager` (scale `0.04`), `ControlManager`, `LogicManager`, `TextureResolver`, and a
  `SpriteManager`.
- **`isometricKubriko`** (`LOG_TAG = "IsometricGraphics"`) — pure rendering: `volumetricViewportManager`,
  `VolumetricRenderManager`, `ControlOverlayManager`, `KeyboardInputManager`, `PointerInputManager`.

`StateHolder.kubriko` exposes `isometricKubriko` (the on-screen viewport) for the debug menu;
`dispose()` tears down both instances.

## The bridge between the two instances

`RenderableCuboidHolder` (`implementation/renderer/data/actor/`) is the key interface. Any actor in
`logicKubriko` that implements it (`MainCharacter`, `Character`, `Tree` — all extend
`PlanarCuboidModelRenderer`) automatically gets a `VolumetricCuboidRenderer` created in
`isometricKubriko` when it enters the logic viewport. `VolumetricRenderManager` subscribes to
`logicActorManager.visibleActorsWithinViewport` and to `ControlManager.cameraOffset`, diffing the
holder set off the main thread to add/remove renderers.

## State-holder wiring (vs. the original Tesselar globals)

Tesselar declared its `Kubriko` instances and managers as module-level singletons and a
`object ControlManager`. Here they are instance members of the state holder instead:

- `ControlManager` is a normal `Manager` **class**, registered in `logicKubriko`.
- `ControlOverlayManager` (in `isometricKubriko`) takes `controlManager` and `logicViewportManager`
  via its constructor, and resolves `VolumetricRenderManager` with the `manager<T>()` delegate.
- `MainCharacter` resolves its `ControlManager` via `kubriko.get()` in `onAdded`.
- The composables `IsometricGraphicsContent` and `MiniMap` take the state holder as a parameter
  rather than reading globals.

## Key types

- `implementation/renderer/data` — serializable model (`Cuboid`, `CuboidModel`, animations, `Vec3`,
  `RenderableCuboid(Model)`). `@Serializable` is used only to **load** the JSON models at runtime.
- `implementation/renderer/planar` — top-down/flat projection used by the minimap and as the base
  class for the logic actors; includes the `TriangleBatch` mesh helper (with an `expect`/`actual`
  `drawTriangles` per platform) and the grid-line caches.
- `implementation/renderer/volumetric` — isometric 3D rendering (`VolumetricRenderManager`,
  `VolumetricCuboidRenderer`, batch renderer, mip chains).
- `implementation/logic` — `ControlManager`, `LogicManager` (loads `character.json` + `tree.json`,
  scatters trees), and the actors.
- `implementation/gameplay/resources` — `TextureResolver` (sprite-backed) and `FileResolver`.
- `implementation/ui` — `IsometricGraphicsContent` (viewport + joystick + minimap), `MiniMap`,
  `ControlOverlayManager`.

## Resources

`commonMain/composeResources/`:
- `drawable/` — `texture_01.webp`, `map_01.webp`.
- `files/model/` — `character.json`, `tree.json` (read directly by `LogicManager` at runtime).
- `values/strings.xml` — only the `description` shown in the `InfoPanel`.

## Platform notes

- `TriangleBatch` has an `expect fun drawTriangles` in `commonMain` with an Android actual
  (`android.graphics`) and a shared Skiko actual in `desktopMain`, `iosMain`, and `webMain`.
- Multi-touch (pinch-zoom) only works on Android and iOS; desktop and web use mouse drag + wheel.
