<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-collision

Broad-phase AABB + narrow-phase SAT collision detection dispatched to `CollisionDetector` actors each tick.

## Key Files

- `src/commonMain/.../CollisionManagerImpl.kt` — detection loop; owns `collisionBuffer`
- `src/commonMain/.../CollisionDetector.kt` — actor trait receiving `onCollisionDetected`
- `src/commonMain/.../mask/CollisionMask.kt` — sealed interface root
- `src/commonMain/.../mask/PolygonCollisionMask.kt` — convex hull; base for `BoxCollisionMask`
- `src/commonMain/.../extensions/CollisionMaskExtensions.kt` — all narrow-phase math
- `src/commonMain/.../implementation/RotationMatrix.kt` — mutable 2×2 matrix, no-alloc `transposeInto`

## Detection Loop (each tick)

1. Iterates only `CollisionDetector` instances (via `detectorsMirror`, an `ArrayList` mirror of the published list refilled on reference change — avoids per-frame persistent-list iterators)
2. For each detector, iterates `collidableTypes` list
3. Scans the type's pre-filtered candidate list (`collidablesByType`), skipping self. The per-type lists are built lazily via `KClass.isInstance` and invalidated when the collidable list changes — `isInstance` is reflective (slow on Wasm) and must not run per candidate per frame
4. Broad phase: AABB overlap check (skip if no overlap)
5. Narrow phase: type-dispatched algorithm via `hasCollisionWith` — the boolean-only twin of `collisionResultWith` that runs the same math but returns a pre-allocated sentinel instead of constructing a `CollisionResult` per colliding pair
6. Matching collidables collected into `collisionBuffer: MutableList<Collidable>` (module-level, reused)
7. `onCollisionDetected(collisionBuffer)` called only when at least one hit found

**Critical**: `collisionBuffer` is a **shared buffer cleared between iterations**. Never store a reference to it — copy contents if needed beyond the callback.

Detection is O(D × T × C′): D = detectors, T = collidableTypes per detector, C′ = collidables matching the type. Keep `collidableTypes` lists narrow.

## Narrow-Phase Algorithms

| Pair | Algorithm |
|---|---|
| Circle–Circle | Distance vs. sum-of-radii |
| Circle–Polygon / Polygon–Circle | SAT; circle transposed into polygon object space |
| Polygon–Polygon | Full SAT + Sutherland–Hodgman clipping; uses pre-allocated static buffers (zero allocation) |
| Point or mismatched | Returns `null` (no collision) |

Pass `shouldSkipAxisAlignedBoundingBoxCheck = true` to `collisionResultWith()` only when AABB overlap is guaranteed.

## Kinematic Movement Response

`CollisionMaskExtensions.kt` also exposes two helpers for actors moved by writing their position directly (no physics plugin) that should be blocked by solid obstacles:

- `slidingMovement(desiredMovement, obstacles, maximumSlideIterations = 4)` — returns the portion of the intended movement that stays collision-free. It advances the receiver to the target, then pushes it back out of any overlap along the obstacle's `contactNormal` scaled by penetration (deepest overlap first, repeated up to `maximumSlideIterations` times). For convex obstacles the push-out cancels the component aimed into the obstacle and leaves the tangential one, so an angled approach glides around a round obstacle's edge while a head-on hit settles at the surface. Probes the receiver mask at candidate positions and restores it before returning (the caller commits the result); free movement builds no `CollisionResult` (nothing overlaps), so it stays allocation-free.
- `depenetrationFrom(obstacles)` — returns the offset that separates the receiver from any obstacle it already overlaps (sum of each `CollisionResult`'s `contactNormal × penetration`), for recovering from spawn-inside-scenery cases the sweep cannot prevent.

Both skip the receiver if it appears in `obstacles`.

## Mask Hierarchy

```
CollisionMask (sealed interface)
└── PointCollisionMask (open) — position + dirty-flag lazy AABB
    ├── CircleCollisionMask
    └── PolygonCollisionMask (open) — convex hull, centered on centroid
        └── BoxCollisionMask — 4-vertex convenience wrapper
```

`ComplexCollisionMask` adds `size: SceneSize` and `isSceneOffsetInside(SceneOffset)`.

`PolygonCollisionMask` silently convexifies input via Andrew's monotone chain algorithm. **Concave polygons are not supported** — shapes are convexified at construction time.

`BoxCollisionMask` is centered on `initialPosition` (vertices derived from half-size).

## RotationMatrix

Mutable 2×2 matrix stored as two `SceneOffset` rows. `transposeInto(dest)` writes result into an existing instance — no allocation. Polygon masks own both `rotationMatrix` and `transposedRotationMatrix`, updated in-place when `rotation` is set. Avoid mutating `rotation` every frame on non-rotating bodies.

## Collidable + CollisionDetector Contract

- `Collidable` — any hittable actor; just position + mask, no callback
- `CollisionDetector extends Collidable` — receives `onCollisionDetected(List<Collidable>)`; declare `collidableTypes: List<KClass<out Collidable>>`
- `CollisionDetector` is never reported colliding with itself (`candidate !== detector`)
- To get `CollisionResult` (contact point, normal, depth): call `collisionMask.collisionResultWith(other.collisionMask)` inside the callback

## Gotchas

- AABB dirty flag: position changes set `isAxisAlignedBoundingBoxDirty = true`; box recomputed lazily on the next read, which also clears the flag so subsequent reads hit the cache (physics reads AABBs in an O(n)-per-body loop — a missing reset here re-runs the full vertex transform on every read)
- `collisionDetectors` and `collidables` StateFlows are derived via `filterIsInstance` on `allActors` on `Dispatchers.Default`, pinned to main-thread via `asStateFlowOnMainThread`
- `PolygonCollisionMask.updateAxisAlignedBoundingBox()` iterates all vertices — avoid high-frequency rotation on high-vertex polygons
