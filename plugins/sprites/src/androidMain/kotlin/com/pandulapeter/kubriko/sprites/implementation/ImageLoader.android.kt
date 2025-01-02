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