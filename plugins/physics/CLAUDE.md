<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-physics

Impulse-based rigid-body physics via KPhysics/JPhysics. Depends on `plugin-collision` for mask types.

## Key Files

- `src/commonMain/.../PhysicsManagerImpl.kt` — update loop, broad/narrow phase, arbiter pool, joint solving
- `src/commonMain/.../PhysicsBody.kt` — wraps a `ComplexCollisionMask`; holds velocity, force, mass, friction, restitution
- `src/commonMain/.../RigidBody.kt` — Actor trait; extends `Collidable`
- `src/commonMain/.../JointWrapper.kt` — Actor trait exposing a `Joint` to the simulation
- `src/commonMain/.../joints/` — `JointToBody` (body–body spring), `JointToPoint` (body–fixed-point spring)
- `src/commonMain/.../explosions/` — `ProximityExplosion`, `RaycastExplosion`, `ParticleExplosion`
- `src/commonMain/.../rays/Ray.kt` — single ray; `updateProjection(bodies)` then read `rayInformation`

## Body Shapes

`PhysicsBody` requires a `ComplexCollisionMask`:
- `CircleCollisionMask(initialRadius, initialPosition)` — mass from `π r² × density`
- `PolygonCollisionMask(vertices, ...)` — auto convex-hulled and centered. **No concave bodies.**

No rectangle shortcut exists for physics; use `PolygonCollisionMask` with four corner vertices or `BoxCollisionMask`.

## Static Bodies

Set `density = 0f` to make a body static — `invMass` and `invInertia` become zero, integration is skipped. Two static bodies in contact are also skipped.

## Critical: Position Sync Required in `Dynamic.update()`

`PhysicsManager` moves `physicsBody.position` and `physicsBody.rotation` directly. The `Visible` `BoxBody` and `collisionMask.position` are **NOT** updated automatically. Every `RigidBody + Dynamic` must copy back:

```kotlin
override fun update(deltaTimeInMilliseconds: Int) {
    body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
    body.rotation = physicsBody.rotation
    collisionMask.position = body.position
    (collisionMask as? PolygonCollisionMask)?.rotation = body.rotation
}
```

Forgetting this leaves rendering and collision detection out of sync.

## Simulation Parameters

- `gravity: MutableStateFlow<SceneOffset>` — default `(0, 9.81)` scene units/s²; Y positive = downward
- `simulationSpeed: MutableStateFlow<Float>` — pure time multiplier (scales the sub-step dt, not the sub-step count)
- `penetrationCorrection: Float` (constructor-only, default 0.2) — fraction of overlap corrected per step; too high causes jitter

Simulation pauses when `stateManager.isRunning` is false.

## Fixed-Timestep Accumulator

`onUpdate` does **not** integrate one Euler step the size of the tick delta. It accumulates real elapsed
time and runs `step(dt)` — recycle arbiters, `broadPhaseCheck`, `semiImplicit`, penetration resolution —
in constant `FIXED_TIME_STEP_IN_MILLISECONDS` (16 ms) quanta, carrying the remainder between ticks. This
keeps the simulation frame-rate independent: at 10 FPS one ~100 ms tick runs ~6 small steps instead of one
huge one. A single large step is what caused tunneling (bodies jump past each other before collision is
detected), joint blow-up (springs overshoot rest length), and stuck/repeating contacts at low frame rates.

- Collisions are re-detected **every sub-step** — that is what prevents tunneling; do not hoist
  `broadPhaseCheck` out of the loop. Sweep-and-prune temporal coherence holds across sub-steps (bodies
  barely move per step), so the extra broad-phase passes stay near-O(n).
- `MAXIMUM_SUB_STEPS_PER_TICK` (8) caps worst-case cost and prevents the spiral of death. Frame rates
  down to ~7.5 FPS stay fully time-accurate; below that the sim runs slower rather than exploding (backlog
  is dropped, not caught up in a burst).
- 16 ms ≈ the per-step dt the engine was tuned against at 60 FPS (`16 * simulationSpeed 1 / 100`), so
  typical-frame-rate behavior is essentially unchanged; the accumulator only changes things when throttled.
- The accumulator is not advanced while `isRunning` is false, so resuming does not trigger a catch-up burst.

## Hardcoded Velocity/Force Thresholds (not configurable)

Setters clamp values to zero: velocity/force magnitude < 0.1 → `Zero`; angular velocity absolute < 0.01 → `Zero`; torque absolute < 0.1 → `Zero`.

## Joints (Hooke's Law + Dampening)

```kotlin
object : JointWrapper {
    override val physicsJoint = JointToBody(
        physicsBody1 = bodyA, physicsBody2 = bodyB,
        jointLength = 24f.sceneUnit, jointConstant = 2000f, dampening = 0.0001f,
        canGoSlack = false,  // true = rope behaviour (resists extension only)
    )
}
```

Add the `JointWrapper` actor to the scene alongside the bodies it connects.

## Explosions

| Class | Behaviour |
|---|---|
| `ProximityExplosion(epicenter, proximity)` | Impulse to all bodies within radius; no line-of-sight check |
| `RaycastExplosion(epicenter, noOfRays, distance, worldBodies)` | Impulse to first hit per ray only |
| `ParticleExplosion(epicenter, noOfParticles, lifespan)` | Creates `PhysicsBody` particles; call `createParticles()` before `applyBlastImpulse()` |

## Arbiter Pooling

`PhysicsManagerImpl` pools `Arbiter` objects to avoid per-frame allocation. Do not hold references to arbiters beyond the physics update — they are recycled every tick.

## Broad Phase: Sweep-and-Prune

`broadPhaseCheck()` keeps body indices sorted by AABB left edge (insertion sort over reusable `IntArray`/`FloatArray` scratch buffers — nearly O(n) per frame thanks to temporal coherence; the order is reset to identity whenever the `rigidBodies` list reference changes). Each body is then only tested against neighbors whose x-extents can still overlap. AABBs are read once per body per frame into the scratch arrays.

**Order preservation is load-bearing**: candidate pairs are packed into a `LongArray` as `(minIndex shl 32) or maxIndex` and sorted before the narrow phase, restoring the exact `(i, j)` order of a naive nested loop. The sequential impulse solver iterates arbiters in insertion order, so changing pair order would change simulation results. Do not "optimize away" the pair sort.

The `>=` x-axis break condition matches `isOverlapping` exactly (touching edges count as non-overlapping), so the sweep prunes no pair the full AABB check would have accepted.

`applyLinearDrag` early-returns when `linearDampening == 0f` (the default) or velocity is zero — a zero drag force is a no-op — and reuses the computed magnitude instead of calling `normalized()`, which would take the same square root twice.
