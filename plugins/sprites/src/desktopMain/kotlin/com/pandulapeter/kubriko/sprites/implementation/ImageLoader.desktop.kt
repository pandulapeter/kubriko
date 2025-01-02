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
    //https://youtrack.jetbrains.com/issue/CMP-5657
    //android only downscales drawables. If there is only low dpi resource then use it as is (not upscale)
    //we need a consistent behavior on all platforms
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

internal actual fun String.processUri() = substringAfter("!/")