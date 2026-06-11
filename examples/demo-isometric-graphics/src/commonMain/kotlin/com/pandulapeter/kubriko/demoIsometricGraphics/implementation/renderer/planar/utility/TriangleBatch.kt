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

import androidx.compose.ui.graphics.Canvas
import kotlin.math.sqrt

// Accumulates solid-color triangles from many cuboids so an entire scene can be rasterized with a
// handful of drawVertices calls instead of thousands of individual canvas operations. Painter's
// order is preserved by emission order within a batch and by flushing whenever something has to be
// drawn through a different code path (e.g. a textured face). Buffers are reused across frames;
// steady-state operation does not allocate.
class TriangleBatch {

    private var positions = FloatArray(INITIAL_VERTEX_CAPACITY * 2)
    private var colors = IntArray(INITIAL_VERTEX_CAPACITY)
    private var vertexCount = 0

    val isEmpty: Boolean get() = vertexCount == 0

    fun addQuad(
        p0x: Float, p0y: Float,
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        argb: Int,
    ) {
        ensureCapacity(vertexCount + 6)
        val positions = positions
        var p = vertexCount * 2
        positions[p++] = p0x; positions[p++] = p0y
        positions[p++] = p1x; positions[p++] = p1y
        positions[p++] = p2x; positions[p++] = p2y
        positions[p++] = p0x; positions[p++] = p0y
        positions[p++] = p2x; positions[p++] = p2y
        positions[p++] = p3x; positions[p] = p3y
        colors.fill(argb, vertexCount, vertexCount + 6)
        vertexCount += 6
    }

    // Emits a line segment as a thin quad. Endpoints are extended by [halfWidth] (square caps) so
    // that segments meeting at shared corners close the joint instead of leaving notches.
    fun addLine(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        halfWidth: Float,
        argb: Int,
    ) {
        var dx = bx - ax
        var dy = by - ay
        val length = sqrt(dx * dx + dy * dy)
        if (length < 0.001f) return
        val scale = halfWidth / length
        dx *= scale
        dy *= scale
        val nx = -dy
        val ny = dx
        addQuad(
            p0x = ax - dx + nx, p0y = ay - dy + ny,
            p1x = bx + dx + nx, p1y = by + dy + ny,
            p2x = bx + dx - nx, p2y = by + dy - ny,
            p3x = ax - dx - nx, p3y = ay - dy - ny,
            argb = argb,
        )
    }

    // Draws the accumulated triangles without clearing them — for callers that build geometry once
    // and replay it across frames under a changing canvas transform (e.g. cached grid lines).
    fun draw(canvas: Canvas) {
        if (vertexCount > 0) {
            drawTriangles(canvas, positions, colors, vertexCount)
        }
    }

    fun reset() {
        vertexCount = 0
    }

    fun flush(canvas: Canvas) {
        draw(canvas)
        reset()
    }

    private fun ensureCapacity(requiredVertexCount: Int) {
        if (colors.size < requiredVertexCount) {
            val newSize = maxOf(requiredVertexCount, colors.size * 2)
            positions = positions.copyOf(newSize * 2)
            colors = colors.copyOf(newSize)
        }
    }

    private companion object {
        const val INITIAL_VERTEX_CAPACITY = 1024
    }
}

// Rasterizes the first [vertexCount] colored vertices in a single native draw call; every three
// consecutive vertices form one triangle. The arrays may be larger than needed — implementations
// that cannot pass a count (Skiko) trim internally, while Android draws straight from the given
// arrays so the per-frame hot path never copies or allocates. Triangles are not anti-aliased on
// any platform, which is invisible in practice because cuboid faces are bordered by their outlines.
internal expect fun drawTriangles(canvas: Canvas, positions: FloatArray, colors: IntArray, vertexCount: Int)
