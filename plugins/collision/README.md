# Collision Plugin

The `collision` plugin provides high-performance collision detection for Kubriko games. It allows actors to define their shapes (masks) and receive notifications when they overlap with other actors.

## Core Concepts

### Collidable
Any actor that participates in collisions must implement the `Collidable` interface. This requires providing a `CollisionMask`.

### CollisionDetector
A specialized `Collidable` that can react to collisions. It specifies which types of actors it is interested in and receives a callback when a collision occurs.

### Collision Masks
Several shapes are supported out of the box:
- `PointCollisionMask`: A single point.
- `CircleCollisionMask`: A circle with a defined radius.
- `BoxCollisionMask`: A rectangular box (can be rotated).
- `PolygonCollisionMask`: A convex polygon.

## Usage

1. **Register the Manager**:
   Add the `CollisionManager` to your `Kubriko` instance:
   ```kotlin
   val kubriko = Kubriko.newInstance(
       managers = listOf(
           CollisionManager.newInstance()
       )
   )
   ```

2. **Implement Collidable**:
   ```kotlin
   class Player : Actor(), Collidable {
       override val collisionMask = BoxCollisionMask(initialSize = SceneSize(10f, 20f))
       // ...
   }
   ```

3. **Detect Collisions**:
   ```kotlin
   class Bullet : Actor(), CollisionDetector {
       override val collisionMask = CircleCollisionMask(initialRadius = 2f)
       override val collidableTypes = listOf(Enemy::class)

       override fun onCollisionDetected(collidables: List<Collidable>) {
           // Handle collision
       }
   }
   ```

## Credit:
- [KPhysics](https://github.com/KPhysics/KPhysics)
- [JPhysics](https://github.com/HaydenMarshalla/JPhysics)

## Public artifact
The artifact for this module has the following ID:
`io.github.pandulapeter.kubriko:plugin-collision`
