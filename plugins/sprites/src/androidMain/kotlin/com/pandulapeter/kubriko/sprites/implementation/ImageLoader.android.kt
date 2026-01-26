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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.pandulapeter.kubriko.sprites.SpriteResource

internal actual fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int, rotation: SpriteResource.Rotation): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        if (resourceDensity > targetDensity) {
            inDensity = resourceDensity
            inTargetDensity = targetDensity
        }
    }
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size, options)
    return when (rotation) {
        SpriteResource.Rotation.NONE -> bitmap
        SpriteResource.Rotation.DEGREES_90 -> bitmap.rotated(90f)
        SpriteResource.Rotation.DEGREES_180 -> bitmap.rotated(180f)
        SpriteResource.Rotation.DEGREES_270 -> bitmap.rotated(270f)
    }.asImageBitmap()
}

private fun Bitmap.rotated(angle: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(angle) }
    return Bitmap.createBitmap(this, 0, 0, getWidth(), getHeight(), matrix, true)
}