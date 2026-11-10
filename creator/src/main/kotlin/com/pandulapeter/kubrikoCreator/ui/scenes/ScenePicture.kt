/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import kotlin.math.roundToInt

/**
 * Takes a picture of the Scene the Scene editor shows, as a PNG, or null if it shows nothing yet.
 */
internal fun interface ScenePicture {
    suspend fun png(): ByteArray?

    companion object {
        /**
         * How wide a picture is at most: as wide as the game's screenshots, which is plenty to tell what a Scene holds.
         */
        const val MAXIMUM_WIDTH = 1280

        /**
         * [bitmap] as a PNG, scaled down to [maximumWidth] if it is wider, or null if it is empty.
         */
        fun pngOf(bitmap: ImageBitmap, maximumWidth: Int = MAXIMUM_WIDTH): ByteArray? {
            if (bitmap.width <= 0 || bitmap.height <= 0) return null
            val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
            val scaled = if (image.width <= maximumWidth) {
                image
            } else {
                val height = (image.height * maximumWidth.toFloat() / image.width).roundToInt().coerceAtLeast(1)
                Surface.makeRasterN32Premul(maximumWidth, height).use { surface ->
                    surface.canvas.drawImageRect(image, Rect.makeWH(image.width.toFloat(), image.height.toFloat()), Rect.makeWH(maximumWidth.toFloat(), height.toFloat()), SamplingMode.LINEAR, null, true)
                    surface.makeImageSnapshot()
                }
            }
            return scaled.encodeToData(EncodedImageFormat.PNG)?.bytes
        }
    }
}

/**
 * Where the Scene editor offers its [ScenePicture] while it shows a Scene.
 */
internal interface ScenePictureHost {
    fun provide(picture: ScenePicture)

    /**
     * Forgets [picture], unless another Scene editor provided one since.
     */
    fun withdraw(picture: ScenePicture)
}
