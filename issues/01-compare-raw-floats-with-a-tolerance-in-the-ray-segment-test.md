# Compare raw floats with a tolerance in the physics ray segment test so axis-aligned rays hit polygon edges

**Challenged:** amended — replaced the three-branch fix with one perpendicular-distance (cross-product) test: the kept sloped branch misses ~98 % of near-vertical rays built from an angle (float32 probe), the new test is a superset of the old accepted set with 0 false hits; added a near-vertical-angle ray test.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/implementation/Helpers.kt`, `plugins/physics/src/commonTest/kotlin/com/pandulapeter/kubriko/physics/RayIntersectionTest.kt`

Ships in `plugin-physics`. No public API changes (`lineIntersect` and `isPointOnLine` are `internal`).

## Problem

`Ray.updateProjection` (and through it `RaycastExplosion` / `RayScatter` and `ShadowCasting`) tests polygon bodies edge by edge with `lineIntersect`, which computes the intersection of the two infinite lines and then asks `isPointOnLine` whether that point lies on both segments. `PhysicsBody.rayIntersect` calls `isPointOnLine` a second time on the same point. At 76992502, `Helpers.kt`:

```kotlin
internal fun isPointOnLine(lineStart: SceneOffset, lineEnd: SceneOffset, point: SceneOffset): Boolean {
    if (lineStart.x == lineEnd.x) {
        return point.x == lineStart.x && ((point.y >= lineStart.y && point.y <= lineEnd.y) || (point.y <= lineStart.y && point.y >= lineEnd.y))
    } else if (lineStart.y == lineEnd.y) {
        return point.y == lineStart.y && ((point.x >= lineStart.x && point.x <= lineEnd.x) || (point.x <= lineStart.x && point.x >= lineEnd.x))
    }
```

The vertical and horizontal branches compare `SceneUnit`s. Two things go wrong there:

1. **Exact equality after rounding.** The intersection is recomputed from the line equations, so it is only equal to the segment's constant coordinate up to float rounding. A horizontal ray at `y = 7.3` against the vertical edge `x = 90` gets an intersection `x = 89.99999`, and `point.x == lineStart.x` rejects it.
2. **Signed zero.** `SceneUnit` is a value class with no custom `equals`, so `==` uses the boxed `Float.equals` semantics (`-0.0 != 0.0`), and its `compareTo` is `raw.compareTo(other.raw)` (`Float.compare`, where `-0.0 < 0.0`). A ray along `y = 0` gets an intersection `y = -0.0`, which fails both `point.y == lineStart.y` and the `>=` range checks.

The general (sloped) branch already uses a tolerance (`abs(...) < 0.01`), so only rays or edges that are exactly axis-aligned are affected. That is the common case: boxes, level geometry, and horizontal or vertical rays.

Confirmed with desktop probes at 76992502 (a 20×20 `BoxCollisionMask` body and a 400-unit `Ray`):
- Ray from `(0, 0)` going right, box at `(100, 0)`: **no hit** (expected `(90, 0)`).
- Ray from `(0, 7.3)` going right, box at `(100, 7.3)`: **no hit**. The same happens for a downward ray at `x = 7.3` and a leftward one.
- Ray from `(0, 0)` going down, box at `(0, 100)`: hits `(0, 110)`, which is the **far** edge. The near edge at `y = 90` was missed, so the reported point is wrong.
- In a randomized probe (10 000 random offsets, box edges in both vertex orders), 91 % of horizontal and 91 % of vertical rays missed a perpendicular axis-aligned edge they cross. With the fix below all of them hit, and no rays that end before the edge produce a false hit.

The plan-53 tests in `RayIntersectionTest.kt` work around this by running their rays at `y = 1`:

```kotlin
    // The mixed rays run at y = 1: along y = 0 the polygon edge test compares a -0.0 intersection with 0.0 and misses.
```

`lineIntersect` has the same signed-zero problem in `if (denominator == SceneUnit.Zero)`: a `-0.0` denominator (parallel lines) gets past it. This is harmless today because the resulting NaN/Infinity point fails every later check, but it should be fixed along with the rest.

## Fix

In `Helpers.kt`, rewrite `isPointOnLine` on raw `Float` locals (primitive `==`, `<=`, `>=` are IEEE comparisons, so `-0f == 0f`) as a single perpendicular-distance test that covers axis-aligned, sloped and near-vertical segments alike:

```kotlin
internal fun isPointOnLine(lineStart: SceneOffset, lineEnd: SceneOffset, point: SceneOffset): Boolean {
    val startX = lineStart.x.raw
    val startY = lineStart.y.raw
    val endX = lineEnd.x.raw
    val endY = lineEnd.y.raw
    val pointX = point.x.raw
    val pointY = point.y.raw
    val deltaX = endX - startX
    val deltaY = endY - startY
    val cross = deltaX * (pointY - startY) - deltaY * (pointX - startX)
    if (cross * cross >= ON_LINE_TOLERANCE_SQUARED * (deltaX * deltaX + deltaY * deltaY)) {
        return false
    }
    return if (abs(deltaX) >= abs(deltaY)) {
        pointX >= min(startX, endX) && pointX <= max(startX, endX)
    } else {
        pointY >= min(startY, endY) && pointY <= max(startY, endY)
    }
}

private const val ON_LINE_TOLERANCE_SQUARED = 0.0001f // The point may be up to 0.01 scene units off the line.
```

Why this shape rather than keeping the three branches with a tolerance in the axis-aligned ones:

- **The sloped branch is ill-conditioned for steep segments.** It tests `abs(pointY - (a * pointX + b)) < 0.01` with `a = dy / dx`. For a near-vertical segment `a` is huge and `a * pointX + b` cancels catastrophically. Rays built from an angle are never exactly vertical: `SceneOffset(AngleRadians)` of `π/2` has `x = cos(1.5707964f) = -4.4e-8`, so a "downward" ray from `RaycastExplosion`/`ShadowCasting` (which aims rays at polygon vertices ±0.001 rad) or the edge of a rotating box lands in the sloped branch, not the vertical one. A float32 probe (ray from `(c, 0)` at angle `π/2 ± 1e-6`, length 400, 20×20 box at `(c, 100)`, `c` random in ±500) misses **98 %** of hits with the three-branch fix (`a ≈ -2.3e7`, `b ≈ 1.7e8`, whose float ulp is 16, far above 0.01) and **0 %** with the version above.
- **Same answers where the three-branch version was right.** For an axis-aligned segment the cross test reduces exactly to `abs(pointX - startX) < 0.01` (or `Y`), i.e. the three-branch fix. For a sloped one it bounds the perpendicular distance, which is never larger than the old vertical distance, so every point the old branch accepted is still accepted. The extent is checked with exact bounds on the dominant axis only; the other axis follows from being on the line within 0.01, and checking it exactly is what rejects rounding on nearly-axis-aligned segments whose minor extent is a few ulps wide. A randomized float32 comparison against a float64 ground truth (20 000 rays at axis and random angles, boxes at 0°, 90°, 180°, random and `π/2 + 1e-6` rotations, ray lengths 10–800) gave: three-branch fix 24 misses, 0 false hits, 0 wrong distances; version above 0 / 0 / 0.
- **The tolerance scales with the segment, not with the coordinates.** `cross` is the perpendicular distance times the segment length, so comparing squares against `0.0001 × length²` needs no `sqrt` and stays well-conditioned at any segment length. The absolute 0.01 is unchanged from today's sloped branch. (Very large coordinates, roughly beyond 1e4, still lose precision earlier, in `lineIntersect`'s products; that is out of scope.)
- **Allocation-free.** Only primitive `Float` locals; the sloped branch's `point.x.raw in smallerX..biggerX` (a `ClosedFloatingPointRange` per call) is gone. This runs for every polygon edge of every body on every ray.
- A zero-length segment (`deltaX == deltaY == 0`) returns `false` (`0 >= 0`). Today it returns true only for a point equal to the segment; `lineIntersect` already returns `null` for it (denominator 0), so no caller can reach this case.

Drop the `sceneUnit` import (unused afterwards). `SceneUnit` stays imported only if still referenced by name.

In `lineIntersect`, change `if (denominator == SceneUnit.Zero)` to `if (denominator.raw == 0f)`: a `-0.0` denominator (parallel lines) currently gets past it. Leave the rest of `lineIntersect` and the duplicate `isPointOnLine` checks in `PhysicsBody.rayIntersect` as they are; they return the same answer once `isPointOnLine` is fixed, and removing them is out of scope.

## Tests

In `plugins/physics/src/commonTest/kotlin/com/pandulapeter/kubriko/physics/RayIntersectionTest.kt` (pure logic, no Kubriko instance):

1. Remove the `// The mixed rays run at y = 1: …` comment. Move `boxInFrontOfCircleIsTheClosestInEitherOrder` and `circleInFrontOfBoxIsTheClosest` back to `y = 0`: use `startPoint = offset(0f)`, drop the `expectedY = 1f` arguments, and change the circle case's expected x to `90f` (at `y = 0` the circle of radius 10 at x = 100 is hit at x = 90).
2. Add `axisAlignedRaysHitTheNearEdgeOfABox`. For each offset `c` in `listOf(0f, 7.3f)`, using `box(x, y)` / `circle` helpers extended to take a y coordinate:
   - a ray from `(0, c)` going right, box at `(100, c)` → hit `(90, c)`;
   - a ray from `(200, c)` going left, box at `(100, c)` → hit `(110, c)`;
   - a ray from `(c, 0)` going down, box at `(c, 100)` → hit `(c, 90)`. At `c = 0` this fails today with `(0, 110)`, the far edge;
   - a ray from `(c, 200)` going up, box at `(c, 100)` → hit `(c, 110)`.

   At 76992502 the `c = 0` right/down cases and all `c = 7.3` cases fail.
3. Add `nearVerticalRayFromAnAngleHitsTheNearEdge`: `Ray(startPoint = SceneOffset(7.3f.sceneUnit, 0f.sceneUnit), direction = (PI / 2).toFloat().rad, distance = 400f.sceneUnit)` (the `AngleRadians` constructor, so the direction's x is `-4.4e-8`, not 0) against a box at `(7.3, 100)` hits `(7.3, 90)`. It fails with a tolerance added only to the axis-aligned branches.
4. Add `rayEndingBeforeABoxMissesIt`: a ray from `(0, 0)` going right with `distance = 50f.sceneUnit` and a box at `(100, 0)` gives `rayInformation == null`. This guards against the tolerance widening the segment's extent.

Run with `./gradlew :plugins:physics:desktopTest` (commonTest runs as part of it).

## Manual check

None. No example in the Showcase app uses the physics plugin's `Ray`, `RaycastExplosion` or `ShadowCasting` (`test-collision`'s `RayEmitter` uses plugin-collision's raycast, which is unaffected), so the unit tests are the only check.
