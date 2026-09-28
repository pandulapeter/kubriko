# Make empty and zero-size polygon masks behave like a point instead of crashing or containing everything

**Challenged:** amended — fixing only the bounding box moves the crash into the narrow phase: once an empty polygon's point box overlaps another mask's box, `checkCircleToPolygonCollision` reads `polygon.vertices[faceNormalIndex]` and `findAxisOfMinPenetration` reads `polygonB.vertices[0]` on the empty list; `collisionCheck` now returns `null` for a polygon without vertices (plan 56 then gives it point semantics), with a test.

**Kind:** bug (crash)  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-collision`
**Files:** `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/mask/PolygonCollisionMask.kt`, `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/CollisionMaskExtensions.kt`, `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/DegeneratePolygonMaskTest.kt` (new), `plugins/collision/CLAUDE.md`

## Problem

Both constructors default to degenerate shapes, and neither case is handled (at 0008d027):

1. `PolygonCollisionMask()` — the public `invoke` defaults to `vertices: List<SceneOffset> = emptyList()`. The first bounding-box read crashes:
   ```kotlin
   override fun updateAxisAlignedBoundingBox(target: AxisAlignedBoundingBox) {
       val firstPoint = rotationMatrix.times(vertices[0])
   ```
   Verified: `PolygonCollisionMask().axisAlignedBoundingBox` throws `IndexOutOfBoundsException: Empty list doesn't contain element at index 0.` A `Collidable` created with a placeholder mask crashes the tick in `CollisionManager` (and `drawDebugBounds` crashes the debug overlay on the same `vertices[0]`).
2. `BoxCollisionMask()` — `initialSize` defaults to `SceneSize.Zero`, so all four corners coincide. The hull keeps two identical vertices, every face vector is zero, `normalized()` of a zero vector is zero, and
   ```kotlin
   override fun isSceneOffsetInside(sceneOffset: SceneOffset): Boolean {
       for (i in vertices.indices) {
           ...
           if (objectPoint.dot(rotationMatrix.times(normals[i])) > SceneUnit.Zero) {
               return false
   ```
   never returns `false`. Verified: `BoxCollisionMask(SceneOffset.Zero).isSceneOffsetInside(SceneOffset(1000, 1000))` is `true`. The public `ComplexCollisionMask.isSceneOffsetInside` KDoc says "Checks if the given [sceneOffset] is inside the mask". (The `SceneOffset.isInside(collisionMask)` extension is protected by its bounding-box pre-check, and physics' internal `ShadowCasting` is the only other in-repo caller, so the visible impact is on consumers calling the method directly.)

## Fix

In `PolygonCollisionMask`:
1. `updateAxisAlignedBoundingBox`: when `vertices` is empty, fall back to `super.updateAxisAlignedBoundingBox(target)` (the point box at `position`).
2. `isSceneOffsetInside`: when the polygon has no area — `vertices.size < 3` is not the right test (a two-vertex segment is a legitimate degenerate hull), so test `size == SceneSize.Zero` (already computed, and zero for empty and all-coincident vertex sets) — return `sceneOffset == position`, the point semantics. Otherwise unchanged.
3. `drawDebugBounds`: when `vertices` is empty, draw nothing (or delegate to `PointCollisionMask`'s dot — pick whichever is one line).
3a. `extensions/CollisionMaskExtensions.kt` → `collisionCheck`: before the four circle/polygon branches, return `null`
   when either mask is a `PolygonCollisionMask` whose `vertices` is empty. Without this, the bounding-box fix only
   moves the crash: a placeholder mask sitting inside another mask's bounds reaches `checkCircleToPolygonCollision`
   (`polygon.vertices[faceNormalIndex]` on an empty list) or `findAxisOfMinPenetration` (`polygonB.vertices[0]`).
   Plan 56 later routes these to its point branches instead of `null`.
4. Leave the defaults and constructors alone (changing them would be an API change).
5. `plugins/collision/CLAUDE.md`: one line in the mask hierarchy section that an empty or zero-size polygon behaves like a point at its position.

A single vertex is left as it is (`generateConvexHull` returns it uncentered); it has a correct bounding box and is out of scope.

## Tests

`DegeneratePolygonMaskTest` in `commonTest`:
- `PolygonCollisionMask(initialPosition = (5, 7)).axisAlignedBoundingBox` does not throw and has `min == max == (5, 7)`.
- `BoxCollisionMask(initialPosition = (5, 7))` (zero size): `isSceneOffsetInside((1000, 1000))` is `false`, `isSceneOffsetInside((5, 7))` is `true`.
- Regression: a 10×10 `BoxCollisionMask` at the origin still contains `(1, 1)` and not `(20, 20)`.
- `PolygonCollisionMask()` at the origin against a `CircleCollisionMask` of radius 10 at the origin and against a
  10×10 `BoxCollisionMask` at the origin, in both orders: `hasCollisionWith` and `collisionResultWith` do not throw (assert only that — plan 56 later makes these collide as a point)
  (at HEAD, and with only the bounding-box fix, they throw `IndexOutOfBoundsException`).

Run `./gradlew :plugins:collision:desktopTest`.

## Manual check

None.
