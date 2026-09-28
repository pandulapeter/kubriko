/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun lineIntersect(line1Start: SceneOffset, line1End: SceneOffset, line2Start: SceneOffset, line2End: SceneOffset): SceneOffset? {
    val x1 = line1Start.x
    val y1 = line1Start.y
    val x2 = line1End.x
    val y2 = line1End.y
    val x3 = line2Start.x
    val y3 = line2Start.y
    val x4 = line2End.x
    val y4 = line2End.y
    val denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if (denominator.raw == 0f) {
        return null
    }
    val x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator
    val y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator
    if (!isPointOnLine(
            lineStart = line1Start,
            lineEnd = line1End,
            point = SceneOffset(x, y),
        ) || !isPointOnLine(
            lineStart = line2Start,
            lineEnd = line2End,
            point = SceneOffset(x, y),
        )
    ) {
        return null
    }
    return SceneOffset(x, y)
}


internal fun isPointOnLine(lineStart: SceneOffset, lineEnd: SceneOffset, point: SceneOffset): Boolean {
    val startX = lineStart.x.raw
    val startY = lineStart.y.raw
    val endX = lineEnd.x.raw
    val endY = lineEnd.y.raw
    val pointX = point.x.raw
    val pointY = point.y.raw
    val deltaX = endX - startX
    val deltaY = endY - startY
    val cross = deltaX * (pointY - startY) - deltaY * (pointX - startX)
    if (cross * cross >= ON_LINE_TOLERANCE_SQUARED * (deltaX * deltaX + deltaY * deltaY)) {
        return false
    }
    return if (abs(deltaX) >= abs(deltaY)) {
        pointX >= min(startX, endX) && pointX <= max(startX, endX)
    } else {
        pointY >= min(startY, endY) && pointY <= max(startY, endY)
    }
}

private const val ON_LINE_TOLERANCE_SQUARED = 0.0001f // The point may be up to 0.01 scene units off the line.
