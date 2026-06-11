/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.ceil
import kotlin.math.floor

fun DrawScope.drawTopDownGrid(
    gridLinesPath: Path,
    gridColor: Color,
    cellSize: SceneUnit,
    cameraPosition: SceneOffset,
    multiplier: Float,
    gridMap: GridMap? = null,
    gridStroke: Stroke,
    lineCache: TopDownGridLineCache? = null,
) {
    gridLinesPath.rewind()
    val cellW = cellSize.raw * multiplier * 2f
    if (cellW <= 0.001f) return
    val camX = cameraPosition.x.raw * multiplier
    val camY = cameraPosition.y.raw * multiplier
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val minI = floor((camX - centerX) / cellW).toInt() - 1
    val maxI = ceil((camX + centerX) / cellW).toInt() + 1
    val minJ = floor((camY - centerY) / cellW).toInt() - 1
    val maxJ = ceil((camY + centerY) / cellW).toInt() + 1
    if (gridMap != null) {
        val iStart = minI.coerceAtLeast(0)
        val iEnd = maxI.coerceAtMost(gridMap.width - 1)
        val jStart = minJ.coerceAtLeast(0)
        val jEnd = maxJ.coerceAtMost(gridMap.height - 1)
        if (iStart > iEnd || jStart > jEnd) return
        val canvas = drawContext.canvas
        canvas.save()
        // One texel per cell: the translate + scale do all the positioning, so the integer
        // src/dst coordinates stay exact and the whole fill pass is a single draw call.
        canvas.translate(centerX - camX, centerY - camY)
        canvas.scale(cellW, cellW)
        val visibleOffset = IntOffset(iStart, jStart)
        val visibleSize = IntSize(iEnd - iStart + 1, jEnd - jStart + 1)
        drawImage(
            image = gridMap.image,
            srcOffset = visibleOffset,
            srcSize = visibleSize,
            dstOffset = visibleOffset,
            dstSize = visibleSize,
            filterQuality = FilterQuality.None,
        )
        canvas.restore()
        if (lineCache != null) {
            lineCache.prepare(
                iStart = iStart,
                iEnd = iEnd,
                jStart = jStart,
                jEnd = jEnd,
                cellW = cellW,
                argb = gridColor.toArgb(),
                halfWidth = gridStroke.width * 0.5f,
                gridMap = gridMap,
            )
            if (!lineCache.triangles.isEmpty) {
                canvas.save()
                canvas.translate(centerX - camX, centerY - camY)
                lineCache.triangles.draw(canvas)
                canvas.restore()
            }
            return
        }
        for (j in jStart..jEnd) {
            val y0 = (j * cellW) - camY + centerY
            val y1 = y0 + cellW
            var flagIndex = j * gridMap.width + iStart
            for (i in iStart..iEnd) {
                val flags = gridMap.edgeFlags[flagIndex].toInt()
                if (flags != 0) {
                    val x0 = (i * cellW) - camX + centerX
                    val x1 = x0 + cellW
                    if (flags and GridMap.EDGE_TOP != 0) {
                        gridLinesPath.moveTo(x0, y0); gridLinesPath.lineTo(x1, y0)
                    }
                    if (flags and GridMap.EDGE_RIGHT != 0) {
                        gridLinesPath.moveTo(x1, y0); gridLinesPath.lineTo(x1, y1)
                    }
                    if (flags and GridMap.EDGE_BOTTOM != 0) {
                        gridLinesPath.moveTo(x1, y1); gridLinesPath.lineTo(x0, y1)
                    }
                    if (flags and GridMap.EDGE_LEFT != 0) {
                        gridLinesPath.moveTo(x0, y1); gridLinesPath.lineTo(x0, y0)
                    }
                }
                flagIndex++
            }
        }
        drawPath(
            path = gridLinesPath,
            color = gridColor,
            style = gridStroke
        )
    } else {
        for (i in minI..maxI) {
            val x = (i * cellW) - camX + centerX
            if (i == 0) {
                drawLine(
                    color = Color.Green,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = gridStroke.width,
                    cap = StrokeCap.Round,
                )
            } else {
                gridLinesPath.moveTo(x, 0f)
                gridLinesPath.lineTo(x, size.height)
            }
        }
        for (j in minJ..maxJ) {
            val y = (j * cellW) - camY + centerY
            if (j == 0) {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = gridStroke.width,
                    cap = StrokeCap.Round,
                )
            } else {
                gridLinesPath.moveTo(0f, y)
                gridLinesPath.lineTo(size.width, y)
            }
        }
        drawPath(
            path = gridLinesPath,
            color = gridColor,
            style = gridStroke
        )
    }
}
