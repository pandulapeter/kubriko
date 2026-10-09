# Delete the unused `ShadowCasting`, `RayAngleInformation`, `Ray` constructors and `RayInformation.index`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-physics
**Files:**
- delete `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/rays/ShadowCasting.kt`
- delete `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/rays/RayAngleInformation.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/rays/Ray.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/rays/RayInformation.kt`

## Problem
All `internal`, all unreferenced at 2480325f (grepped plugins/, engine/, examples/, app/, tools/ and ../Tesselar):
- `internal class ShadowCasting(var startPoint: SceneOffset, private val distance: SceneUnit)` (ShadowCasting.kt:27)
  — no user anywhere. (It also carries P50's thrown-away-rotation bug: `m.transpose().times(direction)` /
  `m.times(direction)` at :76, :81.)
- `internal class RayAngleInformation` — used only by `ShadowCasting`.
- `Ray.kt:41` `constructor(direction: AngleRadians, distance: SceneUnit)` and `Ray.kt:50`
  `constructor(direction: SceneOffset, distance: SceneUnit)` — unused; `RaycastExplosion`/`RayScatter` and
  `RayIntersectionTest` use the primary constructor and `Ray(startPoint, direction: AngleRadians, distance)` (:60,
  used by `RayIntersectionTest`:71).
- `RayInformation(body, sceneOffset: SceneOffset, index)` (RayInformation.kt:64) — unused.
- `RayInformation.index` — never read; its only writer passes `-1` (Ray.kt:93
  `RayInformation(it, minPx, minPy, -1)`).

The KDoc on these is JPhysics javadoc residue ("Similar to [.Ray]", "Getter for … @return returns b variable of type
Body").

## Fix
1. Delete `ShadowCasting.kt` and `RayAngleInformation.kt`.
2. In `Ray.kt` delete the two two-argument secondary constructors with their KDoc; in the surviving
   `(startPoint, direction: AngleRadians, distance)` constructor's KDoc drop the broken "Similar to [.Ray]" line.
3. Reduce `RayInformation.kt` to

   ```kotlin
   /**
    * The closest intersection a [Ray] found: the [body] it hit and the [coordinates] of the hit.
    */
   internal class RayInformation(
       val body: PhysicsBody,
       x: SceneUnit,
       y: SceneUnit,
   ) {
       val coordinates = SceneOffset(x, y)
   }
   ```

   and change Ray.kt:93 to `RayInformation(it, minPx, minPy)`.
4. Trim imports; grep the repo for `ShadowCasting`, `RayAngleInformation` and `.index` on a `RayInformation`
   (none outside these files at 2480325f).

## Behaviour
Only unreferenced code goes. The second sweep's "manual check owed: physics demo explosions / shadow casting" is moot
for shadow casting: nothing in `examples/demo-physics` (which uses only `ProximityExplosion`, Bomb.kt:36), the app or
Tesselar uses it.

## Public API
None — every removed declaration is `internal`.

## Tests
The existing ones (`RayIntersectionTest`, `ExplosionTest`).

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop :plugins:physics:desktopTest`

## Manual check
none
