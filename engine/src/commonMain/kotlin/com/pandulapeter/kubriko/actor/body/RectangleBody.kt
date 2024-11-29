package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.implementation.extensions.bottomRight
import com.pandulapeter.kubriko.implementation.extensions.clamp
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
            pivot = pivot.clamp(min = SceneOffset.Zero, max = value.bottomRight)
            isAxisAlignedBoundingBoxDirty = true
        }
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

    override fun createAxisAlignedBoundingBox() = arrayOf(
        transformPoint(position),
        transformPoint(position + SceneOffset(size.width, 0f.scenePixel)),
        transformPoint(position + SceneOffset(0f.scenePixel, size.height)),
        transformPoint(position + SceneOffset(size.width, size.height)),
    ).let { corners ->
        AxisAlignedBoundingBox(
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