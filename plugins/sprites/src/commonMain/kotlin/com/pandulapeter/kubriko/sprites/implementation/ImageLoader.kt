package com.pandulapeter.kubriko.sprites.implementation

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int): ImageBitmap

internal expect fun String.processUri() : String