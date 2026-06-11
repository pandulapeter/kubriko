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

// Caches the top-down (minimap) grid line geometry between frames — as a triangle mesh, not a
// stroked Path: Skia software-rasterizes complex stroked paths into coverage masks on every
// transform change, while a vertex mesh is transformed on the GPU for free. The mesh is built in
// camera-independent space (cell corners at i * cellW), so a moving camera only changes the canvas
// translation at draw time; rebuilds happen when the visible cell window or the cell size changes.
class TopDownGridLineCache {

    internal val triangles = TriangleBatch()
    private var iStart = Int.MIN_VALUE
    private var iEnd = Int.MIN_VALUE
    private var jStart = Int.MIN_VALUE
    private var jEnd = Int.MIN_VALUE
    private var cellW = Float.NaN
    private var argb = 0
    private var halfWidth = Float.NaN
    private var gridMap: GridMap? = null

    internal fun prepare(
        iStart: Int,
        iEnd: Int,
        jStart: Int,
        jEnd: Int,
        cellW: Float,
        argb: Int,
        halfWidth: Float,
        gridMap: GridMap,
    ) {
        if (iStart == this.iStart && iEnd == this.iEnd && jStart == this.jStart && jEnd == this.jEnd
            && cellW == this.cellW && argb == this.argb && halfWidth == this.halfWidth && gridMap === this.gridMap
        ) {
            return
        }
        this.iStart = iStart; this.iEnd = iEnd; this.jStart = jStart; this.jEnd = jEnd
        this.cellW = cellW
        this.argb = argb; this.halfWidth = halfWidth
        this.gridMap = gridMap
        triangles.reset()
        for (j in jStart..jEnd) {
            val y0 = j * cellW
            val y1 = y0 + cellW
            var flagIndex = j * gridMap.width + iStart
            for (i in iStart..iEnd) {
                val flags = gridMap.edgeFlags[flagIndex].toInt()
                if (flags != 0) {
                    val x0 = i * cellW
                    val x1 = x0 + cellW
                    if (flags and GridMap.EDGE_TOP != 0) {
                        triangles.addLine(x0, y0, x1, y0, halfWidth, argb)
                    }
                    if (flags and GridMap.EDGE_RIGHT != 0) {
                        triangles.addLine(x1, y0, x1, y1, halfWidth, argb)
                    }
                    if (flags and GridMap.EDGE_BOTTOM != 0) {
                        triangles.addLine(x1, y1, x0, y1, halfWidth, argb)
                    }
                    if (flags and GridMap.EDGE_LEFT != 0) {
                        triangles.addLine(x0, y1, x0, y0, halfWidth, argb)
                    }
                }
                flagIndex++
            }
        }
    }
}
