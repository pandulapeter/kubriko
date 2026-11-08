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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ImageBitmap.markImmutable(): ImageBitmap = this

actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int, isDrawOnly: Boolean): ImageBitmap {
    requireArgbPixels(pixels, width, height)
    val software = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    if (!isDrawOnly) return software.asImageBitmap()
    // A device that can't allocate a hardware bitmap keeps the software one.
    val hardware = software.copy(Bitmap.Config.HARDWARE, false) ?: return software.asImageBitmap()
    software.recycle()
    return hardware.asImageBitmap()
}
