# Keep applying explosion impulses after skipping a body at the epicenter

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-physics`
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/explosions/ProximityExplosion.kt`, `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/explosions/RaycastExplosion.kt`, `plugins/physics/src/commonTest/kotlin/com/pandulapeter/kubriko/physics/ExplosionTest.kt` (new)

**Decision needed:** change the simulation so the bodies after an epicenter body receive their impulse? — recommended: **yes** (the alternative is keeping a bug that makes explosions randomly do nothing).

## Problem

Both explosions guard against dividing by a zero distance with `return` inside the loop (at 0008d027):

```kotlin
// ProximityExplosion.applyBlastImpulse
for (b in bodiesEffected) {
    val blastDir = b.position - epicenter
    val distance = blastDir.length()
    if (distance == SceneUnit.Zero) return
    ...
}
```

```kotlin
// RaycastExplosion.applyBlastImpulse
for (ray in raysInContact) {
    val blastDir = ray.coordinates.minus(rayScatter.epicenter)
    val distance = blastDir.length()
    if (distance == SceneUnit.Zero) return
    ...
}
```

One body sitting exactly on the epicenter aborts the impulse for every body that comes after it in the list. The natural way to hit it is an exploding object that is itself a rigid body and uses its own `physicsBody.position` as the epicenter (a grenade, a barrel): whether the other bodies are pushed then depends on where it sits in the list. (`demo-physics`'s `Bomb` is not a `RigidBody` and uses the click position, so the Showcase practically never hits it.) Verified with a probe: `ProximityExplosion` with a body at the epicenter first and a second body 10 units away → the second body's velocity stays `(0, 0)`.

## Fix

Replace `return` with `continue` in both loops. No other change.

## Tests

`ExplosionTest` in `commonTest`: two `PhysicsBody(CircleCollisionMask(...))` instances, one at `(0, 0)` and one at `(10, 0)`; `ProximityExplosion(epicenter = (0, 0), proximity = 100)`, `update(listOf(atEpicenter, other))`, `applyBlastImpulse(1000)` → `other.velocity.x > 0` and `atEpicenter.velocity == SceneOffset.Zero`. No `RaycastExplosion` test: a ray can only report a hit exactly at the epicenter when the epicenter lies on a surface, which is not a stable setup to construct; the change there is the same one keyword.

Run `./gradlew :plugins:physics:desktopTest`.

## Manual check

None practical in the Showcase (see above); the unit test covers it. Optionally confirm the Physics demo's bombs still push shapes as before (`./gradlew :app:desktop:run`).
