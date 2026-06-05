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

A fully editable isometric-projection scene with animated tiles, sprite textures, chain physics,
and a mini-map. It shows how to build isometric rendering on top of the engine's Cartesian
coordinate system using a dual-Kubriko-instance architecture.

## Entry point and managers/plugins

`IsometricGraphicsDemo` (`@Composable`) renders two overlapping `KubrikoViewport`s from two
separate `Kubriko` instances held in `IsometricGraphicsDemoStateHolderImpl`:

**Map Kubriko** (`LOG_TAG = "IsometricGraphicsMap"`) — owns the top-down Cartesian layout of tile
actors (`CubeTile`, `CharacterTile`, `AnimalTile`). Uses `SerializationManager` (from
`scene-editor-api`) to load/save a JSON scene from `files/scenes/scene_isometric_graphics_demo.json`.

**World Kubriko** (`LOG_TAG = "IsometricGraphics"`) — renders the projected isometric
representations. Managers: `GridManager`, `ViewportManager`, `SpriteManager`, `PointerInputManager`,
`IsometricGraphicsDemoManager`. The map viewport is rendered at 128×128 dp in the top-left corner,
scaled ×1.5 and rotated −45° via Compose modifiers so it looks like a bird's-eye map.

## Key actor types

**`IsometricRepresentation`** (abstract `Visible`) — base for the 3-D projected actors. Stores
6-dimensional state (positionX/Y/Z, dimensionX/Y/Z) and recomputes `BoxBody` position, size, and
pivot on every `update()` call using the isometric projection formula:
`screenX = (posX - posY) * tileW * 0.5`, `screenY = -(posX + posY) * tileH * 0.5`.
`drawingOrder = posX + posY - 0.1 * posZ` provides correct painter's-algorithm depth sorting.

**`VisibleInWorld`** (abstract `Visible`, `Dynamic`) — bridges a Cartesian tile Actor in the map
Kubriko to its `IsometricRepresentation` in the world Kubriko. `onAdded` registers the
representation into `isometricWorldActorManager`; `update()` propagates the tile's body state.

**`CubeTile`** — implements `VisibleInWorld` + `Editable`. Supports bounce (sinusoidal Z offset)
and rotation animations controlled by flags on `IsometricGraphicsDemoManager`. Sprite textures
for the top and side faces are drawn via `Cube.drawImageInParallelogram()` using an affine matrix
transform to warp the bitmap onto the rhombus face shape.

**`GridManager`** — draws the infinite isometric grid as a Compose `Canvas` overlay inside
its `Composable()` override. Camera drag and pinch-zoom both flow through
`IsometricGraphicsDemoManager.onPointerDrag/onPointerZoom` into `isometricWorldViewportManager`.

## Non-obvious implementation patterns

**Two-Kubriko architecture.** The map Kubriko holds serializable, Editable tile state; the world
Kubriko holds the rendered representation actors. `IsometricGraphicsDemoManager` is registered in
*both* instances (shared Manager) so tiles can reach `isometricWorldActorManager` at init time.

**Projection math.** The `IsometricRepresentation.update()` call negates Y (`positionY = -posY`)
and applies a `magicEffect` correction factor `(tileH / tileW) * 1.6` to the Z axis so objects
at different heights project correctly regardless of the tile aspect ratio.

**Scene editor integration.** Desktop builds expose `IsometricGraphicsDemoSceneEditor` which
launches the tool-scene-editor with the map Kubriko's serialization manager.

## Platform-specific considerations

- `PlatformSpecificContent` (per-platform `expect`/`actual`) exposes the Scene Editor launch button
  on Desktop; it is a no-op on Android, iOS, and Web.
- The demo includes `desktopMain`, `androidMain`, `iosMain`, and `webMain` source sets for the
  `PlatformSpecificContent` expect function.
