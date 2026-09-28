# Bound the scene editor grid by an integer line count and skip lines too dense to see

**Challenged:** sound

**Kind:** bug (hang) / performance  ·  **Severity:** medium  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/actors/GridOverlay.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/GridLines.kt` (new), `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/GridLinesTest.kt` (new)

## Problem

`GridOverlay.drawToViewport` (runs every frame) walks the visible range with a float accumulator (~57 for X, ~79 for Y):

```kotlin
var currentX = startX
var iterationX = 0
while (currentX <= viewportBottomRight.x.raw) {
    val alpha = if ((startXLineIndex + iterationX) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
    drawLine(...)
    currentX += gridCellSizeX
    iterationX++
}
```

The snap values are user-editable integers ≥ 1 (`MetadataRow.kt`, persisted in `UserPreferences`), and the editor zooms out to `MINIMUM_SCALE_FACTOR = 0.1f`.
1. **Cost.** With snap 1 at minimum zoom, a 2000 × 1200 px canvas spans 20000 × 12000 scene units — about 32 000 `drawLine` calls per frame for lines that are 0.1 px apart and indistinguishable.
2. **Hang.** Once the camera is beyond ±2²⁴ (≈16.7 M) scene units — reachable by holding an arrow key or locating an actor placed far out — `currentX + 1f == currentX` in `Float`, the loop never advances and the editor's UI thread freezes. `startX` is also computed through `Int` (`.toInt()`), which saturates for larger coordinates.

## Fix

1. New `helpers/GridLines.kt` (MPL header), pure and allocation-free:
   ```kotlin
   internal fun firstGridLineIndex(min: Float, cellSize: Float): Long = floor(min.toDouble() / cellSize).toLong()
   internal fun lastGridLineIndex(max: Float, cellSize: Float): Long = floor(max.toDouble() / cellSize).toLong()
   internal fun isGridLineVisible(cellSize: Float, scale: Float) = cellSize * scale >= MIN_GRID_LINE_SPACING_PX
   internal const val MIN_GRID_LINE_SPACING_PX = 4f
   internal const val MAX_GRID_LINES_PER_AXIS = 2000L
   ```
2. `GridOverlay`, per axis (X uses `viewportScaleFactor.horizontal`, Y `vertical`):
   - `first`/`last` from the helpers; `step = if (isGridLineVisible(cell, scale)) 1L else if (isGridLineVisible(cell * 10, scale)) 10L else 0L` (`0` = draw nothing on that axis); when `step == 10L`, round `first` up to a multiple of 10 (`Math.floorMod`-safe for negatives).
   - Skip the axis if `step == 0L` or `(last - first) / step > MAX_GRID_LINES_PER_AXIS`.
   - Loop `var index = first; while (index <= last) { val position = (index * cell.toDouble()).toFloat(); ...; index += step }`, alpha from `index % 10 == 0L` (major).
   Hoist `Color.Gray.copy(alpha = …)` into two companion constants. Remove the old per-axis comments that narrate the float loop.
3. No `CLAUDE.md` change (the grid is not documented there).

## Tests

`GridLinesTest` (desktopTest, pure):
- `firstGridLineIndex(-10.5f, 32f)` = -1, `lastGridLineIndex(100f, 32f)` = 3, `firstGridLineIndex(64f, 32f)` = 2;
- at 2²⁵ with cell 1: `lastGridLineIndex(33554500f, 1f) - firstGridLineIndex(33554432f, 1f)` is a small finite count (the loop bound is an integer, so it terminates);
- `isGridLineVisible(1f, 0.1f)` false, `isGridLineVisible(10f, 0.1f)` false (1 px), `isGridLineVisible(32f, 1f)` true, `isGridLineVisible(40f, 0.1f)` true;
- line count for a 20000-unit wide view with cell 1 at scale 0.1: minor and major lines both invisible → nothing is drawn.

Run `./gradlew :tools:scene-editor:desktopTest`.

## Manual check

Desktop scene editor:
1. Set Snap X and Snap Y to 1, zoom out fully: the editor stays responsive (no frame drop compared with snap 32), and the grid fades to majors, then disappears, as you zoom out.
2. Zoom in: minor lines return at the usual spacing, majors every 10 cells, aligned with (0, 0) as before (compare against a build without the change).
3. Hold an arrow key at maximum zoom-out for a long pan (or temporarily place an actor at x = 20 000 000 and Locate it): no freeze.
