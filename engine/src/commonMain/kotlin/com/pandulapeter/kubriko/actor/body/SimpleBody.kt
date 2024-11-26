package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.types.SceneOffset

interface SimpleBody {

    val axisAlignedBoundingBox: AxisAlignedBoundingBox
    var position: SceneOffset
}