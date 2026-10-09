# Document the undocumented public API in plugin-physics and remove `ParticleExplosion`'s stale TODO and `@param`

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-physics
**Files:**
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/JointWrapper.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/explosions/RaycastExplosion.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/explosions/RayScatter.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/joints/Joint.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/explosions/ParticleExplosion.kt`

## Problem
Public API in a published module must be fully KDoc'd (code-style skill). At 2480325f:
- `interface JointWrapper : Actor { val physicsJoint: Joint }` (JointWrapper.kt:15–18) — no KDoc at all.
- `val rayScatter: RayScatter` (RaycastExplosion.kt:34) — none.
- `class RayScatter(epicenter: SceneOffset, private val noOfRays: Int)` (:22) — class KDoc "Models rayscatter
  explosions." has no `@param`s; `var epicenter` (:29) has none (its setter moves every ray's start point).
- `var object1AttachmentPoint` (Joint.kt:35) — none.
- `ParticleExplosion` (ParticleExplosion.kt): `createParticles` KDoc documents `@param world` for a parameter that does
  not exist (:42), the body carries `//TODO: world.addBody(b)` (:63) for a JPhysics `world` the port does not have, and
  the `lifespan` constructor parameter (`private val lifespan: Float`, :28) is never read.

## Fix
KDoc only, plus deleting the stale `@param world` line and the `//TODO: world.addBody(b)` line:
- `JointWrapper`: an Actor that hands [physicsJoint] to the `PhysicsManager` of the Kubriko instance it is added to;
  add it alongside the bodies it connects (as `plugins/physics/CLAUDE.md` → Joints says). `physicsJoint`: the joint
  simulated while this Actor is in the scene.
- `RaycastExplosion.rayScatter`: the rays the explosion casts from its epicenter.
- `RayScatter`: `@param epicenter`, `@param noOfRays`; `epicenter`: setting it moves the start point of every ray
  already cast.
- `Joint.object1AttachmentPoint`: the scene position the joint is attached to on the first body
  (`physicsBody.position` plus `offset` rotated by the body's rotation, set at construction and recomputed on every
  `applyTension`, JointToBody.kt:46 / JointToPoint.kt:43).
- `ParticleExplosion`: document `lifespan` as currently unused (kept for signature compatibility). The particles are
  not added to any simulation by `createParticles`; say the caller adds them (e.g. via `RigidBody` actors).
- If any other public/protected declaration in these five files lacks KDoc, document it too.

Do not touch `RayScatter.castRays`'s body — that is P50.

## Behaviour
Unchanged — documentation and one comment line only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop`

## Manual check
none
