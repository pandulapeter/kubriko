# Re-attach particle batches after the actor manager drops them, clearing their particles

**Challenged:** sound

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `plugin-particles`
**Files:** `plugins/particles/src/commonMain/kotlin/com/pandulapeter/kubriko/particles/ParticleManagerImpl.kt`, `plugins/particles/src/commonMain/kotlin/com/pandulapeter/kubriko/particles/implementation/ParticleBatch.kt`, `plugins/particles/src/desktopTest/kotlin/com/pandulapeter/kubriko/particles/ParticleBatchReattachTest.kt` (new), `plugins/particles/CLAUDE.md`

## Problem

Particles are drawn by one internal `ParticleBatch` actor per `drawingOrder`, created and added once (`ParticleManagerImpl.kt` at 0008d027):

```kotlin
private fun batchFor(drawingOrder: Float): ParticleBatch {
    for (i in batches.indices) {
        if (batches[i].drawingOrder == drawingOrder) {
            return batches[i]
        }
    }
    return ParticleBatch(drawingOrder).also { batch ->
        batches.add(batch)
        actorManager.add(batch)
    }
}
```

`actorManager.removeAll()` — the usual level-restart call (`game-annoyed-penguins`, `demo-physics` and `demo-performance` use it, though none of them has a `ParticleManager`, which is why the Showcase never shows it) — removes the batch actors along with everything else. `batches` still holds them, so `batchFor` keeps returning them and `onUpdate` keeps simulating their particles, but nothing adds them back: **from then on no particle of that `drawingOrder` is ever drawn again for the lifetime of the manager**. Before particle batching, particles were actors and `removeAll()` removed them, so a restart also cleared the particles on screen; now they live on invisibly.

## Fix

1. `ParticleBatch`: track attachment in a `@Volatile` field with three states — detached, pending, attached (an `Int` with three private constants, or a small private enum; no allocation per frame). `onAdded` sets attached; override `onRemoved` to set detached. Start in detached.
2. `ParticleManagerImpl`: replace the add in `batchFor` with a helper that, for a detached batch, sets it to pending and calls `actorManager.add(batch)`. At the start of `onUpdate` — **before** the `isRunning` early return, so a paused scene that was reset still gets its batches back — loop `batches` by index and, for every detached batch, first drop its particles (step 3) and then re-add it through the helper. The pending state is what prevents a second `add` while the first is still queued: at 0008d027 `ActorManager` does not de-duplicate additions (engine plan 03 changes that), so adding the same batch twice would draw its particles twice; even with de-duplication it would enqueue an operation per tick. The ordering is safe because `ActorManager` processes operations serially and a re-add is only issued after `onRemoved` has fired: a pending add followed by a `removeAll()` in the same batch ends with `onRemoved` (detached, re-added next tick) both at 0008d027 and under engine plan 04's callback pairing (which fires `onAdded` then `onRemoved`).
3. `ParticleBatch.dropAll(recycle)`: hands every state in the published `renderList` to `recycle`, clears both buffers and publishes `emptyList()` (the tick thread owns the buffers; the render thread only reads `renderList`, which stays a single volatile write). This restores the pre-batching behaviour that `removeAll()` clears live particles; emitters are removed by the same call, so nothing refills them.
4. `plugins/particles/CLAUDE.md`: the file is stale (it still describes per-particle `Particle` actors, `Particle.kt` and `ParticleState.kt`, none of which exist). Rewrite "Key Files" and "Object Pool Design" to describe `ParticleBatch` (one per `drawingOrder`, double-buffered) and add: the batch re-adds itself on the next tick after `ActorManager.removeAll()` removes it, and its live particles are dropped (recycled) at that point.

## Tests

`ParticleBatchReattachTest` in `desktopTest` (`ParticleBatch` is internal, visible to the module's tests): build `Kubriko.newInstance(ActorManager.newInstance(shouldComposeLayers = false), ParticleManager.newInstance(cacheSize = 10), tickSource = TickSource.manual())`, `start()`, add an emitter actor with `Mode.Continuous` and a trivial `ParticleState`, tick until `allActors` contains a `ParticleBatch` (poll with a 2 s timeout, ticking 16 ms each round). Call `removeAll()`, poll until `allActors` is empty, then tick and poll until a `ParticleBatch` is present again; assert exactly one instance of it is present (no duplicate add). Dispose the instance at the end.

Run `./gradlew :plugins:particles:desktopTest`.

## Manual check

No Showcase example calls `removeAll()` on an instance that has particles. In `demo-particles`, temporarily make one of its controls call `actorManager.removeAll()` followed by re-adding the demo's emitter actors; run `./gradlew :app:desktop:run`, open the particles demo, use the control → particles keep appearing (before the fix they never appear again). Revert the temporary change.
