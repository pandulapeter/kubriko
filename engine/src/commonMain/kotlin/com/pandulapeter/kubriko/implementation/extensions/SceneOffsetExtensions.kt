package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import kotlin.math.atan2
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

fun SceneOffset.distanceTo(other: SceneOffset): ScenePixel = (x.raw - other.x.raw).let { dx ->
    (y.raw - other.y.raw).let { dy ->
        sqrt(dx * dx + dy * dy).scenePixel
    }
}

fun SceneOffset.length(): ScenePixel = distanceTo(SceneOffset.Zero)

fun SceneOffset.dot(v1: SceneOffset): ScenePixel = (v1.x.raw * x.raw + v1.y.raw * y.raw).scenePixel

fun SceneOffset.normal(): SceneOffset = SceneOffset(-y, x)

fun SceneOffset.normalize(): SceneOffset = length().let { length ->
    (if (length == ScenePixel.Zero) ScenePixel.Unit else length).let { d ->
        SceneOffset(
            x = x / d,
            y = y / d,
        )
    }
}

fun SceneOffset.cross(v1: SceneOffset): ScenePixel = x * v1.y - y * v1.x

fun SceneOffset.cross(a: Float): SceneOffset = normal().scalar(a)

fun SceneOffset.scalar(a: Float): SceneOffset = SceneOffset(x * a, y * a)

fun SceneOffset.scalar(a: ScenePixel): SceneOffset = SceneOffset(x * a, y * a)