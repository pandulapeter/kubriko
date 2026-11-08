/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageBitmapExtensionsTest {

    private fun bitmapOf(width: Int, height: Int, pixel: (Int, Int) -> Int): ImageBitmap {
        val bytes = ByteArray(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = pixel(x, y)
                val offset = (y * width + x) * 4
                // N32 is BGRA on every platform these tests run on.
                bytes[offset] = (argb and 0xFF).toByte()
                bytes[offset + 1] = ((argb shr 8) and 0xFF).toByte()
                bytes[offset + 2] = ((argb shr 16) and 0xFF).toByte()
                bytes[offset + 3] = (argb ushr 24).toByte()
            }
        }
        val bitmap = Bitmap()
        bitmap.installPixels(ImageInfo.makeN32(width, height, ColorAlphaType.UNPREMUL), bytes, width * 4)
        return Image.makeFromBitmap(bitmap).toComposeImageBitmap()
    }

    @Test
    fun `mean of an opaque bitmap averages its channels`() {
        val bitmap = bitmapOf(2, 1) { x, _ -> if (x == 0) 0xFFFF0000.toInt() else 0xFF0000FF.toInt() }

        assertEquals(0xFF7F007F.toInt(), bitmap.meanArgb())
    }

    @Test
    fun `transparent pixels lower the alpha without tinting the color`() {
        val bitmap = bitmapOf(2, 1) { x, _ -> if (x == 0) 0xFF00FF00.toInt() else 0x00000000 }

        assertEquals(0x7F00FF00, bitmap.meanArgb())
    }

    @Test
    fun `the mean of a region reads only that region`() {
        val bitmap = bitmapOf(4, 2) { x, _ -> if (x < 2) 0xFFFF0000.toInt() else 0xFF00FF00.toInt() }

        assertEquals(0xFF00FF00.toInt(), bitmap.meanArgb(left = 2, top = 0, width = 2, height = 2))
        assertEquals(0xFFFF0000.toInt(), bitmap.meanArgb(left = -5, top = 0, width = 7, height = 9))
        assertEquals(0, bitmap.meanArgb(left = 4, top = 0, width = 2, height = 2))
    }

    @Test
    fun `a fully transparent bitmap has no mean color`() {
        assertEquals(0, bitmapOf(3, 3) { _, _ -> 0 }.meanArgb())
    }

    @Test
    fun `a large bitmap is sampled on a bounded grid`() {
        val bitmap = bitmapOf(MEAN_COLOR_SAMPLES_PER_AXIS * 4, 8) { _, _ -> 0xFF336699.toInt() }

        assertEquals(0xFF336699.toInt(), bitmap.meanArgb())
    }

    @Test
    fun `marking a bitmap immutable returns it marked`() {
        val bitmap = ImageBitmap(4, 4)
        assertFalse(bitmap.asSkiaBitmap().isImmutable)

        assertSame(bitmap, bitmap.markImmutable())
        assertTrue(bitmap.asSkiaBitmap().isImmutable)
    }
}
