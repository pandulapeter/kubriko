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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Draws textured batches through the real Skia path and reads the pixels back. */
class TriangleBatchSamplingTest {

    private val red = Color.Red.toArgb()
    private val blue = Color.Blue.toArgb()

    /** Two texels across: red, then blue. */
    private val texture = ImageBitmap(2, 1).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, 1f, 1f, Paint().apply { color = Color.Red })
        canvas.drawRect(1f, 0f, 2f, 1f, Paint().apply { color = Color.Blue })
    }

    /** Draws the texture's coordinates 0 to 4 across an 8 pixel wide strip and returns the strip's pixels. */
    private fun drawStrip(tileMode: TileMode, filterQuality: FilterQuality): IntArray {
        val target = ImageBitmap(8, 1)
        TriangleBatch().apply {
            texture = this@TriangleBatchSamplingTest.texture
            textureTileMode = tileMode
            textureFilterQuality = filterQuality
            addTexturedQuad(
                0f, 0f, 0f, 0.5f,
                8f, 0f, 4f, 0.5f,
                8f, 1f, 4f, 0.5f,
                0f, 1f, 0f, 0.5f,
                argb = Color.White.toArgb(),
            )
            draw(Canvas(target))
        }
        return IntArray(8).also { target.readPixels(it) }
    }

    private val Int.red get() = (this shr 16) and 0xFF
    private val Int.blue get() = this and 0xFF

    @Test
    fun `nearest-neighbor sampling keeps every texel crisp`() {
        val pixels = drawStrip(TileMode.Repeated, FilterQuality.None)

        assertEquals(listOf(red, red, blue, blue, red, red, blue, blue), pixels.toList())
    }

    @Test
    fun `clamped sampling stretches the edge texel instead of repeating`() {
        val pixels = drawStrip(TileMode.Clamp, FilterQuality.None)

        assertEquals(listOf(red, red, blue, blue, blue, blue, blue, blue), pixels.toList())
    }

    @Test
    fun `the default sampling repeats and filters`() {
        val pixels = drawStrip(TileMode.Repeated, FilterQuality.Medium)

        assertTrue(pixels[4].red > pixels[4].blue, "Repeated sampling starts the texture over past its edge.")
        assertTrue(pixels[0].blue > 0, "Filtered sampling blends neighboring texels.")
    }
}
