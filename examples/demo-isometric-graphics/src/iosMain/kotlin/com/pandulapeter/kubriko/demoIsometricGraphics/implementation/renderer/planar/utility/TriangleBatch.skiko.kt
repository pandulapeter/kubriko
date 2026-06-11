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
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.VertexMode

// MODULATE multiplies the vertex colors with the opaque white paint, i.e. uses them as-is.
private val trianglePaint = Paint().apply { color = -1 }

// Skia derives the vertex count from the array sizes, so oversized batch buffers are trimmed into
// reusable mirrors. Several batches with different sizes flush every frame (scene mesh, grid
// caches), hence a small exact-size pool with round-robin eviction instead of a single slot.
private const val TRIM_POOL_SIZE = 8
private val pooledPositions = arrayOfNulls<FloatArray>(TRIM_POOL_SIZE)
private val pooledColors = arrayOfNulls<IntArray>(TRIM_POOL_SIZE)
private var nextEvictionIndex = 0

internal actual fun drawTriangles(canvas: Canvas, positions: FloatArray, colors: IntArray, vertexCount: Int) {
    val positionCount = vertexCount * 2
    val positionsToDraw: FloatArray
    val colorsToDraw: IntArray
    if (positions.size == positionCount) {
        positionsToDraw = positions
        colorsToDraw = colors
    } else {
        var slot = -1
        for (i in 0 until TRIM_POOL_SIZE) {
            if (pooledPositions[i]?.size == positionCount) {
                slot = i
                break
            }
        }
        if (slot == -1) {
            slot = nextEvictionIndex
            nextEvictionIndex = (nextEvictionIndex + 1) % TRIM_POOL_SIZE
            pooledPositions[slot] = FloatArray(positionCount)
            pooledColors[slot] = IntArray(vertexCount)
        }
        positionsToDraw = pooledPositions[slot]!!
        colorsToDraw = pooledColors[slot]!!
        positions.copyInto(positionsToDraw, 0, 0, positionCount)
        colors.copyInto(colorsToDraw, 0, 0, vertexCount)
    }
    canvas.skiaCanvas.drawVertices(
        VertexMode.TRIANGLES,
        positionsToDraw,
        colorsToDraw,
        null,
        null,
        BlendMode.MODULATE,
        trianglePaint,
    )
}
