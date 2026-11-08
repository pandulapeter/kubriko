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
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.pandulapeter.kubriko.implementation.writeArgbPixelsThroughFastPath
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo

actual fun ImageBitmap.markImmutable(): ImageBitmap {
    asSkiaBitmap().setImmutable()
    return this
}

actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int, isDrawOnly: Boolean): ImageBitmap {
    requireArgbPixels(pixels, width, height)
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.UNPREMUL))
    if (!bitmap.writeArgbPixelsThroughFastPath(pixels, width, height)) {
        bitmap.installPixels(pixels.toSkiaPixelBytes(width * height))
    }
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

/** Skia's N32 pixels are byte-ordered blue, green, red, alpha on every Skia target, whatever the platform's endianness. */
private fun IntArray.toSkiaPixelBytes(pixelCount: Int): ByteArray {
    val bytes = ByteArray(pixelCount * Int.SIZE_BYTES)
    var byteIndex = 0
    for (index in 0 until pixelCount) {
        val argb = this[index]
        bytes[byteIndex++] = argb.toByte()
        bytes[byteIndex++] = (argb shr 8).toByte()
        bytes[byteIndex++] = (argb shr 16).toByte()
        bytes[byteIndex++] = (argb shr 24).toByte()
    }
    return bytes
}
