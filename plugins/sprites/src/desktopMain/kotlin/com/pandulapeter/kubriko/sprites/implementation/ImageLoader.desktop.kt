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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.pandulapeter.kubriko.sprites.SpriteResource
import org.jetbrains.skia.Image
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

internal actual fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int, rotation: SpriteResource.Rotation): ImageBitmap {
    val image = Image.makeFromEncoded(this)

    if (resourceDensity == targetDensity && rotation == SpriteResource.Rotation.NONE) return image.toComposeImageBitmap()

    //https://youtrack.jetbrains.com/issue/CMP-5657
    //android only downscales drawables. If there is only low dpi resource then use it as is (not upscale)
    //we need a consistent behavior on all platforms
    val scale = if (resourceDensity > targetDensity) {
            targetDensity.toFloat() / resourceDensity.toFloat()
        } else {
            1f
        }
    val targetH = image.height * scale
    val targetW = image.width * scale

    val canvasH = when (rotation) {
        SpriteResource.Rotation.NONE,
        SpriteResource.Rotation.DEGREES_180 -> targetH
        SpriteResource.Rotation.DEGREES_90,
        SpriteResource.Rotation.DEGREES_270 -> targetW
    }
    val canvasW = when (rotation) {
        SpriteResource.Rotation.NONE,
        SpriteResource.Rotation.DEGREES_180 -> targetW
        SpriteResource.Rotation.DEGREES_90,
        SpriteResource.Rotation.DEGREES_270 -> targetH
    }

    val srcRect = Rect.makeWH(image.width.toFloat(), image.height.toFloat())
    val dstRect = when (rotation) {
        SpriteResource.Rotation.NONE -> Rect.makeWH(targetW, targetH)
        SpriteResource.Rotation.DEGREES_90 -> Rect.makeXYWH(0f, -targetH,targetW, targetH)
        SpriteResource.Rotation.DEGREES_180 -> Rect.makeXYWH(-targetW, -targetH,targetW, targetH)
        SpriteResource.Rotation.DEGREES_270 -> Rect.makeXYWH(-targetW, 0f,targetW, targetH)
    }

    return Surface.makeRasterN32Premul(canvasW.toInt(), canvasH.toInt()).run {
        canvas.apply {
            when (rotation) {
                SpriteResource.Rotation.NONE -> Unit
                SpriteResource.Rotation.DEGREES_90 -> rotate(90f)
                SpriteResource.Rotation.DEGREES_180 -> rotate(-180f)
                SpriteResource.Rotation.DEGREES_270 -> rotate(-90f)
            }
            val paint = Paint().apply { isAntiAlias = true }
            drawImageRect(image, srcRect, dstRect, SamplingMode.LINEAR, paint, true)
        }
        makeImageSnapshot()
    }.toComposeImageBitmap()
}