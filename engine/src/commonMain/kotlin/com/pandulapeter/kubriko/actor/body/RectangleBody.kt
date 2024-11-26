package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.cos
import kotlin.math.sin

// TODO: Pivot and scale are implementation details. `position` and `size` should return adjusted values
class RectangleBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialPivot: SceneOffset = initialSize.center,
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
) {
    var size = initialSize
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    var pivot = initialPivot
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    var scale = initialScale
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    var rotation = initialRotation
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }

    override fun createAxisAlignedBoundingBox(): AxisAlignedBoundingBox {
        val corners = listOf(
            transformPoint(position - pivot),
            transformPoint(position - pivot + SceneOffset(size.width, 0f.scenePixel)),
            transformPoint(position - pivot + SceneOffset(0f.scenePixel, size.height)),
            transformPoint(position - pivot + SceneOffset(size.width, size.height))
        )

        return AxisAlignedBoundingBox(
            min = SceneOffset(corners.minOf { it.x }, corners.minOf { it.y }),
            max = SceneOffset(corners.maxOf { it.x }, corners.maxOf { it.y })
        )
    }

    private fun transformPoint(point: SceneOffset): SceneOffset {
        // Translate to pivot
        val translatedX = point.x
        val translatedY = point.y

        // Apply scaling relative to pivot
        val scaledX = translatedX * scale.horizontal
        val scaledY = translatedY * scale.vertical

        // Apply rotation (assuming rotation is in radians)
        val rotatedX = scaledX * cos(rotation.normalized) - scaledY * sin(rotation.normalized)
        val rotatedY = scaledX * sin(rotation.normalized) + scaledY * cos(rotation.normalized)

        // Translate back to the original position relative to the pivot
        return SceneOffset(rotatedX + pivot.x, rotatedY + pivot.y)
    }
}