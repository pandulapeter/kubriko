package com.pandulapeter.kubriko.physics.implementation.physics.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Math {

    fun lineIntersect(line1Start: Vec2, line1End: Vec2, line2Start: Vec2, line2End: Vec2): Vec2? {
        val x1 = line1Start.x
        val y1 = line1Start.y
        val x2 = line1End.x
        val y2 = line1End.y
        val x3 = line2Start.x
        val y3 = line2Start.y
        val x4 = line2End.x
        val y4 = line2End.y

        val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (denom == 0f) {
            return null
        }

        val x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom
        val y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom

        if (!pointIsOnLine(line1Start, line1End, Vec2(x, y)) || !pointIsOnLine(
                line2Start, line2End,
                Vec2(x, y)
            )
        ) {
            return null
        }

        return Vec2(x, y)
    }


    fun pointIsOnLine(lineStart: Vec2, lineEnd: Vec2, point: Vec2): Boolean {
        if (lineStart.x == lineEnd.x) {
            return point.x == lineStart.x && ((point.y >= lineStart.y && point.y <= lineEnd.y) || (point.y <= lineStart.y && point.y >= lineEnd.y))
        } else if (lineStart.y == lineEnd.y) {
            return point.y == lineStart.y && ((point.x >= lineStart.x && point.x <= lineEnd.x) || (point.x <= lineStart.x && point.x >= lineEnd.x))
        }

        val a = (lineEnd.y - lineStart.y) / (lineEnd.x - lineStart.x)
        val b = lineStart.y - a * lineStart.x
        if (abs(point.y - (a * point.x + b)) < 0.01) {
            val smallerX = min(lineStart.x, lineEnd.x)
            val smallerY = min(lineStart.y, lineEnd.y)
            val biggerX = max(lineStart.x, lineEnd.x)
            val biggerY = max(lineStart.y, lineEnd.y)

            return point.x in smallerX..biggerX && point.y in smallerY..biggerY
        }
        return false
    }
}