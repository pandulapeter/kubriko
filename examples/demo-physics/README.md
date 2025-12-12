# Physics Demo

This demo showcases the physics simulation capabilities of the Kubriko game engine, demonstrating rigid body collisions, chains, and explosions.

## Table of Contents

- [Introduction](#introduction)
- [Controls](#controls)
- [Architecture Overview](#architecture-overview)
- [Key Components](#key-components)
- [Actor Reference](#actor-reference)
- [Physics Concepts](#physics-concepts)
- [Application Flow](#application-flow)
- [Scene Serialization](#scene-serialization)
- [Tutorial: Creating Your Own Physics Actor](#tutorial-creating-your-own-physics-actor)
- [Dependencies](#dependencies)
- [Known Issues](#known-issues)
- [Credits](#credits)

## Introduction

The Physics Demo demonstrates how to use Kubriko's physics plugin to create interactive physics simulations. It features:

- **Rigid body physics** with gravity and collision detection
- **Multiple shape types**: boxes, circles, and arbitrary polygons
- **Static and dynamic objects** with different physics behaviors
- **Physics chains** using joint constraints
- **Explosion effects** with proximity-based impulse forces
- **Scene serialization** for saving/loading physics scenes

## Controls

| Action | Effect |
|--------|--------|
| **Tap/Click anywhere** | Places the currently selected object type at that position |
| **Bottom-right button** | Cycles through object modes: Shape → Chain → Explosion |

### Object Modes

- **Shape**: Spawns a random dynamic shape (box, circle, or polygon) with random size and color
- **Chain**: Creates a physics chain with 10-20 linked segments
- **Explosion**: Creates a bomb that applies blast impulse to nearby objects

## Architecture Overview

```
PhysicsDemo (Composable Entry Point)
    └── PhysicsDemoStateHolder (State Management)
          ├── ViewportManager (Display configuration - 1920 scene units height)
          ├── PhysicsManager (Physics simulation with gravity)
          ├── PointerInputManager (Touch/mouse input handling)
          ├── SerializationManager (Scene JSON loading)
          └── PhysicsDemoManager (Demo logic, UI, input handling)
                ├── Static Actors (platforms, obstacles)
                └── Dynamic Actors (falling shapes, chains, bombs)
```

## Key Components

### Entry Point

**File:** [PhysicsDemo.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/PhysicsDemo.kt)

The main composable that creates the demo viewport:

```kotlin
@Composable
fun PhysicsDemo(
    modifier: Modifier = Modifier,
    stateHolder: PhysicsDemoStateHolder = createPhysicsDemoStateHolder(
        isSceneEditorEnabled = true,
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as PhysicsDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}
```

### State Management

**File:** [PhysicsDemoStateHolder.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolder.kt)

Initializes all managers and creates the Kubriko instance:

```kotlin
private val viewportManager by lazy {
    ViewportManager.newInstance(
        aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
            height = 1920.sceneUnit
        ),
    )
}

private val physicsManager by lazy {
    PhysicsManager.newInstance()  // Default gravity: 9.81 scene units downward
}

private val pointerInputManager by lazy {
    PointerInputManager.newInstance()
}
```

### Demo Manager

**File:** [PhysicsDemoManager.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt)

Handles user input and spawns actors:

```kotlin
override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
    screenOffset.toSceneOffset(viewportManager).let { pointerSceneOffset ->
        when (actionType.value) {
            ActionType.SHAPE -> actorManager.add(
                when (ShapeType.entries.random()) {
                    ShapeType.BOX -> createDynamicBox(pointerSceneOffset)
                    ShapeType.CIRCLE -> createDynamicCircle(pointerSceneOffset)
                    ShapeType.POLYGON -> createDynamicPolygon(pointerSceneOffset)
                }
            )
            ActionType.CHAIN -> actorManager.add(
                DynamicChain.State(
                    linkCount = (10..20).random(),
                    initialCenterOffset = pointerSceneOffset,
                ).restore()
            )
            ActionType.EXPLOSION -> actorManager.add(
                Bomb(epicenter = pointerSceneOffset)
            )
        }
    }
}
```

## Actor Reference

### Static Actors

Static actors have `density = 0`, making them immovable but still participating in collisions.

| Actor | File | Description |
|-------|------|-------------|
| `StaticBox` | [StaticBox.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/StaticBox.kt) | Rectangular platform with optional rotation |
| `StaticCircle` | [StaticCircle.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/StaticCircle.kt) | Circular obstacle |
| `StaticPolygon` | [StaticPolygon.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/StaticPolygon.kt) | Arbitrary polygon shape |

**Example: StaticBox with rotation**

```kotlin
internal class StaticBox private constructor(state: State) : RigidBody, Visible, Dynamic, Editable<StaticBox> {
    override val collisionMask = BoxCollisionMask(
        initialSize = body.size * body.scale,
        initialPosition = body.position,
        initialRotation = body.rotation,
    )
    override val physicsBody = PhysicsBody(collisionMask).apply {
        density = 0f  // Makes it static
        rotation = body.rotation
    }

    @set:Exposed(name = "isRotating")
    var isRotating = state.isRotating

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isRotating) {
            body.rotation -= (0.002 * deltaTimeInMilliseconds).toFloat().rad
            physicsBody.rotation = body.rotation
            collisionMask.rotation = body.rotation
        }
    }
}
```

### Dynamic Actors

Dynamic actors are affected by gravity and physics forces.

| Actor | File | Description |
|-------|------|-------------|
| `DynamicBox` | [DynamicBox.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicBox.kt) | Rectangle with physics |
| `DynamicCircle` | [DynamicCircle.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicCircle.kt) | Circle with physics |
| `DynamicPolygon` | [DynamicPolygon.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicPolygon.kt) | Convex polygon with physics |
| `DynamicChain` | [DynamicChain.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChain.kt) | Chain of linked physics bodies |
| `Bomb` | [Bomb.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/Bomb.kt) | Expanding explosion effect |

### Base Dynamic Object

**File:** [BaseDynamicObject.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/BaseDynamicObject.kt)

All dynamic shapes extend this base class which handles position synchronization:

```kotlin
internal abstract class BaseDynamicObject : RigidBody, Visible, Dynamic {
    abstract override val collisionMask: ComplexCollisionMask

    override fun update(deltaTimeInMilliseconds: Int) {
        // Sync visual position with physics body
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        body.rotation = physicsBody.rotation
        
        // Remove actors that fall outside viewport
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            actorManager.remove(this)
        } else {
            collisionMask.position = body.position
            (collisionMask as? PolygonCollisionMask)?.rotation = body.rotation
        }
    }
}
```

## Physics Concepts

### RigidBody Trait

The `RigidBody` trait marks an actor as participating in physics simulation. It requires:

```kotlin
interface RigidBody : Actor {
    val collisionMask: ComplexCollisionMask
    val physicsBody: PhysicsBody
}
```

### PhysicsBody

Wraps a collision mask with physics properties:

```kotlin
override val physicsBody = PhysicsBody(collisionMask).apply {
    density = 1f        // Mass per unit area (0 = static)
    restitution = 0.4f  // Bounciness (0 = no bounce, 1 = perfect bounce)
}
```

### Collision Masks

Three types of collision masks are available:

| Mask | Use Case |
|------|----------|
| `BoxCollisionMask` | Axis-aligned or rotated rectangles |
| `CircleCollisionMask` | Circles |
| `PolygonCollisionMask` | Arbitrary convex polygons |

### Physics Joints

The `JointToBody` connects two physics bodies with a spring-like constraint:

```kotlin
val joint = JointToBody(
    physicsBody1 = chainLinks[index - 1].physicsBody,
    physicsBody2 = chainLink.physicsBody,
    jointLength = ChainLink.Radius,
    jointConstant = 2000f,   // Spring stiffness
    dampening = 0.0001f,     // Energy loss
    canGoSlack = false,      // Must maintain minimum length
    offset1 = SceneOffset(-ChainLink.Radius, SceneUnit.Zero),
    offset2 = SceneOffset(ChainLink.Radius, SceneUnit.Zero),
)
```

### Explosions

The `ProximityExplosion` applies impulse forces to nearby rigid bodies:

```kotlin
private val explosion = ProximityExplosion(
    epicenter = pointerSceneOffset,
    proximity = 750.sceneUnit,  // Blast radius
)

// Apply forces each frame
explosion.update(actorManager.allActors.value.filterIsInstance<RigidBody>().map { it.physicsBody })
explosion.applyBlastImpulse(25000000.sceneUnit)  // Force magnitude
```

## Application Flow

1. **Initialization**: `PhysicsDemo` composable creates `PhysicsDemoStateHolder`
2. **Manager Setup**: StateHolder lazily initializes all managers (ViewportManager, PhysicsManager, PointerInputManager)
3. **Scene Loading**: `PhysicsDemoManager.loadMap()` loads static scene from `scenes/scene_physics_test.json`
4. **User Input**: When user taps, `onPointerReleased` converts screen coordinates to scene coordinates
5. **Actor Creation**: Based on selected mode, appropriate actor is created and added via `ActorManager`
6. **Physics Loop**: Each frame, `PhysicsManager` runs physics simulation
7. **Position Sync**: Dynamic actors read positions from `physicsBody.position` in their `update()` method
8. **Rendering**: `Visible` actors draw themselves using Compose `DrawScope`
9. **Cleanup**: Actors outside viewport bounds are automatically removed

## Scene Serialization

The demo uses `EditableMetadata` for scene serialization, enabling the Scene Editor tool:

```kotlin
val serializationManager = EditableMetadata.newSerializationManagerInstance(
    EditableMetadata(
        typeId = "staticBox",
        deserializeState = { json.decodeFromString<StaticBox.State>(it) },
        instantiate = { StaticBox.State(body = BoxBody(initialPosition = it, ...)) },
    ),
    EditableMetadata(
        typeId = "dynamicChain",
        deserializeState = { json.decodeFromString<DynamicChain.State>(it) },
        instantiate = { DynamicChain.State(linkCount = 20, initialCenterOffset = it) },
    ),
    // ... more types
)
```

Scene JSON is loaded from resources:

```kotlin
private fun loadMap() = scope.launch {
    val json = Res.readBytes("files/scenes/$SCENE_NAME").decodeToString()
    processJson(json)
}
```

## Tutorial: Creating Your Own Physics Actor

Follow these steps to create a custom physics-enabled actor:

### Step 1: Define the Collision Mask

Choose the appropriate mask for your shape:

```kotlin
// For a circle
override val collisionMask = CircleCollisionMask(
    initialRadius = 50.sceneUnit,
    initialPosition = SceneOffset.Zero,
)

// For a box
override val collisionMask = BoxCollisionMask(
    initialSize = SceneSize(100.sceneUnit, 60.sceneUnit),
    initialPosition = SceneOffset.Zero,
)

// For a polygon
override val collisionMask = PolygonCollisionMask(
    initialPosition = SceneOffset.Zero,
    vertices = listOf(
        SceneOffset(-50.sceneUnit, -30.sceneUnit),
        SceneOffset(50.sceneUnit, -30.sceneUnit),
        SceneOffset(0.sceneUnit, 50.sceneUnit),
    ),
)
```

### Step 2: Create the PhysicsBody

Wrap your collision mask with physics properties:

```kotlin
override val physicsBody = PhysicsBody(collisionMask).apply {
    density = 1f        // Higher = heavier
    restitution = 0.5f  // 0 = no bounce, 1 = perfect bounce
}
```

### Step 3: Implement the RigidBody Trait

Your actor must implement `RigidBody` along with other necessary traits:

```kotlin
class MyPhysicsActor(
    initialPosition: SceneOffset,
) : RigidBody, Visible, Dynamic {
    
    override val body = BoxBody(
        initialSize = SceneSize(100.sceneUnit, 100.sceneUnit),
        initialPosition = initialPosition,
    )
    
    override val collisionMask = CircleCollisionMask(
        initialRadius = 50.sceneUnit,
        initialPosition = initialPosition,
    )
    
    override val physicsBody = PhysicsBody(collisionMask).apply {
        density = 1f
        restitution = 0.3f
    }
}
```

### Step 4: Update Position from Physics

In the `update()` method, sync the visual position with the physics simulation:

```kotlin
override fun update(deltaTimeInMilliseconds: Int) {
    // Copy position from physics body
    body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
    body.rotation = physicsBody.rotation
    
    // Also update collision mask position for consistency
    collisionMask.position = body.position
}
```

### Step 5: Render the Actor

Implement the `draw()` method from the `Visible` trait:

```kotlin
override fun DrawScope.draw() {
    drawCircle(
        color = Color.Blue,
        radius = 50f,
        center = body.size.center.raw,
    )
    // Add outline
    drawCircle(
        color = Color.Black,
        radius = 50f,
        center = body.size.center.raw,
        style = Stroke(width = 2f),
    )
}
```

### Complete Example

Here's a complete custom physics actor:

```kotlin
class BouncyBall(
    initialPosition: SceneOffset,
    private val radius: SceneUnit = 40.sceneUnit,
    private val color: Color = Color.Red,
) : RigidBody, Visible, Dynamic {

    override val body = BoxBody(
        initialSize = SceneSize(radius * 2, radius * 2),
        initialPosition = initialPosition,
    )

    override val collisionMask = CircleCollisionMask(
        initialRadius = radius,
        initialPosition = initialPosition,
    )

    override val physicsBody = PhysicsBody(collisionMask).apply {
        density = 0.5f       // Light weight
        restitution = 0.9f   // Very bouncy!
    }

    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        body.rotation = physicsBody.rotation
        collisionMask.position = body.position

        // Remove if outside viewport
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            actorManager.remove(this)
        }
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = color,
            radius = radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = body.size.center.raw,
            style = Stroke(width = 2f),
        )
    }
}
```

## Dependencies

This demo requires the following Kubriko modules:

| Module | Purpose |
|--------|---------|
| `engine` | Core Kubriko engine |
| `plugins:physics` | Physics simulation |
| `plugins:pointer-input` | Touch/mouse input handling |
| `tools:scene-editor` (optional) | Level editing capabilities |
| `tools:ui-components` | Floating buttons and info panels |

```kotlin
// build.gradle.kts
dependencies {
    implementation(projects.engine)
    implementation(projects.plugins.physics)
    implementation(projects.plugins.pointerInput)
    implementation(projects.tools.uiComponents)
    // Optional scene editor
    implementation(projects.tools.sceneEditor)
}
```

## Known Issues

- **DynamicChain Editor Preview**: There's an issue with the editor preview for chain actors (see TODO in [DynamicChain.kt](src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChain.kt))
- **Physics Plugin Documentation**: The physics plugin documentation is still pending

## Credits

The physics engine is based on:
- [KPhysics](https://github.com/KPhysics/KPhysics)
- [JPhysics](https://github.com/HaydenMarshalla/JPhysics)

---

**Public artifact:** `io.github.pandulapeter.kubriko:plugin-physics`
