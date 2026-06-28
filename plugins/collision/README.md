<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Collision Plugin

The `collision` plugin provides high-performance collision detection for Kubriko games. It allows actors to define their shapes (masks) and receive notifications when they overlap with other actors.

## Features

- **Spatial Partitioning**: Optimized detection that handles many objects efficiently.
- **Multiple Mask Shapes**: Supports Points, Circles, Boxes, and Polygons.
- **Trait Integration**: Simple integration using the `Collidable` and `CollisionDetector` traits.
- **Movement & Queries**: Slide kinematic actors along obstacles, and raycast the world for line-of-sight, hitscan, or picking.

## Usage

### 1. Register the Manager

Add the `CollisionManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    CollisionManager.newInstance(),
    // ... other managers
)
```

### 2. Implement Collidable

Any actor that participates in collisions must implement the `Collidable` interface. This requires providing a `CollisionMask`.

```kotlin
class Player : Actor(), Collidable {
    override val collisionMask = BoxCollisionMask(initialSize = SceneSize(10f, 20f))
    // ...
}
```

### 3. Detect Collisions

A `CollisionDetector` is a specialized `Collidable` that can react to collisions. It specifies which types of actors it is interested in and receives a callback when a collision occurs.

```kotlin
class Bullet : Actor(), CollisionDetector {
    override val collisionMask = CircleCollisionMask(initialRadius = 2f)
    override val collidableTypes = listOf(Enemy::class)

    override fun onCollisionDetected(collidables: List<Collidable>) {
        // Handle collision
    }
}
```

### 4. Block Movement (Optional)

For actors moved by writing their position directly (without the physics plugin) that should be stopped by solid obstacles, `slidingMovement` returns the portion of an intended movement that stays collision-free, sliding along obstacles instead of passing through them:

```kotlin
val allowed = collisionMask.slidingMovement(
    desiredMovement = velocity * deltaTimeInMilliseconds,
    obstacles = walls.map { it.collisionMask },
)
body.position += allowed
collisionMask.position = body.position
```

`depenetrationFrom(obstacles)` complements it by returning the offset that separates an actor already overlapping an obstacle (e.g. one that spawned inside scenery). A `Collidable.slidingMovement(desiredMovement, collisionManager)` overload can also gather the obstacles from the manager for you.

### 5. Query the World (Optional)

Cast a ray (or a segment between two points) against collision masks — useful for line-of-sight, hitscan weapons, or picking a shape under the cursor:

```kotlin
val hit = walls.map { it.collisionMask }.segmentCast(start = eye, end = target)
val canSee = hit == null // nothing blocks the line between eye and target
```

`CollisionManager.collidables` exposes the current collidable actors, so these queries can run against the live world without tracking the list yourself.

## Supported Masks

Several shapes are supported out of the box:
- `PointCollisionMask`: A single point.
- `CircleCollisionMask`: A circle with a defined radius.
- `BoxCollisionMask`: A rectangular box (can be rotated).
- `PolygonCollisionMask`: A convex polygon.

## Credits

- [KPhysics](https://github.com/KPhysics/KPhysics)
- [JPhysics](https://github.com/HaydenMarshalla/JPhysics)

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-collision`
