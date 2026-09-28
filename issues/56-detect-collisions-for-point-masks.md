# Detect collisions between point masks and circle or polygon masks

**Challenged:** amended — plan 55 (same lane, lands first) makes `collisionCheck` return `null` for a `PolygonCollisionMask` without vertices; this plan replaces that guard by routing such a polygon to the point branches, so an empty polygon collides exactly like a point at its position, as 55's "behaves like a point" promises.

**Kind:** bug / docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `plugin-collision`
**Files:** `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/CollisionMaskExtensions.kt`, `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/PointMaskCollisionTest.kt` (new), `plugins/collision/CLAUDE.md`, `plugins/collision/README.md`, `documentation/GETTING_STARTED_08.md` (only for option B)

**Decision needed:** implement point collisions, or document that point masks never collide? — recommended: **A, implement point-vs-circle and point-vs-polygon** (new collisions become observable through `onCollisionDetected`, `hasCollisionWith`, `collisionResultWith` and the sliding helpers).

## Problem

`CollisionMaskExtensions.kt`, `collisionCheck` (at 0008d027) only handles circle/polygon pairs:

```kotlin
when {
    collisionMaskA is CircleCollisionMask && collisionMaskB is CircleCollisionMask -> ...
    collisionMaskA is CircleCollisionMask && collisionMaskB is PolygonCollisionMask -> ...
    collisionMaskA is PolygonCollisionMask && collisionMaskB is CircleCollisionMask -> ...
    collisionMaskA is PolygonCollisionMask && collisionMaskB is PolygonCollisionMask -> ...
    else -> null
}
```

A plain `PointCollisionMask` (which `PolygonCollisionMask` extends, but a bare one is neither) falls to `else -> null`, so it never collides with anything. The README advertises "**Multiple Mask Shapes**: Supports Points, Circles, Boxes, and Polygons" and lists `PointCollisionMask: A single point`; `documentation/GETTING_STARTED_08.md` names it as a collision shape. A consumer using a point mask for a bullet or a pickup trigger gets no `onCollisionDetected` and no error. Only `plugins/collision/CLAUDE.md` says "Point or mismatched | Returns `null`". Verified: a point at `(1, 1)` against a circle of radius 10 at the origin, and against a 10×10 box at the origin → `hasCollisionWith` is `false` for both.

## Fix

- **A (recommended) — implement.** Add branches (ordered after the existing four, and matching `collisionMaskA !is ComplexCollisionMask` so polygons/circles keep their paths) for point–circle, circle–point, point–polygon, polygon–point; point–point stays `null`. Respect the three result modes like the existing checks (`RESULT_NONE` returns the `COLLISION_DETECTED` marker without computing a result; `RESULT_SCRATCH` goes through `collisionResult(...)`):
  - Point–circle: collide when the squared distance to the center is below `radius²` (raw floats, no square root in the `RESULT_NONE` path). Result: `contact` = the point, `contactNormal` = the unit vector along the line between the point and the center, oriented from mask A to mask B as `checkCircleToCircleCollision` does (a fixed axis when the point is exactly at the center), `penetration` = `radius - distance`.
  - Point–polygon: collide when `polygon.isSceneOffsetInside(point)`. Result: transform the point into polygon space (as `checkCircleToPolygonCollision` does with `transposedRotationMatrix`), find the face with the smallest separation (`normals[i] · (p - vertices[i])`, the largest value, which is ≤ 0), `penetration` = its negation, `contactNormal` = that face normal rotated back to world space, `contact` = the point.
  - The normal always points from A to B, so the swapped order flips it (as `shouldFlipContactNormal` does for circle–polygon).
  No allocation beyond the existing `RESULT_OBJECT` result.
  - An empty `PolygonCollisionMask` (no vertices) is a point: replace plan 55's early `null` in `collisionCheck` with
    the point branches for it (point side = the empty polygon), ahead of the circle/polygon branches that would
    index its empty `vertices`. Two points, or an empty polygon and a point, stay `null`. Add one case to the tests:
    `PolygonCollisionMask(initialPosition = (1, 1))` vs the circle of radius 10 at the origin → true in both orders.
  Docs: `plugins/collision/CLAUDE.md` narrow-phase table gains the two rows and "Point–point | `null`"; `README.md` keeps its claim, adding "(points collide with circles and polygons, not with other points)".
- **B — document only.** Leave the code; change the README bullet and `PointCollisionMask` entry to say point masks are for positioning/raycast origins and never collide, and adjust the sentence in `documentation/GETTING_STARTED_08.md`.

Physics is unaffected either way (`PhysicsBody` requires a `ComplexCollisionMask`).

## Tests

`PointMaskCollisionTest` in `commonTest` (option A):
- Point `(1, 1)` vs circle `r = 10` at origin → `hasCollisionWith` true in both orders; point `(20, 0)` → false.
- Point `(1, 1)` vs 10×10 `BoxCollisionMask` at origin → true in both orders; `(20, 20)` → false; a rotated box (45°) with the point just inside a corner region that the unrotated box would not contain → true.
- `collisionResultWith` for point `(8, 0)` vs circle `r = 10` → penetration ≈ 2, contact `(8, 0)`, and a unit normal on the x axis whose sign matches what `checkCircleToCircleCollision` produces for the same A/B order; the swapped order gives the negated normal.
- Point vs point at the same position → false.

Run `./gradlew :plugins:collision:desktopTest`.

## Manual check

None.
