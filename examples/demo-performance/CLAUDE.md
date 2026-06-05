<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# demo-performance CLAUDE.md

## What this demo demonstrates

A stress-test scene with hundreds of simultaneously active actors — rotating, scaling, and drifting
boxes — to benchmark the engine's update-loop throughput and frustum-culling sleep mechanism. A
mini-map overlay visualises which actors are in-viewport, active-but-offscreen, or fully sleeping.

## Entry point and managers/plugins

`PerformanceDemo` (`@Composable`) is the entry point. `PerformanceDemoStateHolderImpl` creates:
- `ActorManager` with `invisibleActorMinimumRefreshTimeInMillis = 500` — off-screen actors update at
  most every 500 ms instead of every frame, reducing CPU load for the large off-screen population
- `ViewportManager` — `initialScaleFactor = 0.5`, `viewportEdgeBuffer = 400 su`
- `PerformanceDemoManager` — loads the scene JSON and owns the UI composable
- `SerializationManager` (via `EditableMetadata.newSerializationManagerInstance`) — deserialises
  `BoxWithCircle`, `MovingBox`, and `Camera` actors from
  `files/scenes/scene_performance_demo.json`

## Key actor types

**`MovingBox`** (`Visible`, `Dynamic`, `Editable`) — rotates (direction randomised per actor) and
pulses scale between 0.5× and 1.6× while drifting along its rotation heading. Position update:
`position += SceneOffset(cos(rotation), -sin(rotation)) * delta * 0.2`. Implements `Serializable`
so the scene can be saved/loaded from the Scene Editor.

**`BoxWithCircle`** (`Visible`, `Dynamic`, `Editable`) — simpler variant: constant rotation only,
no position drift. Draws a rectangle and an inscribed circle.

**`Camera`** (`Unique`, `Dynamic`, `Positionable`, `Editable`) — the single camera actor. It moves
the viewport in a circular orbit of radius 2500 su around the origin, computed as
`pos = SceneOffset(cos(acc/5000), sin(acc/5000)) * PATH_RADIUS`, then calls
`viewportManager.setCameraPosition(pos)` each frame. Using an Actor for camera movement rather than
a Manager lets it participate in the editor and be serialised.

## Non-obvious implementation patterns

**`invisibleActorMinimumRefreshTimeInMillis = 500`.** This is the key performance parameter.
Without it, all actors in a large scene update every frame even when far off-screen. Setting it to
500 ms means the engine skips `Dynamic.update()` calls for sleeping actors most frames, roughly
halving CPU work when less than half the scene is on-screen. The `viewportEdgeBuffer = 400 su`
widens the "visible" zone so actors don't pop in abruptly.

**Mini-map rendering without flow subscriptions.** `MiniMap` is a `Canvas` composable invalidated
by a `gameTime` parameter (from `MetadataManager.totalRuntimeInMilliseconds` filtered to every
other millisecond). It reads actor lists via lambda callbacks (`getAllVisibleActors`,
`getAllVisibleActorsWithinViewport`, `getAllActiveDynamicActors`) rather than collecting StateFlows,
which avoids re-composing the mini-map's parent on every frame.

**Scene Editor integration on Desktop.** `PerformanceDemoSceneEditor` (Desktop only) launches the
tool-scene-editor with the same `SerializationManager`, letting developers redesign the stress-test
layout and observe the performance impact immediately.

## Platform-specific considerations

- `PlatformSpecificContent` (expect/actual) exposes the Scene Editor launch button on Desktop only.
- The demo includes `desktopMain`, `androidMain`, `iosMain`, and `webMain` source sets.
