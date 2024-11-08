package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.atan2

fun SceneOffset.angleTowards(offset: SceneOffset): AngleRadians = atan2((offset.y - y).raw, (offset.x - x).raw).rad

fun SceneOffset.constrainedWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
    var offset = this
    if (offset.x < topLeft.x) {
        offset = SceneOffset(topLeft.x, offset.y)
    }
    if (offset.x > bottomRight.x) {
        offset = SceneOffset(bottomRight.x, offset.y)
    }
    if (offset.y < topLeft.y) {
        offset = SceneOffset(offset.x, topLeft.y)
    }
    if (offset.y > bottomRight.y) {
        offset = SceneOffset(offset.x, bottomRight.y)
    }
    return offset
}

fun SceneOffset.wrapWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
    var offset = this
    if (offset.x < topLeft.x) {
        offset = SceneOffset(bottomRight.x, offset.y)
    }
    if (offset.x > bottomRight.x) {
        offset = SceneOffset(topLeft.x, offset.y)
    }
    if (offset.y < topLeft.y) {
        offset = SceneOffset(offset.x, bottomRight.y)
    }
    if (offset.y > bottomRight.y) {
        offset = SceneOffset(offset.x, topLeft.y)
    }
    return offset
}