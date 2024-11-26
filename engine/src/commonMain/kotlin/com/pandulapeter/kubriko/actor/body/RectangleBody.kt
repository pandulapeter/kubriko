package com.pandulapeter.kubriko.actor.body

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
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    override var pivot = initialPivot
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    override var scale = initialScale
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    override var rotation = initialRotation
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
}