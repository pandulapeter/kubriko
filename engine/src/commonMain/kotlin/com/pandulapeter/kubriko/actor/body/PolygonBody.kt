package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.implementation.extensions.bottomRight
import com.pandulapeter.kubriko.implementation.extensions.center
import com.pandulapeter.kubriko.implementation.extensions.clamp
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

// TODO: Merge with RectangleBody
class PolygonBody(
    val vertices: List<SceneOffset> = emptyList(),
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialPivot: SceneOffset = vertices.center,
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
), ComplexBody {
    override val size = SceneSize(
        width = vertices.maxOf { it.x } - vertices.minOf { it.x },
        height = vertices.maxOf { it.y } - vertices.minOf { it.y },
    )
    override var pivot = initialPivot.clamp(min = SceneOffset.Zero, max = size.bottomRight)
        set(value) {
            field = value.clamp(min = SceneOffset.Zero, max = size.bottomRight)
            isAxisAlignedBoundingBoxDirty = true
        }
    override var scale = initialScale
        set(value) {
            field = value
            isAxisAlignedBoundingBoxDirty = true
        }
    override var rotation = initialRotation
        set(value) {
            field = value
            isAxisAlignedBoundingBoxDirty = true
        }

    // TODO
    override fun createAxisAlignedBoundingBox() = arrayOf(
        transformPoint(SceneOffset.Zero),
        transformPoint(SceneOffset.Right * size.width),
        transformPoint(SceneOffset.Bottom * size.height),
        transformPoint(size.bottomRight),
    ).let { corners ->
        AxisAlignedBoundingBox(
            min = SceneOffset(corners.minOf { it.x }, corners.minOf { it.y }) - pivot + position,
            max = SceneOffset(corners.maxOf { it.x }, corners.maxOf { it.y }) - pivot + position,
        )
    }

    private fun transformPoint(point: SceneOffset): SceneOffset {
        val scaled = (point - pivot) * scale
        val rotated = if (rotation == AngleRadians.Zero) scaled else SceneOffset(
            x = scaled.x * rotation.cos - scaled.y * rotation.sin,
            y = scaled.x * rotation.sin + scaled.y * rotation.cos,
        )
        return rotated + pivot
    }

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = this@PolygonBody.size.raw.let { size ->
        val path = Path().apply {
            moveTo(vertices[0].x.raw + pivot.x.raw, vertices[0].y.raw + pivot.y.raw)
            for (i in 1 until vertices.size) {
                lineTo(vertices[i].x.raw + pivot.x.raw, vertices[i].y.raw + pivot.y.raw)
            }
            close()
        }
        drawPath(path = path, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(pivot.x.raw, 0f),
            end = Offset(pivot.x.raw, size.height),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = color,
            start = Offset(0f, pivot.y.raw),
            end = Offset(size.width, pivot.y.raw),
            strokeWidth = stroke.width,
        )
    }
}