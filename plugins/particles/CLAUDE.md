<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-particles

Object-pooled particle system. Emitters spawn short-lived particles backed by reusable `ParticleState` instances. Particles are not Actors: they are drawn by internal `ParticleBatch` actors.

## Key Files

- `src/commonMain/.../ParticleManagerImpl.kt` — owns the pool cache keyed by `KClass<ParticleState>`, emits, ages and recycles particles, and owns the batches
- `src/commonMain/.../ParticleEmitter.kt` — trait interface implemented by emitter Actors; also defines `ParticleState` and `Mode`
- `src/commonMain/.../implementation/ParticleBatch.kt` — internal `Visible` actor that draws every live particle of one `drawingOrder`

## Batching

One `ParticleBatch` exists per distinct `drawingOrder`, created lazily and kept for the lifetime of the manager, so particles still interleave correctly with the rest of the scene. Each batch is double-buffered: the tick thread fills the working buffer (`beginFrame` ages and recycles, `addParticle` appends, `endFrame` publishes it with one volatile write) while the render thread only reads the published list.

When `ActorManager.removeAll()` (or any removal) takes a batch out of the scene, the manager re-adds it on the next tick — even while paused — and drops (recycles) its live particles at that point, so a scene reset also clears the particles on screen. A pending state keeps the batch from being queued twice while its re-add is in flight.

## Object Pool Design

Zero allocations at steady state: `ParticleManagerImpl.cache` is a `Map<KClass<ParticleState>, ArrayDeque<ParticleState>>` of available instances keyed by state type. A particle whose `update` returns false (or that is dropped with its batch) goes back into it; emission pops from it and calls `reuseParticleState` before falling back to `createParticleState`.

`cacheSize` in `ParticleManager.newInstance(cacheSize)` is **per state type**, not global.

## Emission Modes

- **Continuous**: accumulator carries fractional particles across frames — e.g. 2.5 particles/frame emits 2 one frame, 3 the next. Stored per-emitter in `emissionAccumulators`. The per-tick batch is **sub-frame staggered**: each particle is pre-aged (one `state.update(preAge)` call before it is added) by the slice of the tick interval it should already have lived — evenly spaced over `[0, delta)`, oldest first. Without this, at low frame rates the whole batch spawns on one instant at the emitter origin and renders as an expanding shell per tick (concentric rings); staggering scatters it along its trajectory into a continuous stream. The pre-age scales with `delta`, so it is negligible at 60 FPS. If a particle's lifetime fully fits inside its catch-up window (`state.update` returns false), it is recycled instead of added.
- **Burst**: fires once on the same tick the mode is set, then auto-resets `particleEmissionMode = Inactive`. **Never staggered** — a burst is a single instant by definition.

Emission is gated on `stateManager.isRunning` — particles pause when the game is paused. `onUpdate`
returns immediately in that case: with nothing to age, emit or recycle, `ParticleBatch.beginFrame()`
would only copy every live particle into the other buffer to publish the same render list again. The
already-published list keeps rendering and following the camera.

## Implementing a `ParticleState`

```kotlin
class MyParticleState : ParticleEmitter.ParticleState() {
    override val body = BoxBody(...)
    var velocity = SceneOffset.Zero
    var remainingLifetime = 1000

    // Return false once the particle is done, which recycles it into the pool:
    override fun update(deltaTimeInMilliseconds: Int): Boolean { /* no allocations here */ }
    override fun DrawScope.draw() { /* no allocations here */ }
}

class MyEmitter : ParticleEmitter<MyParticleState> {
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous { 0.1f }
    override val particleStateType = MyParticleState::class

    // Called only when the pool is empty:
    override fun createParticleState() = MyParticleState()

    // Must fully reset ALL mutable fields of the pooled state:
    override fun reuseParticleState(state: MyParticleState) {
        state.body.position = SceneOffset.Zero
        state.velocity = SceneOffset.Zero
        state.remainingLifetime = 1000
    }
}
```

`particleStateType: KClass<S>` on the emitter **must** match the actual runtime class — a mismatch breaks pool lookup silently and causes allocations every emission.

## Gotchas

- `reuseParticleState(state)` must reset **every** mutable field — the instance is re-used as-is from the previous lifetime
- Never allocate in `ParticleState.update()` or `draw()` — these run every frame per particle
- `createParticleState()` is called only on pool miss; keep it cheap
- Burst mode's auto-reset happens in the same tick as emission — set it fresh each burst cycle
- Pool size needs tuning: too small → allocations; too large → memory waste
