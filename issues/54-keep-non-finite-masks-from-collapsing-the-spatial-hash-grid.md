# Keep masks with non-finite bounds from collapsing the spatial hash grid's cell size

**Challenged:** sound

**Kind:** bug (performance cliff)  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-collision`
**Files:** `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/SpatialHashGrid.kt`, `plugins/collision/src/desktopTest/kotlin/com/pandulapeter/kubriko/collision/SpatialHashGridNonFiniteTest.kt` (new), `plugins/collision/CLAUDE.md`

## Problem

`SpatialHashGrid.refreshCellSize` (public class, at 0008d027) sizes cells from the mean extent of all masks:

```kotlin
var extentSum = 0f
for (index in masks.indices) {
    val bounds = masks[index].axisAlignedBoundingBox
    extentSum += (bounds.maxXRaw - bounds.minXRaw) + (bounds.maxYRaw - bounds.minYRaw)
}
val meanExtent = extentSum / (masks.size * 2)
inverseCellSize = 1f / (meanExtent * CELLS_PER_MEAN_MASK_EXTENT).coerceAtLeast(MINIMUM_CELL_SIZE)
```

One mask with a `NaN` position, a `NaN` size, or an infinite size (a `BoxCollisionMask` with an infinite width ends up with `NaN` vertices and bounds after hull centering) makes `extentSum` `NaN` (or infinite). `NaN.coerceAtLeast(1f)` is still `NaN`, so `inverseCellSize` becomes `NaN` (or `0`), `cellCoordinate` returns `0` for every mask, and **every mask lands in the same cell**: each detector query walks the whole type. Measured in the live run: `CollisionManager` with 3000 boxes, 616 µs per tick → 65 ms per tick after adding one `NaN`-position or infinite-size box — a 100× cliff from a single bad actor, anywhere in the scene. Verified with a probe: 3000 boxes, `inverseCellSize` 0.025 → `NaN` after adding one box at `x = NaN` and after adding one box with an infinite width. Results stay correct (the exact bounds check filters), only the cost explodes.

## Fix

1. `refreshCellSize`: skip masks whose extent is not finite (`!sum.isFinite()` on the per-mask `width + height`), and divide by twice the number of masks actually counted. If none were counted, leave `inverseCellSize` as it is.
2. `rebuild`: route a mask with any non-finite bound (`!minXRaw.isFinite() || ...`) straight to `addOversized(maskIndex)` before computing cell coordinates. An oversized mask is offered to every query and filtered by the exact bounds check, which is correct for both cases: a `NaN` box overlaps nothing, a box with an infinite bound overlaps what it should. (Infinite bounds already reach the oversized list through the cell-count check today; the explicit route also covers `NaN`, which currently lands in cell `(0, 0)`.)
3. Both are per-mask float checks in loops that already read the bounds; nothing allocates.
4. Add one line to the Spatial Hash Grid section of `plugins/collision/CLAUDE.md`: masks with non-finite bounds are ignored when sizing cells and kept in the oversized list.

## Tests

`SpatialHashGridNonFiniteTest` in `desktopTest` (JVM reflection is used to read the private `inverseCellSize`):
- 1000 `BoxCollisionMask`s of 20×20 at seeded random positions in a 3000×3000 area, `rebuild()`, read `inverseCellSize`; add one box at `(NaN, 0)`, `rebuild()` → `inverseCellSize` is finite and equal to the value before. Same with a box of infinite width.
- Correctness: for a handful of query rectangles, the set returned by `findCandidates` equals a brute-force list of the masks whose bounds overlap the rectangle (using the same inclusive comparison as `SpatialHashGrid.isOverlapping`), with the `NaN` box never returned.

Run `./gradlew :plugins:collision:desktopTest`.

## Manual check

None — no Showcase scene produces a non-finite mask.
