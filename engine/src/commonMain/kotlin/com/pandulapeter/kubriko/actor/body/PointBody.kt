package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.types.SceneOffset

open class PointBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
) : Body {
    override var position = initialPosition
        set(value) {
            field = value
            axisAlignedBoundingBox = createAxisAlignedBoundingBox()
        }
    private var _axisAlignedBoundingBox: AxisAlignedBoundingBox? = null
    override var axisAlignedBoundingBox: AxisAlignedBoundingBox
        get() = _axisAlignedBoundingBox ?: createAxisAlignedBoundingBox().also { _axisAlignedBoundingBox = it }
        protected set(value) {
            _axisAlignedBoundingBox = value
        }

    protected open fun createAxisAlignedBoundingBox() = AxisAlignedBoundingBox(
        min = position,
        max = position,
    )

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = drawCircle(
        color = color,
        radius = stroke.width,
        style = Fill,
    )
}