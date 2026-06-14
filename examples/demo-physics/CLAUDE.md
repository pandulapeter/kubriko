<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# demo-physics CLAUDE.md

## What this demo demonstrates

Interactive showcase of the `plugin-physics` module: rigid-body simulation with boxes, circles,
and polygons, a jointed chain made of linked rigid bodies, and point explosions using
`ProximityExplosion`. Tap/click to spawn objects or trigger blasts on top of a pre-built static
environment loaded from a scene file.

## Entry point and managers/plugins

`PhysicsDemo` (`@Composable`) is the entry point. `PhysicsDemoStateHolderImpl` creates:
- `ViewportManager` — `AspectRatioMode.FitVertical(1920 su)` keeps the scene height fixed
- `PhysicsManager` — the physics simulation engine
- `PointerInputManager` — routes tap events to `PhysicsDemoManager`
- `PhysicsDemoManager` — loads scene JSON, handles input, owns UI composable
- `SerializationManager` — deserialises static and dynamic actor states from
  `files/scenes/scene_physics_test.json`

## Key actor types

**`BaseDynamicObject`** (abstract, `RigidBody`, `Visible`, `Dynamic`) — shared base for dynamic
actors. `update()` copies `physicsBody.position/rotation` back into `body`, then removes the
actor if its AABB leaves the viewport bounds. Collision mask position is kept in sync here.

**`DynamicBox` / `DynamicCircle` / `DynamicPolygon`** — concrete dynamic actors with random
pastel HSV colours. Each wraps an appropriate `PhysicsBody` and collision mask
(`BoxCollisionMask`, `CircleCollisionMask`, `PolygonCollisionMask`). Dynamic objects are spawned
via `onPointerReleased` at the screen tap position converted to scene coordinates.

**`StaticBox` / `StaticCircle` / `StaticPolygon`** — immovable environment pieces loaded from the
scene JSON. They implement `Editable` / `Serializable` so the scene can be rebuilt in the editor.

**`DynamicChain`** (`Group`, `Dynamic`, `Visible`, `Editable`) — creates `linkCount` `ChainLink`
rigid bodies (circular) and `linkCount - 1` `JointToBody` spring joints connecting adjacent links.
The chain's `BoxBody` is kept up to date each frame in `refreshBodySize()` by computing the AABB of
all link positions. It reads `ChainLink.physicsBody.position` (the authoritative physics state),
**not** `ChainLink.body.position`: as a `Group` parent this actor updates before its child links in
the same tick (BFS flatten order in `ActorManagerImpl`), so the links' render bodies still hold the
previous tick's position when `refreshBodySize()` runs. Reading them made the clip/cull bounds lag
the drawn path by one tick — invisible at 60 FPS, but enough to clip the chain visibly at low/throttled
frame rates. `physicsBody.position` is the same value the links copy into their render bodies this
tick, so bounds and path stay in sync.
The chain draws a smooth quadratic-Bezier path through all link centres using two `drawPath` calls
(thick black outline then thinner coloured stroke). Uses `Group` so all link and joint Actors are
added/removed atomically with the chain.

**`Bomb`** (`Visible`, `Dynamic`) — spawned on explosion taps. On `onAdded` it immediately creates
a `ProximityExplosion` and calls `update()` on all current `RigidBody` physics bodies within
750 su. Each subsequent frame it grows, fades, and repeatedly calls `applyBlastImpulse(25 000 000
su)` until alpha ≤ 0, then removes itself.

## Non-obvious implementation patterns

**`PhysicsDemoManager` is both Manager and Actor.** It implements `PointerInputAware` and `Unique`,
adding itself to `ActorManager` during `onInitialize` so pointer events are routed through the
plugin. A cycling `ActionType` enum (`SHAPE` → `CHAIN` → `EXPLOSION`) controls what `onPointerReleased`
spawns; the FAB icon updates accordingly.

**Off-screen removal.** `BaseDynamicObject.update()` calls the `isWithinViewportBounds` extension on
the actor's AABB, removing the actor when it leaves the viewport. This keeps the actor pool bounded
without a manual culling pass.

**`DynamicPolygon` does not use `Editable`.** Unlike boxes and circles, polygons are constructed
entirely at runtime with random vertex counts (3–10 sides); they are not persisted in the scene
file, so no `State`/`serialize()` boilerplate is needed.

## Platform-specific considerations

- `PlatformSpecificContent` (expect/actual) exposes the Scene Editor button on Desktop only.
- Source sets: `desktopMain`, `androidMain`, `iosMain`, `webMain`.
- `PhysicsDemoSceneEditor` (Desktop only) launches the tool-scene-editor for the physics scene.
