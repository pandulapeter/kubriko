/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.Shader
import android.media.ImageReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

// The probe has to go through the same pipeline the scene does, which rules out a Bitmap-backed Canvas: that one
// rasterizes on the CPU and would report success on every device, including the ones this exists to catch. So it
// renders a RenderNode into an ImageReader through HardwareRenderer - the display's own path, off the main thread
// - and reads the result back.
private const val PROBE_SIZE = 8

// The coordinates are asked well away from the origin, which is what makes this a question worth asking rather
// than a formality: at the origin every device answers correctly, and it is only once a coordinate grows that a
// half-float varying stops resolving a single texel. An even offset, so it names the same pair of texels of the
// two-wide probe tile that the origin would.
private const val PROBE_TEXEL_OFFSET = 1024f

// One quad the width of the target, its left half naming an opaque texel and its right half a black one, drawn
// through the same call shape the scene batch uses: colours, texture coordinates and an index array all present.
// Nearest sampling, so the two halves come back as the texels themselves rather than as a blend of them.
private val PROBE_POSITIONS = floatArrayOf(
    0f, 0f,
    PROBE_SIZE.toFloat(), 0f,
    PROBE_SIZE.toFloat(), PROBE_SIZE.toFloat(),
    0f, PROBE_SIZE.toFloat(),
)
private val PROBE_TEXTURE_COORDINATES = floatArrayOf(
    PROBE_TEXEL_OFFSET, 0.5f,
    PROBE_TEXEL_OFFSET + 2f, 0.5f,
    PROBE_TEXEL_OFFSET + 2f, 0.5f,
    PROBE_TEXEL_OFFSET, 0.5f,
)
private val PROBE_COLORS = IntArray(4) { Color.WHITE }
private val PROBE_INDICES = shortArrayOf(0, 1, 2, 0, 2, 3)

// Far enough apart that only a genuinely varying sample clears it, and far below the full black-to-white step the
// two texels actually name.
private const val MINIMUM_HALF_DIFFERENCE = 64

internal actual suspend fun probeTextureSampling(): Boolean = withContext(Dispatchers.Default) {
    var reader: ImageReader? = null
    var renderer: HardwareRenderer? = null
    try {
        reader = ImageReader.newInstance(PROBE_SIZE, PROBE_SIZE, PixelFormat.RGBA_8888, 2)
        renderer = HardwareRenderer().apply { setSurface(reader.surface) }
        val node = RenderNode("TriangleBatchProbe").apply { setPosition(0, 0, PROBE_SIZE, PROBE_SIZE) }
        node.beginRecording(PROBE_SIZE, PROBE_SIZE).run {
            drawColor(Color.BLACK)
            drawVertices(
                Canvas.VertexMode.TRIANGLES,
                PROBE_POSITIONS.size,
                PROBE_POSITIONS,
                0,
                PROBE_TEXTURE_COORDINATES,
                0,
                PROBE_COLORS,
                0,
                PROBE_INDICES,
                0,
                PROBE_INDICES.size,
                probePaint(),
            )
        }
        node.endRecording()
        renderer.setContentRoot(node)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        val image = reader.acquireNextImage() ?: return@withContext true
        try {
            val plane = image.planes[0]
            val row = plane.rowStride * (PROBE_SIZE / 2)
            val left = plane.buffer.get(row + plane.pixelStride).toInt() and 0xFF
            val right = plane.buffer.get(row + plane.pixelStride * (PROBE_SIZE - 2)).toInt() and 0xFF
            abs(left - right) >= MINIMUM_HALF_DIFFERENCE
        } finally {
            image.close()
        }
    } catch (_: Throwable) {
        // A device that cannot be asked is answered for. @see TriangleBatchSupport
        true
    } finally {
        renderer?.destroy()
        reader?.close()
    }
}

private fun probePaint() = Paint().apply {
    color = Color.WHITE
    isFilterBitmap = false
    shader = BitmapShader(probeTile(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
}

// Two texels across: an opaque left column and a black right column, which is the whole difference the probe
// reads. Repeated, so the offset above lands on the same pair the origin would.
private fun probeTile() = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
    setPixel(0, 0, Color.WHITE)
    setPixel(0, 1, Color.WHITE)
    setPixel(1, 0, Color.BLACK)
    setPixel(1, 1, Color.BLACK)
}
