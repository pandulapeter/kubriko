# Make `RayScatter.castRays` actually cast `noOfRays` evenly rotated rays, so `RaycastExplosion` affects bodies

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-physics
**Files:**
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/explosions/RayScatter.kt`
- new `plugins/physics/src/commonTest/kotlin/com/pandulapeter/kubriko/physics/RaycastExplosionTest.kt`

## Problem
`RayScatter.castRays` (RayScatter.kt:42–50 at 2480325f) never creates a ray:

```kotlin
fun castRays(distance: SceneUnit) {
    val angle = 6.28319f / noOfRays
    val direction = SceneOffset.UpRight
    val u = RotationMatrix(angle.rad)
    for (i in rays.indices) {
        rays.add(Ray(epicenter, direction, distance))
        u.times(direction)
    }
}
```

1. `rays` (`internal val rays = mutableListOf<Ray>()`, :28) is only ever filled here, and `RaycastExplosion.init`
   calls `castRays` on a fresh `RayScatter`, so `rays.indices` is empty and the loop body never runs. (Verified by
   reading: there is no other writer of `rays` in the module.)
2. Even if it ran, `direction` never rotates: `RotationMatrix.times` (plugin-collision `RotationMatrix.kt:60`) returns
   a new `SceneOffset` that is discarded, and `direction` is a `val`. In JPhysics, `rays` was a `Ray[noOfRays]` array
   filled by `for (i = 0; i < rays.length; i++)`, and `u.mul(direction)` rotated `direction` in place.

So `RaycastExplosion.update` finds no hits and `applyBlastImpulse` does nothing, contradicting its KDoc ("Models
raycast explosions") and the physics CLAUDE.md table ("`RaycastExplosion(epicenter, noOfRays, distance,
worldBodies)` | Impulse to first hit per ray only"). Nothing in this repo or ../Tesselar constructs `RaycastExplosion`
or `RayScatter` (`examples/demo-physics` uses `ProximityExplosion`), and no test covers it — which is why it went
unnoticed.

## Decision
Awaiting the user. `RaycastExplosion` is public in a published artifact; the fix makes it start applying impulses.
- **A (recommended): fix it.** It is what the KDoc and CLAUDE.md promise; no known caller depends on the no-op.
  Mention it in the release notes as a fix.
- B: leave it and document it as non-functional.
- C: deprecate `RaycastExplosion`/`RayScatter` for removal.

## Fix (option A)
```kotlin
fun castRays(distance: SceneUnit) {
    val angle = 6.28319f / noOfRays
    var direction = SceneOffset.UpRight
    val u = RotationMatrix(angle.rad)
    rays.clear()
    repeat(noOfRays) {
        rays.add(Ray(epicenter, direction, distance))
        direction = u.times(direction)
    }
}
```

`rays.clear()` matches upstream, where a second call overwrote the array's slots instead of appending. `Ray`
normalizes `direction`, and a rotation keeps its length, so every ray is unit-direction as before. `castRays` runs
once per explosion (construction), not per frame; the allocations are unchanged in kind. Keep the existing KDoc.
Re-locate the function by the quote (P08 documents this file first but does not touch the body).

## Behaviour
Changes, deliberately: `RaycastExplosion` now casts `noOfRays` rays 360°/`noOfRays` apart starting at 45°, and
`applyBlastImpulse` pushes the nearest body each ray hits within `distance`. `RayScatter.castRays` called again
replaces the rays instead of adding more. Nothing else changes.

## Public API
No signature change; observable behaviour of `RaycastExplosion` / `RayScatter.castRays` changes as above (the
decision).

## Tests
`RaycastExplosionTest` in `commonTest` (pure logic, no drawing), modelled on `ExplosionTest`:
- `castsTheRequestedNumberOfRays`: `RaycastExplosion(SceneOffset.Zero, noOfRays = 36, distance = 100f.sceneUnit,
  worldBodies = emptyList())` → `rayScatter.rays.size == 36` (internal is visible to the module's tests), and the ray
  directions are pairwise distinct.
- `bodyWithinReachIsPushedAwayFromTheEpicenter`: a circle body of radius 10 at (50, 0) (subtends ±11.5°, so at least
  two of 36 rays hit it); `update(listOf(body))`, `applyBlastImpulse(1000f.sceneUnit)` → `body.velocity.x > 0`.
- `bodyOutOfReachIsUntouched`: same body at (500, 0) → velocity stays `SceneOffset.Zero`.
- `castingTwiceKeepsTheRayCount`: calling `rayScatter.castRays(100f.sceneUnit)` again leaves 36 rays.

The first two fail on 2480325f.

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop :plugins:physics:desktopTest`

## Manual check
none (no in-repo caller; optionally drop a `RaycastExplosion` into `demo-physics` locally to watch bodies get pushed,
then revert).
