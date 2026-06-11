/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.utility

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.GridMap
import kotlin.math.hypot

fun DrawScope.drawIsometricGrid(
    gridLinesPath: Path,
    isoMatrix: Matrix,
    gridColor: Color,
    tileWidth: SceneUnit,
    tileHeight: SceneUnit,
    cameraPosition: SceneOffset,
    worldRotation: AngleRadians,
    zoom: Float = 1f,
    tilt: Float = 1f,
    gridMap: GridMap? = null,
    stroke: Stroke,
    size: Size,
    cameraPositionIsProjected: Boolean = false,
    focusHeight: Float = 0f,
    lineCache: IsometricGridLineCache? = null,
) {
    gridLinesPath.rewind()
    val sqrtTwo = 1.4142135f
    val multiplierX = zoom * sqrtTwo
    val multiplierY = zoom * sqrtTwo * tilt
    val depthEffect = (multiplierY / (multiplierX * 2f)) * 1.5f
    val tileW = tileWidth.raw * multiplierX * 2f
    if (tileW <= 0.001f) return
    val ratio = (tileHeight.raw * multiplierY) / tileW
    val camX = cameraPosition.x.raw
    val camY = cameraPosition.y.raw
    val camXScaled = camX * multiplierX
    val camYScaled = camY * multiplierX
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val cosR = worldRotation.cos
    val sinR = worldRotation.sin
    val m0 = 0.5f * (cosR - sinR)
    val m1 = 0.5f * ratio * (cosR + sinR)
    val m4 = -0.5f * (cosR + sinR)
    val m5 = 0.5f * ratio * (cosR - sinR)
    val det = (m0 * m5 - m1 * m4).takeIf { it != 0f } ?: 0.0001f
    val invM0 = m5 / det
    val invM4 = -m4 / det
    val invM1 = -m1 / det
    val invM5 = m0 / det

    val worldCamX = if (cameraPositionIsProjected) camX * invM0 + camY * invM4 else camXScaled
    val worldCamY = if (cameraPositionIsProjected) camX * invM1 + camY * invM5 else camYScaled

    val tx = if (cameraPositionIsProjected) centerX - camX else centerX - (camXScaled * m0 + camYScaled * m4)
    val ty = if (cameraPositionIsProjected) {
        centerY - camY
    } else {
        centerY - (camXScaled * m1 + camYScaled * m5) + 0.5f * (focusHeight / depthEffect) * multiplierY
    }

    fun gridI(screenX: Float, screenY: Float): Float {
        val relX = screenX - tx
        val relY = screenY - ty
        return (relX * invM0 + relY * invM4) / tileW
    }

    fun gridJ(screenX: Float, screenY: Float): Float {
        val relX = screenX - tx
        val relY = screenY - ty
        return (relX * invM1 + relY * invM5) / tileW
    }

    val pad = stroke.width + 2f
    val left = -pad
    val right = size.width + pad
    val top = -pad
    val bottom = size.height + pad
    val i0 = gridI(left, top)
    val i1 = gridI(right, top)
    val i2 = gridI(right, bottom)
    val i3 = gridI(left, bottom)
    val j0 = gridJ(left, top)
    val j1 = gridJ(right, top)
    val j2 = gridJ(right, bottom)
    val j3 = gridJ(left, bottom)
    val iMin = kotlin.math.floor(minOf(minOf(i0, i1), minOf(i2, i3))).toInt()
    val iMax = kotlin.math.ceil(maxOf(maxOf(i0, i1), maxOf(i2, i3))).toInt()
    val jMin = kotlin.math.floor(minOf(minOf(j0, j1), minOf(j2, j3))).toInt()
    val jMax = kotlin.math.ceil(maxOf(maxOf(j0, j1), maxOf(j2, j3))).toInt()
    val dXi = tileW * m0;
    val dYi = tileW * m1
    val dXj = tileW * m4;
    val dYj = tileW * m5
    if (gridMap != null) {
        val iStart = iMin.coerceAtLeast(0)
        val iEnd = iMax.coerceAtMost(gridMap.width - 1)
        val jStart = jMin.coerceAtLeast(0)
        val jEnd = jMax.coerceAtMost(gridMap.height - 1)
        if (iStart > iEnd || jStart > jEnd) return
        isoMatrix.reset()
        val v = isoMatrix.values
        v[0] = m0; v[1] = m1
        v[4] = m4; v[5] = m5
        v[12] = tx; v[13] = ty
        v[10] = 1f; v[15] = 1f
        drawContext.canvas.save()
        drawContext.canvas.concat(isoMatrix)
        // One texel per tile: the concat + scale do all the scaling, so the integer src/dst
        // coordinates stay exact and the whole fill pass is a single draw call.
        drawContext.canvas.scale(tileW, tileW)
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
        drawContext.canvas.restore()
        // LOD: as tiles shrink toward a few pixels the lines degenerate into moiré noise while
        // their stroking cost explodes (cell count grows with 1 / zoom²), so they fade out and
        // are skipped entirely below the threshold.
        val lineAlpha = ((tileW - GRID_LINES_FADE_OUT_TILE_PX) / (GRID_LINES_FADE_IN_TILE_PX - GRID_LINES_FADE_OUT_TILE_PX)).coerceIn(0f, 1f)
        if (lineAlpha <= 0f) return
        if (lineCache != null) {
            lineCache.prepare(
                iStart = iStart,
                iEnd = iEnd,
                jStart = jStart,
                jEnd = jEnd,
                dXi = dXi,
                dYi = dYi,
                dXj = dXj,
                dYj = dYj,
                argb = gridColor.copy(alpha = gridColor.alpha * lineAlpha).toArgb(),
                halfWidth = stroke.width * 0.5f,
                gridMap = gridMap,
                quadI0 = i0, quadJ0 = j0,
                quadI1 = i1, quadJ1 = j1,
                quadI2 = i2, quadJ2 = j2,
                quadI3 = i3, quadJ3 = j3,
            )
            if (!lineCache.triangles.isEmpty) {
                val canvas = drawContext.canvas
                canvas.save()
                canvas.translate(tx, ty)
                lineCache.triangles.draw(canvas)
                canvas.restore()
            }
            return
        }
        val boundsPadding = stroke.width * 1.5f
        var baseRowX = tx + iStart * dXi + jStart * dXj
        var baseRowY = ty + iStart * dYi + jStart * dYj
        for (j in jStart..jEnd) {
            var ax = baseRowX
            var ay = baseRowY
            var flagIndex = j * gridMap.width + iStart
            for (i in iStart..iEnd) {
                val flags = gridMap.edgeFlags[flagIndex].toInt()
                if (flags != 0) {
                    val bx = ax + dXi
                    val by = ay + dYi
                    val dx = ax + dXj
                    val dy = ay + dYj
                    val cx = bx + dXj
                    val cy = by + dYj
                    val minX = minOf(minOf(ax, bx), minOf(cx, dx))
                    val maxX = maxOf(maxOf(ax, bx), maxOf(cx, dx))
                    val minY = minOf(minOf(ay, by), minOf(cy, dy))
                    val maxY = maxOf(maxOf(ay, by), maxOf(cy, dy))
                    if (maxX >= -boundsPadding && minX <= size.width + boundsPadding &&
                        maxY >= -boundsPadding && minY <= size.height + boundsPadding
                    ) {
                        if (flags and GridMap.EDGE_TOP != 0) {
                            gridLinesPath.moveTo(ax, ay); gridLinesPath.lineTo(bx, by)
                        }
                        if (flags and GridMap.EDGE_RIGHT != 0) {
                            gridLinesPath.moveTo(bx, by); gridLinesPath.lineTo(cx, cy)
                        }
                        if (flags and GridMap.EDGE_BOTTOM != 0) {
                            gridLinesPath.moveTo(cx, cy); gridLinesPath.lineTo(dx, dy)
                        }
                        if (flags and GridMap.EDGE_LEFT != 0) {
                            gridLinesPath.moveTo(dx, dy); gridLinesPath.lineTo(ax, ay)
                        }
                    }
                }
                ax += dXi
                ay += dYi
                flagIndex++
            }
            baseRowX += dXj
            baseRowY += dYj
        }
        drawPath(
            path = gridLinesPath,
            color = gridColor,
            alpha = lineAlpha,
            style = stroke
        )
    } else {
        fun mapScreen(lx: Float, ly: Float) = Offset(
            x = tx + lx * m0 + ly * m4,
            y = ty + lx * m1 + ly * m5
        )

        val startY = jMin * tileW
        val endY = jMax * tileW
        for (i in iMin..iMax) {
            if (i == 0) continue
            val currX = i * tileW
            val p1 = mapScreen(currX, startY)
            val p2 = mapScreen(currX, endY)
            gridLinesPath.moveTo(p1.x, p1.y)
            gridLinesPath.lineTo(p2.x, p2.y)
        }
        val startX = iMin * tileW
        val endX = iMax * tileW
        for (j in jMin..jMax) {
            if (j == 0) continue
            val currY = j * tileW
            val p1 = mapScreen(startX, currY)
            val p2 = mapScreen(endX, currY)
            gridLinesPath.moveTo(p1.x, p1.y)
            gridLinesPath.lineTo(p2.x, p2.y)
        }
        drawPath(
            path = gridLinesPath,
            color = gridColor,
            style = stroke
        )
        val drawDist = hypot(size.width, size.height) * 2f
        drawLine(
            color = Color.Red,
            start = mapScreen(worldCamX - drawDist, 0f),
            end = mapScreen(worldCamX + drawDist, 0f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.Green,
            start = mapScreen(0f, worldCamY - drawDist),
            end = mapScreen(0f, worldCamY + drawDist),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.Blue,
            start = Offset(tx, -drawDist),
            end = Offset(tx, size.height + drawDist),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

// Tile width thresholds (in pixels) between which the grid lines fade out.
private const val GRID_LINES_FADE_IN_TILE_PX = 20f
private const val GRID_LINES_FADE_OUT_TILE_PX = 10f
