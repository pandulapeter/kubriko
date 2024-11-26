package com.pandulapeter.kubriko.actor.body

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
}