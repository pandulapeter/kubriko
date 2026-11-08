/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import com.pandulapeter.kubriko.helpers.TriangleBatch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val OUTLINE_ARGB = 0xFF000000.toInt()
private const val CIRCLE_SEGMENTS = 48

/**
 * A filled box with a hairline outline, as the plain actors draw it with `drawRect` and a default `Stroke`: one screen
 * pixel wide and kept inside the box, since the plain path clips it to the body.
 */
internal fun TriangleBatch.addOutlinedBox(width: Float, height: Float, fillArgb: Int, pixelsPerUnit: Float) {
    addQuad(0f, 0f, width, 0f, width, height, 0f, height, fillArgb)
    val halfWidth = 0.5f / pixelsPerUnit
    addLine(halfWidth, halfWidth, width - halfWidth, halfWidth, halfWidth, OUTLINE_ARGB, isAntiAlias = true, pixelsPerUnit = pixelsPerUnit)
    addLine(width - halfWidth, halfWidth, width - halfWidth, height - halfWidth, halfWidth, OUTLINE_ARGB, isAntiAlias = true, pixelsPerUnit = pixelsPerUnit)
    addLine(width - halfWidth, height - halfWidth, halfWidth, height - halfWidth, halfWidth, OUTLINE_ARGB, isAntiAlias = true, pixelsPerUnit = pixelsPerUnit)
    addLine(halfWidth, height - halfWidth, halfWidth, halfWidth, halfWidth, OUTLINE_ARGB, isAntiAlias = true, pixelsPerUnit = pixelsPerUnit)
}

/** A filled circle with a hairline outline, as `drawCircle` and a default `Stroke` draw one. */
internal fun TriangleBatch.addOutlinedCircle(centerX: Float, centerY: Float, radius: Float, fillArgb: Int, pixelsPerUnit: Float) {
    if (radius <= 0f) return
    val center = addVertex(centerX, centerY, fillArgb)
    val first = addVertex(centerX + radius, centerY, fillArgb)
    var previous = first
    for (i in 1..CIRCLE_SEGMENTS) {
        val current = if (i == CIRCLE_SEGMENTS) first else {
            val angle = 2.0 * PI * i / CIRCLE_SEGMENTS
            addVertex(centerX + radius * cos(angle).toFloat(), centerY + radius * sin(angle).toFloat(), fillArgb)
        }
        addTriangle(center, previous, current)
        previous = current
    }
    val halfWidth = 0.5f / pixelsPerUnit
    var previousX = centerX + radius
    var previousY = centerY
    for (i in 1..CIRCLE_SEGMENTS) {
        val angle = 2.0 * PI * i / CIRCLE_SEGMENTS
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        addLine(previousX, previousY, x, y, halfWidth, OUTLINE_ARGB, isAntiAlias = true, pixelsPerUnit = pixelsPerUnit)
        previousX = x
        previousY = y
    }
}
