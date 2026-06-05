<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# demo-particles CLAUDE.md

## What this demo demonstrates

Interactive particle emitter showcase for the `plugin-particles` module. Users can adjust emission
rate, lifespan, switch between continuous and burst modes, and watch the engine's particle pooling
system recycle state objects.

## Entry point and managers/plugins

`ParticlesDemo` (`@Composable`) is the entry point. `ParticlesDemoStateHolderImpl` creates:
- `ParticleManager` — the particle plugin Manager, required for `ParticleEmitter` actors to function
- `ParticlesDemoManager` — custom Manager and emitter; also the sole Actor in the scene

## Key actor types

**`ParticlesDemoManager`** implements both `Manager` and `ParticleEmitter<DemoParticleState>`.
It adds itself to `ActorManager` during `onInitialize` so it acts as the stationary emitter at the
viewport centre. Emission mode switches reactively: a `isEmittingContinuously` flow drives
`particleEmissionMode` between `Continuous { rate }` and `Inactive` via `launchIn(scope)`.

Burst mode is triggered imperatively: `burst()` sets `particleEmissionMode =
ParticleEmitter.Mode.Burst(count)` for a single-frame burst without a persistent flow.

**`DemoParticleState`** extends `ParticleEmitter.ParticleState` — the pooled object the engine
allocates once and reuses. It stores per-particle state: `direction` (random radian), `hue`
(random 0–360), `remainingLifespan`, `currentProgress`. The `reset()` method re-randomises all
fields so the pool can hand the same instance back to a new particle.

`update()` returns `false` (signal the engine to reclaim the state) in two conditions: progress
≥ 1 or `body.scale.horizontal < 0.05`. Shrinking below 5% scale is used as an early-termination
guard to avoid nearly-invisible particles consuming pool slots.

`draw()` renders a filled HSV circle for the first 70% of lifetime, then a plain black-outline
ghost for the fade-out tail, creating a sparkle-then-dissipate visual.

## Non-obvious implementation patterns

**Pool reuse via `reuseParticleState`.** `ParticlesDemoManager` overrides both `createParticleState`
(called when the pool is empty) and `reuseParticleState` (called when a recycled slot is available).
Both delegate to `DemoParticleState.reset()`, keeping the pool warm without per-frame allocation.

**Lifespan is multiplied × 6 on the way in.** The UI slider exposes `lifespan` in an intuitive
scale; internally the value is passed as `lifespan.value * 6` to the particle state so the raw
millisecond countdown stays in a visually calibrated range without surfacing the multiplier to users.

**Manager-as-Actor pattern.** Using `Unique` on `ParticlesDemoManager` ensures exactly one
emitter exists. Registering it as an Actor also means it is eligible for viewport culling sleep,
though as a zero-size emitter at the origin it stays active.

## Platform-specific considerations

No platform-specific source files in this module. The `plugin-particles` module is pure
multiplatform; particle rendering uses standard `DrawScope` calls that work identically on all
targets.
