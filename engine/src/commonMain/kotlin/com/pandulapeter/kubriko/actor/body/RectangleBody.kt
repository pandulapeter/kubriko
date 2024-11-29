package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.cos
import kotlin.math.sin

class RectangleBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialPivot: SceneOffset = initialSize.center,
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
), ComplexBody {
    override var size = initialSize
        set(value) {
            field = value
            isAxisAlignedBoundingBoxDirty = true
        }
    override var pivot = initialPivot
        set(value) {
            field = value
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

    private val corners = mutableListOf<SceneOffset>()

    override fun createAxisAlignedBoundingBox(): AxisAlignedBoundingBox {
        corners.clear()
        corners.add(transformPoint(position))
        corners.add(transformPoint(position + SceneOffset(size.width, 0f.scenePixel)))
        corners.add(transformPoint(position + SceneOffset(0f.scenePixel, size.height)))
        corners.add(transformPoint(position + SceneOffset(size.width, size.height)))
        return AxisAlignedBoundingBox(
            min = SceneOffset(corners.minOf { it.x }, corners.minOf { it.y }) - pivot,
            max = SceneOffset(corners.maxOf { it.x }, corners.maxOf { it.y }) - pivot,
        )
    }

    private fun transformPoint(point: SceneOffset): SceneOffset {
        val absolutePivot = position + pivot
        val scaled = (point - absolutePivot) * scale
        val rotated = if (rotation == AngleRadians.Zero) scaled else SceneOffset(
            x = scaled.x * cos(rotation.normalized) - scaled.y * sin(rotation.normalized),
            y = scaled.x * sin(rotation.normalized) + scaled.y * cos(rotation.normalized)
        )
        return rotated + absolutePivot
    }

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = this@RectangleBody.size.raw.let { size ->
        drawRect(
            color = color,
            size = size,
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset.Zero,
            end = Offset(size.width, size.height),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f),
            strokeWidth = stroke.width,
        )
    }
}