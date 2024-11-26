package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.types.SceneOffset

open class PointBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
) : SimpleBody {
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
}