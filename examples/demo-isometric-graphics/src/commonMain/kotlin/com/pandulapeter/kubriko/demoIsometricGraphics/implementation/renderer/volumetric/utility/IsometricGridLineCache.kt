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

import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.GridMap
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.TriangleBatch
import kotlin.math.ceil
import kotlin.math.floor

// Caches the isometric grid line geometry between frames — as a triangle mesh, not a stroked Path:
// Skia software-rasterizes complex stroked paths into full-screen coverage masks on every transform
// change (~ms of CPU per frame on the HWUI workers), while a vertex mesh is transformed on the GPU
// for free. The mesh is built in camera-independent space (vertices at i * dXi + j * dXj without
// the screen translation), so a moving camera only changes the canvas translation at draw time:
// walking across the map never rebuilds it. Rebuilds happen when the visible cell window or the
// isometric basis vectors (zoom, rotation, tilt) change.
//
// At rebuild time, cells are restricted to the screen's footprint: the screen rectangle maps to a
// convex quad in (i, j) cell space (the quad corners are passed in but are deliberately NOT part
// of the cache key), and each cell row is walked only across the span where it intersects that
// quad. The quad is padded by QUAD_PADDING_CELLS — the camera can drift by at most one cell before
// the window indices shift and trigger a rebuild anyway, so the pad keeps every visible cell
// covered between rebuilds.
class IsometricGridLineCache {

    internal val triangles = TriangleBatch()
    private var iStart = Int.MIN_VALUE
    private var iEnd = Int.MIN_VALUE
    private var jStart = Int.MIN_VALUE
    private var jEnd = Int.MIN_VALUE
    private var dXi = Float.NaN
    private var dYi = Float.NaN
    private var dXj = Float.NaN
    private var dYj = Float.NaN
    private var argb = 0
    private var halfWidth = Float.NaN
    private var gridMap: GridMap? = null

    internal fun prepare(
        iStart: Int,
        iEnd: Int,
        jStart: Int,
        jEnd: Int,
        dXi: Float,
        dYi: Float,
        dXj: Float,
        dYj: Float,
        argb: Int,
        halfWidth: Float,
        gridMap: GridMap,
        quadI0: Float, quadJ0: Float,
        quadI1: Float, quadJ1: Float,
        quadI2: Float, quadJ2: Float,
        quadI3: Float, quadJ3: Float,
    ) {
        if (iStart == this.iStart && iEnd == this.iEnd && jStart == this.jStart && jEnd == this.jEnd
            && dXi == this.dXi && dYi == this.dYi && dXj == this.dXj && dYj == this.dYj
            && argb == this.argb && halfWidth == this.halfWidth && gridMap === this.gridMap
        ) {
            return
        }
        this.iStart = iStart; this.iEnd = iEnd; this.jStart = jStart; this.jEnd = jEnd
        this.dXi = dXi; this.dYi = dYi; this.dXj = dXj; this.dYj = dYj
        this.argb = argb; this.halfWidth = halfWidth
        this.gridMap = gridMap
        triangles.reset()
        val quadMinJ = minOf(minOf(quadJ0, quadJ1), minOf(quadJ2, quadJ3))
        val quadMaxJ = maxOf(maxOf(quadJ0, quadJ1), maxOf(quadJ2, quadJ3))
        for (j in jStart..jEnd) {
            val jCenter = (j + 0.5f).coerceIn(quadMinJ + ROW_EPSILON, quadMaxJ - ROW_EPSILON)
            if (jCenter > j + 0.5f + QUAD_PADDING_CELLS || jCenter < j + 0.5f - QUAD_PADDING_CELLS) continue
            // The horizontal line j = jCenter crosses exactly two edges of the convex quad; the
            // crossings bound the i-span of visible cells in this row.
            var spanMin = Float.POSITIVE_INFINITY
            var spanMax = Float.NEGATIVE_INFINITY
            var crossing = edgeCrossing(quadI0, quadJ0, quadI1, quadJ1, jCenter)
            if (!crossing.isNaN()) { if (crossing < spanMin) spanMin = crossing; if (crossing > spanMax) spanMax = crossing }
            crossing = edgeCrossing(quadI1, quadJ1, quadI2, quadJ2, jCenter)
            if (!crossing.isNaN()) { if (crossing < spanMin) spanMin = crossing; if (crossing > spanMax) spanMax = crossing }
            crossing = edgeCrossing(quadI2, quadJ2, quadI3, quadJ3, jCenter)
            if (!crossing.isNaN()) { if (crossing < spanMin) spanMin = crossing; if (crossing > spanMax) spanMax = crossing }
            crossing = edgeCrossing(quadI3, quadJ3, quadI0, quadJ0, jCenter)
            if (!crossing.isNaN()) { if (crossing < spanMin) spanMin = crossing; if (crossing > spanMax) spanMax = crossing }
            if (spanMin > spanMax) continue
            val iFrom = floor(spanMin - QUAD_PADDING_CELLS).toInt().coerceAtLeast(iStart)
            val iTo = ceil(spanMax + QUAD_PADDING_CELLS).toInt().coerceAtMost(iEnd)
            if (iFrom > iTo) continue
            var ax = iFrom * dXi + j * dXj
            var ay = iFrom * dYi + j * dYj
            var flagIndex = j * gridMap.width + iFrom
            for (i in iFrom..iTo) {
                val flags = gridMap.edgeFlags[flagIndex].toInt()
                if (flags != 0) {
                    val bx = ax + dXi
                    val by = ay + dYi
                    val dx = ax + dXj
                    val dy = ay + dYj
                    val cx = bx + dXj
                    val cy = by + dYj
                    if (flags and GridMap.EDGE_TOP != 0) {
                        triangles.addLine(ax, ay, bx, by, halfWidth, argb)
                    }
                    if (flags and GridMap.EDGE_RIGHT != 0) {
                        triangles.addLine(bx, by, cx, cy, halfWidth, argb)
                    }
                    if (flags and GridMap.EDGE_BOTTOM != 0) {
                        triangles.addLine(cx, cy, dx, dy, halfWidth, argb)
                    }
                    if (flags and GridMap.EDGE_LEFT != 0) {
                        triangles.addLine(dx, dy, ax, ay, halfWidth, argb)
                    }
                }
                ax += dXi
                ay += dYi
                flagIndex++
            }
        }
    }

    // Returns the i coordinate where the edge (aI, aJ) → (bI, bJ) crosses the line j = jLine, or
    // NaN when it doesn't.
    private fun edgeCrossing(aI: Float, aJ: Float, bI: Float, bJ: Float, jLine: Float): Float {
        if ((aJ - jLine) * (bJ - jLine) > 0f || aJ == bJ) return Float.NaN
        return aI + (jLine - aJ) / (bJ - aJ) * (bI - aI)
    }

    private companion object {
        const val QUAD_PADDING_CELLS = 2f
        const val ROW_EPSILON = 0.0001f
    }
}
