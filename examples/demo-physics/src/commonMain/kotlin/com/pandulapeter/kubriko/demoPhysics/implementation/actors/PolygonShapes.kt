/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

internal fun randomPolygonVertices(): List<SceneOffset> = (3..10).random().let { sideCount ->
    (0..sideCount).map { sideIndex ->
        val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
        SceneOffset(
            x = (30..120).random().sceneUnit * angle.cos,
            y = (30..120).random().sceneUnit * angle.sin,
        )
    }
}

internal fun DrawScope.drawPolygon(vertices: List<SceneOffset>, pivot: SceneOffset, fillColor: Color) {
    val path = Path().apply {
        moveTo(vertices[0].x.raw + pivot.x.raw, vertices[0].y.raw + pivot.y.raw)
        for (i in 1 until vertices.size) {
            lineTo(vertices[i].x.raw + pivot.x.raw, vertices[i].y.raw + pivot.y.raw)
        }
        close()
    }
    drawPath(
        path = path,
        color = fillColor,
        style = Fill,
    )
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(width = 2f),
    )
}
