/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.helpers.extensions.minus
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.MAX_GRID_LINES_PER_AXIS
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.UserPreferences
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.alignGridLineIndex
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.firstGridLineIndex
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.gridLineStep
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.lastGridLineIndex

internal class GridOverlay(
    private val viewportManager: ViewportManager,
    private val userPreferences: UserPreferences,
) : Overlay, Unique {

    override fun DrawScope.drawToViewport() = viewportManager.scaleFactor.value.let { viewportScaleFactor ->
        val gridCellSizeX = userPreferences.snapX.value.toFloat()
        val gridCellSizeY = userPreferences.snapY.value.toFloat()
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

                drawGridLines(
                    isVertical = true,
                    cellSize = gridCellSizeX,
                    scale = viewportScaleFactor.horizontal,
                    min = viewportTopLeft.x.raw,
                    max = viewportBottomRight.x.raw,
                    crossMin = viewportTopLeft.y.raw,
                    crossMax = viewportBottomRight.y.raw,
                    strokeWidth = strokeWidth,
                )
                drawGridLines(
                    isVertical = false,
                    cellSize = gridCellSizeY,
                    scale = viewportScaleFactor.vertical,
                    min = viewportTopLeft.y.raw,
                    max = viewportBottomRight.y.raw,
                    crossMin = viewportTopLeft.x.raw,
                    crossMax = viewportBottomRight.x.raw,
                    strokeWidth = strokeWidth,
                )
            },
        )
    }

    private fun DrawScope.drawGridLines(
        isVertical: Boolean,
        cellSize: Float,
        scale: Float,
        min: Float,
        max: Float,
        crossMin: Float,
        crossMax: Float,
        strokeWidth: Float,
    ) {
        if (cellSize <= 0f) return
        val step = gridLineStep(cellSize, scale)
        if (step == 0L) return
        val first = alignGridLineIndex(firstGridLineIndex(min, cellSize), step)
        val last = lastGridLineIndex(max, cellSize)
        if ((last - first) / step > MAX_GRID_LINES_PER_AXIS) return
        var index = first
        while (index <= last) {
            val position = (index * cellSize.toDouble()).toFloat()
            drawLine(
                color = if (index % 10 == 0L) COLOR_MAJOR else COLOR_MINOR,
                start = if (isVertical) Offset(position, crossMin) else Offset(crossMin, position),
                end = if (isVertical) Offset(position, crossMax) else Offset(crossMax, position),
                strokeWidth = strokeWidth,
            )
            index += step
        }
    }

    companion object {
        private val COLOR_MAJOR = Color.Gray.copy(alpha = 0.4f)
        private val COLOR_MINOR = Color.Gray.copy(alpha = 0.2f)
    }
}