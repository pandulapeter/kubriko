# Start collecting the physics body and joint lists at initialization instead of on the first tick

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `plugin-physics`
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsManagerImpl.kt`

## Problem

`PhysicsManagerImpl` (at 0008d027):

```kotlin
private val rigidBodies by lazy {
    actorManager.allActors
        .map { it.filterIsInstance<RigidBody>().map { it.physicsBody } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
}
private val joints by lazy { /* same shape, JointWrapper */ }
```

Plain `lazy` starts the `stateIn` collection on first access, which is the first `onUpdate`. That first tick reads the initial `emptyList()` because the collection has only just been launched on `Dispatchers.Default`, so **the first simulated tick after initialization ignores every body and joint**. Verified with a probe: two overlapping balls added and given 200 ms to register, then one `tick(16)` → 0 arbiters; the second tick → 1. In a game this loses the first 16 ms step (bodies spawned overlapping are resolved a frame late); with `TickSource.manual()` in tests or replays it makes the first step silently a no-op. Every other plugin Manager (`CollisionManagerImpl`, `ParticleManagerImpl`, `ShaderManagerImpl`) uses `autoInitializingLazy`, which starts the collection in `onInitialize`.

## Fix

Change both delegates to `by autoInitializingLazy { ... .asStateFlow(emptyList()) }` (the Manager helper for `stateIn(scope, SharingStarted.Eagerly, initial)`), keeping the mapping as it is. Consider `.flowOn(Dispatchers.Default)` before `asStateFlow` for parity with `CollisionManagerImpl` — optional, the scope is already on `Default`. The only behaviour change is that bodies present at initialization take part in the first tick. The race does not disappear entirely (actors added in the same instant as the first tick can still miss it, as with every Manager that mirrors `allActors`), so do not document it as a guarantee.

## Tests

No reliable unit test: the difference is a race between the first tick and a collection on `Dispatchers.Default`, and a test would have to sleep to observe it. Compile and run the module's existing tests: `./gradlew :plugins:physics:desktopTest`.

## Manual check

None needed beyond the physics demo still behaving the same: `./gradlew :app:desktop:run` → Physics demo, drop a few shapes.
