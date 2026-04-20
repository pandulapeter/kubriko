/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Returns the angle in radians from this offset towards the given [offset].
 */
fun SceneOffset.angleTowards(offset: SceneOffset): AngleRadians = atan2((offset.y - y).raw, (offset.x - x).raw).rad

/**
 * Returns a new [SceneOffset] constrained within the box defined by [topLeft] and [bottomRight].
 */
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

/**
 * Returns a new [SceneOffset] clamped between the specified [min] and [max] offsets.
 */
fun SceneOffset.clamp(
    min: SceneOffset? = null,
    max: SceneOffset? = null,
) = SceneOffset(
    x = max((min ?: this).x.raw, min((max ?: this).x.raw, x.raw)).sceneUnit,
    y = max((min ?: this).y.raw, min((max ?: this).y.raw, y.raw)).sceneUnit
)

/**
 * Returns a new [SceneOffset] clamped within the area defined by [topLeft] and [bottomRight].
 */
fun SceneOffset.clampWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
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

/**
 * Calculates the distance between this offset and [other].
 */
fun SceneOffset.distanceTo(other: SceneOffset): SceneUnit = (x.raw - other.x.raw).let { dx ->
    (y.raw - other.y.raw).let { dy ->
        sqrt(dx * dx + dy * dy).sceneUnit
    }
}

/**
 * Returns the direction in radians towards [other].
 */
fun SceneOffset.directionTowards(other: SceneOffset): AngleRadians = atan2(other.y.raw - y.raw, other.x.raw - x.raw).rad

/**
 * Returns the Euclidean length of this offset.
 */
fun SceneOffset.length(): SceneUnit = distanceTo(SceneOffset.Zero)

/**
 * Calculates the dot product between this offset and [v1].
 */
fun SceneOffset.dot(v1: SceneOffset): SceneUnit = (v1.x.raw * x.raw + v1.y.raw * y.raw).sceneUnit

/**
 * Returns a new [SceneOffset] perpendicular to this one.
 */
fun SceneOffset.normal(): SceneOffset = SceneOffset(-y, x)

/**
 * Returns a normalized (unit length) version of this [SceneOffset].
 */
fun SceneOffset.normalized(): SceneOffset = length().let { length ->
    (if (length == SceneUnit.Zero) SceneUnit.Unit else length).let { d ->
        SceneOffset(
            x = x / d,
            y = y / d,
        )
    }
}

/**
 * Calculates the 2D cross product with [v1].
 */
fun SceneOffset.cross(v1: SceneOffset): SceneUnit = x * v1.y - y * v1.x

/**
 * Calculates the 2D cross product with a scalar [a].
 */
fun SceneOffset.cross(a: Float): SceneOffset = normal().scalar(a)

/**
 * Multiplies this offset by a scalar [a].
 */
fun SceneOffset.scalar(a: Float): SceneOffset = SceneOffset(x * a, y * a)

/**
 * Multiplies this offset by a scalar [a] of type [SceneUnit].
 */
fun SceneOffset.scalar(a: SceneUnit): SceneOffset = SceneOffset(x * a, y * a)

/**
 * Checks if this offset is within the specified [axisAlignedBoundingBox].
 */
fun SceneOffset.isInside(
    axisAlignedBoundingBox: AxisAlignedBoundingBox
): Boolean = x.raw in axisAlignedBoundingBox.left..axisAlignedBoundingBox.right && y.raw in axisAlignedBoundingBox.top..axisAlignedBoundingBox.bottom

/**
 * Returns the geometric center of a list of offsets.
 */
val List<SceneOffset>.center
    get() = when {
        isEmpty() -> SceneOffset.Zero
        size == 1 -> first()
        else -> SceneOffset(
            x = (maxOf { it.x } + minOf { it.x }) / 2,
            y = (maxOf { it.y } + minOf { it.y }) / 2
        )
    }

/**
 * Converts this [SceneOffset] to a screen [Offset].
 */
fun SceneOffset.toOffset(viewportManager: ViewportManager): Offset = toOffset(
    viewportScaleFactor = viewportManager.scaleFactor.value,
)

/**
 * Converts this [SceneOffset] to a screen [Offset] using the given [viewportScaleFactor].
 */
fun SceneOffset.toOffset(
    viewportScaleFactor: Scale,
): Offset = Offset(
    x = x.raw * viewportScaleFactor.horizontal,
    y = y.raw * viewportScaleFactor.vertical,
)

/**
 * Rotates this offset around a [center] point by [radians].
 */
fun SceneOffset.rotateAround(center: SceneOffset, radians: AngleRadians): SceneOffset {
    val cosA = radians.cos
    val sinA = radians.sin
    val dx = x - center.x
    val dy = y - center.y
    return SceneOffset(
        x = center.x + dx * cosA - dy * sinA,
        y = center.y + dx * sinA + dy * cosA
    )
}

/**
 * Linearly interpolates between [start] and [stop] offsets.
 */
fun lerp(
    start: SceneOffset,
    stop: SceneOffset,
    fraction: Float,
) = SceneOffset(
    x = lerp(start.x.raw, stop.x.raw, fraction).sceneUnit,
    y = lerp(start.y.raw, stop.y.raw, fraction).sceneUnit,
)