# Physics Plugin

The `physics` plugin adds a 2D rigid body physics simulation to Kubriko. It allows actors to have physical properties like mass, friction, velocity, and gravity, and handles collisions and joints between them.

## Features

- **Rigid Body Dynamics**: Realistic movement with forces, impulses, and torque.
- **Multiple Shapes**: Supports circular and polygonal physics bodies.
- **Joint Systems**: Connect bodies together with distance joints (to other bodies or fixed points).
- **Ray casting**: Query the world for intersections along a line.
- **Explosions**: Support for various types of area-of-effect forces (proximity, ray-scatter, etc.).
- **Configurable World**: Customizable gravity and simulation speed.

## Usage

### 1. Register the Manager

Add the `PhysicsManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    PhysicsManager.newInstance(
        initialGravity = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit)
    ),
    // ... other managers
)
```

### 2. Implement RigidBody

Any actor that should be part of the physics simulation must implement the `RigidBody` interface and provide a `PhysicsBody`.

```kotlin
class Crate : Actor(), RigidBody {
    override val collisionMask = BoxCollisionMask(initialSize = SceneSize(10f, 10f))
    override val physicsBody = PhysicsBody(
        collisionMask = collisionMask,
        density = 1f,
        restitution = 0.5f
    )
}
```

### 3. Apply Forces and Impulses

You can manipulate physical objects by applying forces or impulses:

```kotlin
// Apply a sudden jump impulse
crate.physicsBody.applyLinearImpulse(SceneOffset(0f.sceneUnit, (-5f).sceneUnit))

// Apply a constant push force
crate.physicsBody.applyForce(SceneOffset(2f.sceneUnit, 0f.sceneUnit))
```

## Credits

- [KPhysics](https://github.com/KPhysics/KPhysics)
- [JPhysics](https://github.com/HaydenMarshalla/JPhysics)

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-physics`
