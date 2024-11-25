package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

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

fun SceneOffset.distanceTo(other: SceneOffset): ScenePixel = sqrt((x.raw - other.x.raw).pow(2) + (y.raw - other.y.raw).pow(2)).scenePixel