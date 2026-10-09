# Share the polygon path drawing of `DynamicPolygon` and `StaticPolygon`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/PolygonShapes.kt` (created by D19), `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicPolygon.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/StaticPolygon.kt`

## Problem
`DynamicPolygon.draw()` (`DynamicPolygon.kt:32-50`) and `StaticPolygon.draw()` (`StaticPolygon.kt:56-74`) are the same code apart from the fill colour:
```kotlin
val path = Path().apply {
    moveTo(collisionMask.vertices[0].x.raw + body.pivot.x.raw, collisionMask.vertices[0].y.raw + body.pivot.y.raw)
    for (i in 1 until collisionMask.vertices.size) {
        lineTo(collisionMask.vertices[i].x.raw + body.pivot.x.raw, collisionMask.vertices[i].y.raw + body.pivot.y.raw)
    }
    close()
}
drawPath(path = path, color = color /* or Color.DarkGray */, style = Fill)
drawPath(path = path, color = Color.Black, style = Stroke(width = 2f))
```

## Fix
Add to `PolygonShapes.kt` `internal fun DrawScope.drawPolygon(vertices: List<SceneOffset>, pivot: SceneOffset, fillColor: Color)` holding that body verbatim (with `vertices`/`pivot`/`fillColor` substituted). The overrides become `override fun DrawScope.draw() = drawPolygon(collisionMask.vertices, body.pivot, color)` and `… Color.DarkGray)`. Trim imports (`Path`, `Fill`, `Stroke`). Keep the existing per-draw `Path()`/`Stroke` allocation exactly as it is — changing it is out of scope for a move.

## Behaviour
Identical draw calls in the same order.

## Public API
None.

## Tests
The existing ones (none; drawing cannot be unit-tested without Skia).

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop`

## Manual check
Open Physics: static polygons are dark grey, spawned polygons pastel, both with a black 2 px outline.
