<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# test-collision

Test example that exercises the `collision` plugin and the `pointer-input` plugin together.
Only enabled in the Showcase app when `showcase.areTestExamplesEnabled=true` in `gradle.properties`.

## What it tests

- All three collision mask types: `BoxCollisionMask` (rotatable), `PolygonCollisionMask`, `CircleCollisionMask`
- `CollisionDetector` receiving `onCollisionDetected` callbacks with a list of `Collidable` actors
- `PointerInputAware` drag interaction: pointer press, release, and position tracking in scene coordinates
- Viewport aspect ratio mode `FitVertical` — the scene always fits its full height on screen
- 65 randomly-placed, randomly-shaped actors running simultaneously (stress test for collision performance)
- `RayEmitter` actor (currently commented out in `CollisionTestManager`) — a draggable actor that casts
  32 rays from its centre; can be re-enabled to visualise ray-vs-mask intersection

## Module structure

```
CollisionTest.kt                        — public Composable + createCollisionTestStateHolder() factory
implementation/
  CollisionTestStateHolder.kt           — sealed interface + Impl; creates CollisionManager,
                                          PointerInputManager, ViewportManager, CollisionTestManager
  managers/
    CollisionTestManager.kt             — spawns 65 DraggableCollidableActors on init
  actors/
    DraggableActor.kt                   — abstract base: Visible + CollisionDetector + PointerInputAware + Dynamic;
                                          handles drag logic and per-frame rotation for polygon masks
    DraggableCollidableActor.kt         — concrete collidable; newRandomShape() picks box/polygon/circle randomly
    RayEmitter.kt                       — draggable actor that draws 32 outward rays; currently unused
```

## Key patterns

- `DraggableActor` stores drag state as plain fields, not flows, to avoid per-frame allocation.
- On press, hit-testing is done via `sceneOffset.isCollidingWith(collisionMask)` before starting a drag.
- `drawingOrder` is decremented on pick-up so the dragged actor renders on top.
- Collisions are accumulated in `onCollisionDetected` and cleared at the start of each `update()` tick;
  the fill colour changes to `Color.DarkGray` when colliding.
- Polygon actors auto-rotate each frame (unless being dragged); the rotation is applied to both `body.rotation`
  and `collisionMask.rotation` in sync.
- `collidableTypes = listOf(DraggableCollidableActor::class)` means collision detection is restricted to
  `DraggableCollidableActor` instances only; `RayEmitter` does not register as a collidable target.
- `AREA_LIMIT = 512` (scene units) is used by both `CollisionTestManager` for random placement and
  by `CollisionTestStateHolder` as the `FitVertical` height so the two stay in sync.
