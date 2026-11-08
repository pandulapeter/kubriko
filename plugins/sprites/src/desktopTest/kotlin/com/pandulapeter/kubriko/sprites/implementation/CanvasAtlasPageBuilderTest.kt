/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.implementation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.pandulapeter.kubriko.sprites.SpriteResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Packs real bitmaps through Skia, checking the copied pixels and their padding one by one. */
class CanvasAtlasPageBuilderTest {

    @Test
    fun `sprites are copied exactly, padded by their own edges, with nothing between them`() {
        val sizes = listOf(5 to 3, 4 to 7, 1 to 1, 6 to 2)
        val sprites = sizes.mapIndexed { index, (width, height) -> bitmapOf(width, height) { x, y -> pixelOf(index, x, y) } }
        val resources = sprites.indices.map { spriteResource("sprite$it") }
        val padding = 2

        val packedAtlas = packAtlas(resources, sprites, maximumPageSize = 64, padding = padding, pageBuilder = CanvasAtlasPageBuilder)

        val page = packedAtlas.pages.single()
        val expected = IntArray(page.width * page.height)
        sprites.indices.forEach { index ->
            val region = packedAtlas.regions.getValue(resources[index])
            val (width, height) = sizes[index]
            for (y in -padding until height + padding) {
                for (x in -padding until width + padding) {
                    expected[(region.offset.y + y) * page.width + region.offset.x + x] = pixelOf(index, x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
                }
            }
        }
        val actual = IntArray(page.width * page.height).also { page.readPixels(it) }
        assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun `a finished page is immutable`() {
        val page = CanvasAtlasPageBuilder.build(4, 4, listOf(bitmapOf(2, 2) { _, _ -> OPAQUE_RED }), intArrayOf(1), intArrayOf(1), padding = 1)

        assertTrue(page.asSkiaBitmap().isImmutable)
    }

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
        bitmap.installPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL), bytes, width * 4)
        return Image.makeFromBitmap(bitmap).toComposeImageBitmap()
    }

    /** An opaque color naming its sprite and coordinates. */
    private fun pixelOf(sprite: Int, x: Int, y: Int) = OPAQUE_BLACK or ((sprite + 1) shl 16) or (y shl 8) or x

    @OptIn(InternalResourceApi::class)
    private fun spriteResource(path: String) = SpriteResource(DrawableResource(path, emptySet()))

    private companion object {
        const val OPAQUE_BLACK = 0xFF000000.toInt()
        const val OPAQUE_RED = 0xFFFF0000.toInt()
    }
}
