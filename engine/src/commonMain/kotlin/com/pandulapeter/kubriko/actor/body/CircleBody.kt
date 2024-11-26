package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

class CircleBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRadius: ScenePixel = ScenePixel.Zero,
    initialPivot: SceneOffset = SceneOffset(initialRadius, initialRadius),
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
), ComplexBody {
    var radius = initialRadius
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    override val size get() = SceneSize(radius * 2, radius * 2)
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
        // Convert pivot to an absolute scene position
        val absolutePivot = position// + pivot

        // Compute the scaled radius in both directions
        val scaledRadiusX = radius * scale.horizontal
        val scaledRadiusY = radius * scale.vertical

        // Compute the AABB based on the scaled radii
        return AxisAlignedBoundingBox(
            min = SceneOffset(
                x = absolutePivot.x - scaledRadiusX,
                y = absolutePivot.y - scaledRadiusY
            ),
            max = SceneOffset(
                x = absolutePivot.x + scaledRadiusX,
                y = absolutePivot.y + scaledRadiusY
            )
        )
    }
}