/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

interface MiniMapMarker {

    fun DrawScope.draw(
        centerX: Float,
        centerY: Float,
        halfWidth: Float,
        halfHeight: Float,
        rotation: Float,
        color: Color,
        stroke: Stroke
    )

    object Circle : MiniMapMarker {
        override fun DrawScope.draw(
            centerX: Float,
            centerY: Float,
            halfWidth: Float,
            halfHeight: Float,
            rotation: Float,
            color: Color,
            stroke: Stroke
        ) {
            val radius = halfWidth // The sampler already limits this if needed
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(centerX, centerY),
            )
            drawCircle(
                color = Color.Black,
                radius = radius,
                center = Offset(centerX, centerY),
                style = stroke,
            )
        }
    }

    object Rectangle : MiniMapMarker {
        override fun DrawScope.draw(
            centerX: Float,
            centerY: Float,
            halfWidth: Float,
            halfHeight: Float,
            rotation: Float,
            color: Color,
            stroke: Stroke
        ) {
            val rectSize = Size(halfWidth * 2f, halfHeight * 2f)
            val topLeft = Offset(centerX - halfWidth, centerY - halfHeight)
            if (rectSize.width <= stroke.width * 2f || rectSize.height <= stroke.width * 2f) {
                drawRect(
                    color = Color.Black,
                    topLeft = topLeft,
                    size = rectSize,
                )
            } else {
                rotate(
                    degrees = rotation,
                    pivot = Offset(centerX, centerY),
                ) {
                    drawRect(
                        color = color,
                        topLeft = topLeft,
                        size = rectSize,
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = topLeft,
                        size = rectSize,
                        style = stroke,
                    )
                }
            }
        }
    }
}
