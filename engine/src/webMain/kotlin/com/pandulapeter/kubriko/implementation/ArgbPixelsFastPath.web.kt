/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo

internal actual fun createArgbBitmapThroughFastPath(pixels: IntArray, width: Int, height: Int): Bitmap? {
    val bitmap = Bitmap()
    check(bitmap.allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.UNPREMUL))) { "Skia could not allocate a $width × $height bitmap." }
    WasmPixelBridge.write(bitmap, pixels, width, height)
    return bitmap
}
