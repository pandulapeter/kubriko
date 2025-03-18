/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun SceneOffset.toVec2() = Vec2(
    x = x,
    y = y,
)

internal fun isPointInside(body: PhysicsBody, startPoint: SceneOffset) = body.shape.isPointInside(startPoint)

internal fun lineIntersect(line1Start: SceneOffset, line1End: SceneOffset, line2Start: SceneOffset, line2End: SceneOffset): SceneOffset? {
    val x1 = line1Start.x
    val y1 = line1Start.y
    val x2 = line1End.x
    val y2 = line1End.y
    val x3 = line2Start.x
    val y3 = line2Start.y
    val x4 = line2End.x
    val y4 = line2End.y

    val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if (denom == SceneUnit.Zero) {
        return null
    }

    val x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom
    val y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom

    if (!isPointOnLine(line1Start, line1End, SceneOffset(x, y)) || !isPointOnLine(
            line2Start, line2End,
            SceneOffset(x, y)
        )
    ) {
        return null
    }

    return SceneOffset(x, y)
}


internal fun isPointOnLine(lineStart: SceneOffset, lineEnd: SceneOffset, point: SceneOffset): Boolean {
    if (lineStart.x == lineEnd.x) {
        return point.x == lineStart.x && ((point.y >= lineStart.y && point.y <= lineEnd.y) || (point.y <= lineStart.y && point.y >= lineEnd.y))
    } else if (lineStart.y == lineEnd.y) {
        return point.y == lineStart.y && ((point.x >= lineStart.x && point.x <= lineEnd.x) || (point.x <= lineStart.x && point.x >= lineEnd.x))
    }

    val a = (lineEnd.y - lineStart.y) / (lineEnd.x - lineStart.x)
    val b = lineStart.y - a * lineStart.x
    if (abs((point.y - (a * point.x + b)).raw) < 0.01) {
        val smallerX = min(lineStart.x.raw, lineEnd.x.raw).sceneUnit
        val smallerY = min(lineStart.y.raw, lineEnd.y.raw).sceneUnit
        val biggerX = max(lineStart.x.raw, lineEnd.x.raw).sceneUnit
        val biggerY = max(lineStart.y.raw, lineEnd.y.raw).sceneUnit

        return point.x.raw in smallerX..biggerX && point.y.raw in smallerY..biggerY
    }
    return false
}