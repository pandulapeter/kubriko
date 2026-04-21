# Collision Plugin

The `collision` plugin provides high-performance collision detection for Kubriko games. It allows actors to define their shapes (masks) and receive notifications when they overlap with other actors.

## Features

- **Spatial Partitioning**: Optimized detection that handles many objects efficiently.
- **Multiple Mask Shapes**: Supports Points, Circles, Boxes, and Polygons.
- **Trait Integration**: Simple integration using the `Collidable` and `CollisionDetector` traits.

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
