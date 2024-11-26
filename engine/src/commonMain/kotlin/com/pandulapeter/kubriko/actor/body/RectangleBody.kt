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
        // Compute all four corners of the transformed rectangle
        val corners = listOf(
            transformPoint(position), // Top-left
            transformPoint(position + SceneOffset(size.width, 0f.scenePixel)), // Top-right
            transformPoint(position + SceneOffset(0f.scenePixel, size.height)), // Bottom-left
            transformPoint(position + SceneOffset(size.width, size.height)) // Bottom-right
        )

        return AxisAlignedBoundingBox(
            min = SceneOffset(corners.minOf { it.x }, corners.minOf { it.y }),
            max = SceneOffset(corners.maxOf { it.x }, corners.maxOf { it.y })
        )
    }

    private fun transformPoint(point: SceneOffset): SceneOffset {
        // Convert pivot to an absolute scene position
        val absolutePivot = position + pivot

        // Translate the point relative to the pivot
        val translatedX = point.x - absolutePivot.x
        val translatedY = point.y - absolutePivot.y

        // Apply scaling relative to the pivot
        val scaledX = translatedX * scale.horizontal
        val scaledY = translatedY * scale.vertical

        // Apply rotation (assuming rotation is in radians)
        val rotatedX = scaledX * cos(rotation.normalized) - scaledY * sin(rotation.normalized)
        val rotatedY = scaledX * sin(rotation.normalized) + scaledY * cos(rotation.normalized)

        // Translate back to the scene position
        return SceneOffset(rotatedX + absolutePivot.x, rotatedY + absolutePivot.y)
    }
}