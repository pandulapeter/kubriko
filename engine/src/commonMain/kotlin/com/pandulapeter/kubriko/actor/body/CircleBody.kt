package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.implementation.extensions.bottomRight
import com.pandulapeter.kubriko.implementation.extensions.clamp
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class CircleBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRadius: ScenePixel = ScenePixel.Zero,
    initialPivot: SceneOffset = SceneOffset(initialRadius, initialRadius),
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
), ComplexBody {
    var radius = initialRadius.clamp(min = ScenePixel.Zero)
        set(value) {
            field = value.clamp(min = ScenePixel.Zero)
            pivot = pivot.clamp(min = SceneOffset.Zero, max = size.bottomRight)
            isAxisAlignedBoundingBoxDirty = true
        }
    override val size get() = SceneSize(radius * 2, radius * 2)
    override var pivot = initialPivot.clamp(SceneOffset.Zero, size.bottomRight)
        set(value) {
            field = value.clamp(SceneOffset.Zero, size.bottomRight)
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
            if (scale.horizontal != scale.vertical) {
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    // TODO: Pivot should be taken into consideration
    override fun createAxisAlignedBoundingBox(): AxisAlignedBoundingBox {
        if (scale.horizontal == scale.vertical) {
            val scaledRadius = radius * scale.horizontal
            return AxisAlignedBoundingBox(
                min = SceneOffset(
                    x = position.x - scaledRadius,
                    y = position.y - scaledRadius
                ),
                max = SceneOffset(
                    x = position.x + scaledRadius,
                    y = position.y + scaledRadius
                )
            )
        } else {
            val scaledRadius = SceneOffset(
                x = radius * scale.horizontal,
                y = radius * scale.vertical,
            )
            val cosTheta = cos(rotation.normalized)
            val sinTheta = sin(rotation.normalized)
            val rotatedRadius = SceneOffset(
                x = abs(scaledRadius.x.raw * cosTheta).scenePixel + abs(scaledRadius.y.raw * sinTheta).scenePixel,
                y = abs(scaledRadius.x.raw * sinTheta).scenePixel + abs(scaledRadius.y.raw * cosTheta).scenePixel,
            )

            return AxisAlignedBoundingBox(
                min = position - rotatedRadius,
                max = position + rotatedRadius
            )
        }
    }

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = this@CircleBody.size.raw.let { size ->
        drawCircle(
            color = color,
            radius = radius.raw,
            center = size.center,
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