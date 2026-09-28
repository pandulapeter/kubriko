# Fix the raycast hit point and distance for circle bodies

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-physics`
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsBody.kt`, `plugins/physics/src/commonTest/kotlin/com/pandulapeter/kubriko/physics/RayIntersectionTest.kt` (new)

**Decision needed:** change where `RaycastExplosion` rays hit circles (and therefore which bodies get pushed, and where)? — recommended: **yes, fix it**; the alternative is keeping hit points that are wrong whenever the ray does not start at the origin.

## Problem

`PhysicsBody.rayIntersect`, circle branch (at 0008d027):

```kotlin
val ray = endPoint - startPoint
...
val t1 = (-b - discriminant) / (a * 2)
if (t1.raw in 0.0..1.0) {
    if (t1 < maxDistance) {
        maxD = t1
        minPx = startPoint.x + endPoint.x * t1
        minPy = startPoint.y + endPoint.y * t1
```

Two errors:
1. The hit point uses `endPoint` where it needs the ray vector: it must be `startPoint + (endPoint - startPoint) * t1`. It is only right when the ray starts at `(0, 0)`.
2. `t1` is the parametric position along the segment (`0..1`), but it is compared with and returned as `maxDistance`, which `Ray.updateProjection` passes from body to body as a **distance** — and the polygon branch compares real distances (`startPoint.distanceTo(intersection) < maxD`). Mixing the two picks the wrong closest body: a circle behind a polygon wins (its `t1 < 1` beats any polygon distance > 1), and a polygon evaluated after a circle can never win (its distance is compared against a `t1 < 1`).

Verified with a probe: ray from `(50, 0)` along +x, length 200, against a circle at `(100, 0)` with radius 10 → hit reported at `x = 100` (expected 90). Ray from the origin against `[box at x=100 (20 wide), circle at x=150]` → the circle is reported as closest, at `x = 140`, instead of the box at `x = 90`.

`rayIntersect`, `Ray` and `ShadowCasting` are internal; the public surface that uses them is `RaycastExplosion` (which bodies are hit, the impulse direction and its `1 / distance` falloff via `ray.coordinates`).

## Fix

In the circle branch: compute `val hitDistance = ray.length() * t1` (keep `t1 in 0..1` as the on-segment test), compare `hitDistance < maxDistance`, store `maxD = hitDistance`, and set `minPx = startPoint.x + ray.x * t1`, `minPy = startPoint.y + ray.y * t1`. The polygon branch is correct and stays as it is.

## Tests

`RayIntersectionTest` in `commonTest` (`Ray` is internal, visible to the module's tests):
- `Ray(startPoint = (50, 0), direction = (1, 0), distance = 200)`, `updateProjection(listOf(circle at (100, 0), r = 10))` → `rayInformation.coordinates == (90, 0)` (with a small tolerance).
- Ray from `(0, 0)` along +x, length 400, against `listOf(box at (100, 0) size 20×20, circle at (150, 0) r = 10)` → closest body is the box, hit at `x ≈ 90`; the same with the list reversed.
- Ray against `listOf(circle at (100, 0), box at (200, 0))` → the circle, at `x ≈ 90`.

Run `./gradlew :plugins:physics:desktopTest`.

## Manual check

None in the Showcase (no example uses `RaycastExplosion`).
