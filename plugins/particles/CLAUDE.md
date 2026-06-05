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

Object-pooled particle system. Emitters spawn short-lived `Particle` Actors backed by reusable `ParticleState` instances.

## Key Files

- `src/commonMain/.../ParticleManagerImpl.kt` — owns the pool cache keyed by `KClass<ParticleState>`
- `src/commonMain/.../ParticleEmitter.kt` — trait interface implemented by emitter Actors
- `src/commonMain/.../Particle.kt` — internal Actor wrapping a `ParticleState`; implements `Visible + Dynamic`
- `src/commonMain/.../ParticleState.kt` — base class for per-particle mutable state

## Object Pool Design

Two-layer pool — zero allocations at steady state:
1. `ParticleManagerImpl.cache`: `Map<KClass<ParticleState>, ArrayDeque<Particle>>` — available instances keyed by state type
2. `Particle.onRemoved()` returns itself to the cache automatically

`cacheSize` in `ParticleManager.newInstance(cacheSize)` is **per state type**, not global.

`Particle<S>` delegates `body`, `drawingOrder`, `draw()`, and `update()` to the `ParticleState` it wraps — the state object IS the rendering/logic unit.

## Emission Modes

- **Continuous**: accumulator carries fractional particles across frames — e.g. 2.5 particles/frame emits 2 one frame, 3 the next. Stored per-emitter in `emissionAccumulators`.
- **Burst**: fires once on the same tick the mode is set, then auto-resets `particleEmissionMode = Inactive`.

Emission is gated on `stateManager.isRunning` — particles pause when the game is paused.

## Implementing a `ParticleState`

```kotlin
class MyParticleState : ParticleState() {
    // Must fully reset ALL mutable fields in reuseParticleState():
    override fun reuseParticleState() { velocity = SceneOffset.Zero; lifetime = 1000 }
    // Called only when pool is empty:
    override fun createParticleState(): MyParticleState = MyParticleState()

    override val body = BoxBody(...)
    override fun DrawScope.draw() { /* no allocations here */ }
    override fun update(deltaTimeInMilliseconds: Int) { /* no allocations here */ }
}
```

`particleStateType: KClass<S>` on the emitter **must** match the actual runtime class — a mismatch breaks pool lookup silently and causes allocations every emission.

## Gotchas

- `reuseParticleState()` must reset **every** mutable field — the instance is re-used as-is from the previous lifetime
- Never allocate in `ParticleState.update()` or `draw()` — these run every frame per particle
- `createParticleState()` is called only on pool miss; keep it cheap
- Burst mode's auto-reset happens in the same tick as emission — set it fresh each burst cycle
- Pool size needs tuning: too small → allocations; too large → memory waste
