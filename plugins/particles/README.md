# Particles Plugin

The `particles` plugin provides a high-performance particle system for the Kubriko engine. It allows for the efficient creation, management, and rendering of large numbers of short-lived visual effects like smoke, fire, or explosions.

## Features

- **Efficient Pooling**: Automatically manages a pool of particle objects to minimize garbage collection.
- **Emission Modes**: Supports continuous emission and burst emission.
- **Customizable Particles**: Define your own particle behavior and rendering by implementing `ParticleState`.
- **Z-Order Support**: Particles can have their own drawing order.

## Usage

### 1. Register the Manager

Add the `ParticleManager` to your `Kubriko` instance. You can specify a maximum cache size for pooled particles.

```kotlin
val kubriko = Kubriko.newInstance(
    ParticleManager.newInstance(cacheSize = 2000),
    // ... other managers
)
```

### 2. Implement a Particle Emitter

Create an actor that implements the `ParticleEmitter` interface. You'll also need a custom `ParticleState` implementation.

```kotlin
class SparkEmitter : Actor, ParticleEmitter<SparkState> {
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous { 0.5f }
    override val particleStateType = SparkState::class

    override fun createParticleState() = SparkState()
    
    override fun reuseParticleState(state: SparkState) {
        // Reset the state for a new spark (position, velocity, etc.)
    }
}

class SparkState : ParticleEmitter.ParticleState() {
    override val body = BoxBody(...)
    
    override fun update(deltaTimeInMilliseconds: Int): Boolean {
        // Move the spark and return true if it's still alive
    }

    override fun DrawScope.draw() {
        // Render the spark
    }
}
```

### 3. Add the Emitter to the Game

Simply add your emitter actor to the `ActorManager`. The `ParticleManager` will automatically detect it and start managing its particles.

```kotlin
actorManager.add(SparkEmitter())
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-particles`
