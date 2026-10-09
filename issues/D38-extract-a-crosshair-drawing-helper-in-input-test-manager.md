# Extract one allocation-free crosshair-drawing helper in `InputTestManager.drawToViewport`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/managers/InputTestManager.kt`

## Problem
`examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/managers/InputTestManager.kt`'s `override fun DrawScope.drawToViewport()` draws the same crosshair twice: for the hover pointer (:82–118: black 4 f + white 2 f vertical and horizontal lines, a filled white circle of radius 20 f, a black `Stroke()` circle) and for each pressed pointer (:119–156: the same with `color = pointerId.toColor()` instead of white and radius 40 f).

## Fix
Add `private fun DrawScope.drawCrosshair(position: Offset, color: Color, radius: Float)` containing the four `drawLine` and two `drawCircle` calls in the original order (black outer line, `color` inner line, vertical then horizontal; filled `color` circle; black `Stroke()` circle). Call it as `drawCrosshair(pointerOffset, Color.White, 20f)` inside the hover `let`, and `drawCrosshair(pointerOffset, pointerId.toColor(), 40f)` inside the pressed `forEach`. No lambda, no new allocation (the existing `Stroke()` per call stays exactly as it is).

## Behaviour
Identical draw calls in identical order.

## Public API
None.

## Tests
None possible (drawing needs Skia).

## Verify
`./gradlew :examples:test-input:compileKotlinDesktop`

## Manual check
Input test: hovering shows a white crosshair circle (small), pressing shows a coloured larger one.
