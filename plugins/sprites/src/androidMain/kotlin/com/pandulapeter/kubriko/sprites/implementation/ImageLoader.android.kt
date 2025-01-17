/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.implementation

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        if (resourceDensity > targetDensity) {
            inDensity = resourceDensity
            inTargetDensity = targetDensity
        }
    }
    return BitmapFactory.decodeByteArray(this, 0, size, options).asImageBitmap()
}