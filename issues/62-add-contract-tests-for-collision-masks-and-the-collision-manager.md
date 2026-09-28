# Add contract tests for the collision masks and a differential test of the collision manager's broad phase

**Challenged:** amended — `collisionResultWith` takes a required `shouldSkipAxisAlignedBoundingBoxCheck`; translation and π/2-rotation invariance are asserted only for the clear placements (float rounding at 12 345 and `cos(π/2) ≠ 0` can flip an exact touch); touching placements are limited to pairs whose touch is exact; the point mask's semantics after `56` are spelled out (strict bounding-box test, point–point never collides); the manager tests wait on the published `collidables`/`collisionDetectors` sizes and clear the recorder before every tick; the differential reference mirrors the manager's own candidate filter; the allocation test uses non-copying detectors.

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all (runs on the desktop JVM)  ·  **Artifact:** `plugin-collision` (tests only)
**Files:** `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/CollisionMaskContractTest.kt` (new), `plugins/collision/src/desktopTest/kotlin/com/pandulapeter/kubriko/collision/CollisionManagerContractTest.kt` (new)

**Part of the testing extension.** It runs in lane C after `54`–`56`, which change non-finite masks, degenerate
polygons and point masks. It adds tests only.

## Problem

`CollisionManagerImpl.onUpdate` has two paths that must agree. Below `MINIMUM_CANDIDATES_FOR_BROAD_PHASE = 32`
candidates of a type it checks every candidate. At 32 or more it asks a `SpatialHashGrid` and re-sorts the result:

```kotlin
val grid = if (cache.isGridInUse) cache.grid else null
if (grid == null) {
    for (candidateIndex in candidates.indices) {
        collectIfColliding(detector, candidates[candidateIndex])
    }
} else {
```

The grid was added in the September performance pass. The only check that it finds the same collisions as the
brute-force path was a manual review. Plans `54`–`56` test their own edge cases. Nothing tests the narrow phase's
basic properties (symmetry, translation invariance, AABB containment) across the mask types, or the manager's
callback contract (which types a detector hears about, never itself, no callbacks after removal).

## Fix

**`CollisionMaskContractTest`** (commonTest, pure; mask construction and maths need no Skia; only `drawDebugBounds`
creates a `Path`):
- Build a fixed catalogue of mask **factories** (every placement builds fresh instances, since masks are mutable):
  - `CircleCollisionMask` with radius 10
  - `BoxCollisionMask` 20×10, unrotated and rotated by `π/6`, and a 16×16 square (for the rotation check)
  - `PolygonCollisionMask` (a triangle and a pentagon; note that the constructor re-centres the hull on its centroid)
  - `PointCollisionMask`. After `56` a point collides with circles and polygons, never with another point, and only
    when it lies strictly inside the other mask's bounding box: `collisionCheck` first runs the strict
    `AxisAlignedBoundingBox.isOverlapping`, and a point's box has zero size.

  For every ordered pair, place the second mask at two offsets from the first: clearly overlapping (for a point: well
  inside the other mask; for point–point: the same position, expected `false`) and clearly separated (at least 30
  units apart). Assert:
  - **Symmetry and value:** `a.hasCollisionWith(b) == b.hasCollisionWith(a)`, and equal to the expected value, in
    both placements.
  - **Touching:** only for circle–circle, circle–unrotated box and unrotated box–box, placed along one axis so that
    the touch is exact in floating point (integer coordinates). Assert only that both orders agree. Other pairs have no
    exact touching position, and a rounding-level disagreement there would not be a bug.
  - **Translation invariance:** shifting both masks by `(12 345, -6 789)` does not change the result of the two clear
    placements. (At that magnitude the float spacing is about `0.001`, so an exact touch could flip.)
  - **Rotation of the square by `π/2`** gives the same result as the unrotated square in the two clear placements.
    `RotationMatrix(π/2)` holds `cos = -4.4e-8`, not `0`, so the rotated vertices are not bitwise the same.
  - **AABB containment:** every world-space polygon vertex (`position + rotationMatrix.times(vertex)`) and every point
    on the circle (16 sampled angles) lies inside the mask's `axisAlignedBoundingBox` (the circle's box is padded by
    `radius + 0.5`). A point's box is its position.
  - **`collisionResultWith(other, shouldSkipAxisAlignedBoundingBoxCheck = false)`** (the parameter has no default)
    returns non-null exactly when `hasCollisionWith` is true, and then has a finite `contact`, a finite
    `contactNormal` of unit length (within `1e-3`) and a non-negative `penetration`.

  Use the mask-level `CollisionMask.hasCollisionWith(other)` and `collisionResultWith(…)` from
  `CollisionMaskExtensions.kt`; `Collidable.isCollidingWith` delegates to `hasCollisionWith`. An asymmetric
  polygon–polygon result in a clear placement is possible in principle (`selectionBias` can pick a different reference
  face per order, and the clipping then decides): that is a real finding, handled as the Tests section says.

**`CollisionManagerContractTest`** (desktopTest). Build with `newManualKubriko(CollisionManager.newInstance())` from
`:tools:test-fixtures`, keeping the `CollisionManager` reference. Actors are `Collidable` (so also `Positionable`,
with a `PointBody`) around one mask each. The detector and collidable lists reach the Manager through
`Dispatchers.Default` and a main-thread hop (`asStateFlowOnMainThread`, the Swing event thread on the desktop JVM),
and the two lists are published independently. So wait for registration with
`tickUntil { collisionManager.collidables.value.size == expected && collisionManager.collisionDetectors.value.size == expected }`
(detectors count as collidables too), not on a first callback. The tick that `tickUntil` returns on may still have
run with the old lists, so assertions start with the next tick. Record callbacks per tick: clear each detector's
recorded set before every tick, treat "no callback" as the empty set, and copy the list passed to
`onCollisionDetected`, which is a reused buffer.
- `detectorHearsOnlyItsTypes` — a detector with `collidableTypes = listOf(A::class)` overlaps one `A` and one `B`,
  and a second `A` is far away. After registration, tick once: exactly the overlapping `A` is reported, never the
  `B`, the far `A` or the detector itself.
- `noCallbackAfterRemoval` — remove the overlapping `A`, `awaitProcessed()`, then `tickUntil` the manager's
  `collidables.value` no longer contains it. Every tick after that reports nothing for it.
- `broadPhaseMatchesBruteForce` — the differential test. For seeds `1..10`, create `N` collidables of one type `A`
  with random masks from the catalogue above (a fresh instance each), positions in a 2 000 × 2 000 area, and random
  rotations for the polygons. Add one detector (`collidableTypes = listOf(A::class)`, also with a random mask) per 10
  collidables. Run it for `N = 20` and `N = 200`. `N` counts every instance of `A`: if the detectors are of type `A`
  too, they are candidates, which is fine as long as the reference below uses the same rule; the manager uses the grid
  from 32 candidates of a type, so `N = 20` (plus two detectors) takes the brute-force path and `N = 200` the grid.
  Then:
  - Wait for registration, then for 20 ticks: move every mask (detectors included) randomly by up to 50 units on the
    test thread, then tick once. The manager rebuilds the grids at the start of each tick, so moving between ticks is
    what the contract covers.
  - After each tick, compare each detector's reported set with the reference: every element of
    `collisionManager.collidables.value` that is an `A`, is not the detector itself (`!==`), and for which
    `detector.isCollidingWith(it)` is true — the same filter `CollisionManagerImpl.onUpdate` applies, so only the
    broad phase differs.
  - The sets must be equal on every tick, for both sizes. Order is not part of the contract; compare as sets.
  - Also assert that the reported list never holds the same candidate twice.
- `steadyStateTickAllocationIsBounded` — 300 collidables and 30 detectors, all registered, nothing moving. These
  detectors must not allocate in `onCollisionDetected` (count calls in an `Int` field instead of copying the list).
  Measure one 16 ms tick per run with `measureAllocatedBytesPerRun { tick() }`. Take the first measurement at
  execution time; if it is at most 1 024 B per tick, assert it with that budget. If it is larger, do not invent a
  budget: mark the test `@Ignore("measured <n> B/tick — needs a budget decision")` and report the number.

The narrow phase's scratch buffers are file-level globals (`CollisionMaskExtensions.kt`), so the tests must not
call collision functions from several threads at once. JUnit runs the tests of one class sequentially, and nothing
here starts threads.

## Tests

This plan is the tests. Run `./gradlew :plugins:collision:desktopTest`, then `./gradlew :plugins:collision:build`. A
disagreement between the grid and brute force, or an asymmetric pair, is a real bug. Do not fix it here: `@Ignore`
the case with the seed and the pair, and report it for a follow-up plan.

## Manual check

None.
