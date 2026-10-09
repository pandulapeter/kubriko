# Pin plugin-collision's raycast results with tests

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-collision (test sources only)
**Files:**
- new `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/RaycastTest.kt`

## Problem
`RaycastExtensions.kt` (391 lines) publishes `CollisionMask.raycast`, `List<CollisionMask>.raycast`,
`raycastDistance` and `segmentCast`, and Tesselar calls `raycastDistance` (`gameplay/actor/Dog.kt`,
`gameplay/actor/NonPlayerCharacter.kt`). At 2480325f plugin-collision has no test that calls any of them
(`grep -rln "raycast\|segmentCast" plugins/collision/src/*Test` is empty). P53 refactors the polygon scan behind them
and needs the current results pinned first.

## Fix
Tests only — no production code. `RaycastTest` in package `com.pandulapeter.kubriko.collision`, masks built with the
public factories (`CircleCollisionMask(...)`, `BoxCollisionMask(initialPosition, initialSize, initialRotation)`,
`PolygonCollisionMask(vertices, ...)`), pure math, no drawing:
- a ray crossing an axis-aligned box: `raycast` returns the entry edge's point, outward normal and distance, and
  `listOf(box).raycastDistance(...)` returns the same distance;
- the same against a rotated box and a triangle `PolygonCollisionMask` (tolerance-based asserts, e.g. 1e-4, since the
  expected values are computed by hand);
- a circle: entry point, normal (center → point, normalized) and distance;
- a ray starting inside a polygon or circle: `raycast` returns `null`, `raycastDistance` returns `maxDistance`;
- a ray parallel to an edge and missing the shape; a ray beyond `maxDistance`; a zero-length direction;
- the List overloads pick the nearest of several masks regardless of list order, and `segmentCast(start, end)` equals
  `raycast(start, end - start, |end - start|)`;
- a point mask is never hit.

Every test must pass on 2480325f. If one fails, that is a real defect: `@Ignore` it with the reason and report it as
a new finding rather than changing production code here.

## Behaviour
Unchanged — tests only.

## Public API
None.

## Tests
This plan is the test.

## Verify
`./gradlew :plugins:collision:desktopTest`

## Manual check
none
