/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.minus
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

internal class GridOverlay(private val viewportManager: ViewportManager) : Overlay, Unique {

    override fun DrawScope.drawToViewport() = viewportManager.scaleFactor.value.let { viewportScaleFactor ->
        withTransform(
            transformBlock = {
                viewportManager.cameraPosition.value.let { viewportCenter ->
                    transformViewport(
                        viewportCenter = viewportCenter,
                        shiftedViewportOffset = (size / 2f) - viewportCenter,
                        viewportScaleFactor = viewportScaleFactor,
                    )
                }
            },
            drawBlock = {
                val viewportTopLeft = viewportManager.topLeft.value
                val viewportBottomRight = viewportManager.bottomRight.value
                val strokeWidth = 2f / (viewportScaleFactor.horizontal + viewportScaleFactor.vertical)

                // Calculate the starting point for vertical lines and ensure alignment with (0,0)
                var startX = (viewportTopLeft.x / GRID_CELL_SIZE).raw.toInt() * GRID_CELL_SIZE
                if (startX > viewportTopLeft.x.raw) startX -= GRID_CELL_SIZE
                val startXLineIndex = (startX / GRID_CELL_SIZE).toInt()

                // Draw vertical grid lines
                var currentX = startX
                var iterationX = 0
                while (currentX <= viewportBottomRight.x.raw) {
                    val alpha = if ((startXLineIndex + iterationX) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
                    drawLine(
                        color = Color.Gray.copy(alpha = alpha),
                        start = Offset(currentX, viewportTopLeft.y.raw),
                        end = Offset(currentX, viewportBottomRight.y.raw),
                        strokeWidth = strokeWidth
                    )
                    currentX += GRID_CELL_SIZE
                    iterationX++
                }

                // Calculate the starting point for horizontal lines, aligning with (0,0)
                var startY = (viewportTopLeft.y / GRID_CELL_SIZE).raw.toInt() * GRID_CELL_SIZE
                if (startY > viewportTopLeft.y.raw) startY -= GRID_CELL_SIZE
                val startYLineIndex = (startY / GRID_CELL_SIZE).toInt()

                // Draw horizontal grid lines
                var currentY = startY
                var iterationY = 0
                while (currentY <= viewportBottomRight.y.raw) {
                    val alpha = if ((startYLineIndex + iterationY) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
                    drawLine(
                        color = Color.Gray.copy(alpha = alpha),
                        start = Offset(viewportTopLeft.x.raw, currentY),
                        end = Offset(viewportBottomRight.x.raw, currentY),
                        strokeWidth = strokeWidth
                    )
                    currentY += GRID_CELL_SIZE
                    iterationY++
                }
            },
        )
    }

    private fun DrawTransform.transformViewport(
        viewportCenter: SceneOffset,
        shiftedViewportOffset: SceneOffset,
        viewportScaleFactor: Scale,
    ) {
        translate(
            left = shiftedViewportOffset.x.raw,
            top = shiftedViewportOffset.y.raw,
        )
        scale(
            scaleX = viewportScaleFactor.horizontal,
            scaleY = viewportScaleFactor.vertical,
            pivot = viewportCenter.raw,
        )
    }

    companion object {
        private const val GRID_CELL_SIZE = 100f
        private const val ALPHA_MAJOR = 0.4f
        private const val ALPHA_MINOR = 0.2f
    }
}