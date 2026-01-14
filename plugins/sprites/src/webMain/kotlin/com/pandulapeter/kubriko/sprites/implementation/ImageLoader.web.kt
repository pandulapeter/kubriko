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
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

internal actual fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int): ImageBitmap {
    val image = Image.makeFromEncoded(this)
    val targetImage: Image
    if (resourceDensity > targetDensity) {
        val scale = targetDensity.toFloat() / resourceDensity.toFloat()
        val targetH = image.height * scale
        val targetW = image.width * scale
        val srcRect = Rect.Companion.makeWH(image.width.toFloat(), image.height.toFloat())
        val dstRect = Rect.Companion.makeWH(targetW, targetH)
        targetImage = Surface.makeRasterN32Premul(targetW.toInt(), targetH.toInt()).run {
            val paint = Paint().apply { isAntiAlias = true }
            canvas.drawImageRect(image, srcRect, dstRect, SamplingMode.LINEAR, paint, true)
            makeImageSnapshot()
        }
    } else {
        targetImage = image
    }
    return targetImage.toComposeImageBitmap()
}