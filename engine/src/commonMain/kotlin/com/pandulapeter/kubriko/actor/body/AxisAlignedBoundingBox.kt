package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

data class AxisAlignedBoundingBox(
    val min: SceneOffset,
    val max: SceneOffset
) {
    val size = SceneSize(
        width = max.x - min.x,
        height = max.y - min.y,
    )
    val left = min.x
    val top = min.y
    val right = max.x
    val bottom = max.y
}