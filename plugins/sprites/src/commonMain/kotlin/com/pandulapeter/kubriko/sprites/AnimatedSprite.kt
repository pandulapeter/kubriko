package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

class AnimatedSprite(
    private val getImageBitmap: () -> ImageBitmap?,
    private val frameSize: IntSize,
    private val frameCount: Int,
    private val framesPerColumn: Int, // TODO: Support different orientations
    private val framesPerSecond: Float = 60f,
) {
    private var imageIndex = 0f

    fun stepForward(
        deltaTimeInMillis: Float,
        speed: Float = 1f,
        shouldLoop: Boolean = true,
    ) {
        imageIndex += (speed * framesPerSecond * deltaTimeInMillis) / 1000
        normalizeImageIndex(shouldLoop)
    }

    fun stepBackwards(
        deltaTimeInMillis: Float,
        speed: Float = 1f,
        shouldLoop: Boolean = true,
    ) {
        imageIndex -= (speed * framesPerSecond * deltaTimeInMillis) / 1000
        normalizeImageIndex(shouldLoop)
    }

    private fun normalizeImageIndex(shouldLoop: Boolean) {
        if (imageIndex >= frameCount || imageIndex < 0) {
            imageIndex = if (shouldLoop) {
                imageIndex % frameCount
            } else {
                if (imageIndex < 0) 0f else frameCount.toFloat()
            }
            if (imageIndex < 0) {
                imageIndex += frameCount
            }
        }
    }

    fun draw(scope: DrawScope) {
        getImageBitmap()?.let {
            val x = imageIndex.roundToInt() / framesPerColumn
            val y = imageIndex.roundToInt() % framesPerColumn
            scope.drawImage(
                image = it,
                srcOffset = IntOffset(frameSize.width * x, frameSize.height * y),
                srcSize = frameSize,
            )
        }
    }
}