package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

class AnimatedSprite(
    private val getImageBitmap: () -> ImageBitmap?,
    private val frameSize: IntSize,
    private val frameCount: Int,
    private val framesPerRow: Int, // TODO: Support different orientations
    private val framesPerSecond: Float = 60f,
) {
    private var _imageIndex = 0f
    var imageIndex
        get() = _imageIndex.roundToInt()
        set(value) {
            _imageIndex = value.toFloat()
        }

    fun stepForward(
        deltaTimeInMilliseconds: Float,
        speed: Float = 1f,
        shouldLoop: Boolean = true,
    ) {
        _imageIndex += (speed * framesPerSecond * deltaTimeInMilliseconds) / 1000
        normalizeImageIndex(shouldLoop)
    }

    fun stepBackwards(
        deltaTimeInMilliseconds: Float,
        speed: Float = 1f,
        shouldLoop: Boolean = true,
    ) {
        _imageIndex -= (speed * framesPerSecond * deltaTimeInMilliseconds) / 1000
        normalizeImageIndex(shouldLoop)
    }

    private fun normalizeImageIndex(shouldLoop: Boolean) {
        if (_imageIndex >= frameCount || _imageIndex < 0) {
            _imageIndex = if (shouldLoop) {
                _imageIndex % frameCount
            } else {
                if (_imageIndex < 0) 0f else frameCount - 1f
            }
        }
    }

    fun draw(
        scope: DrawScope,
        colorFilter: ColorFilter? = null,
    ) {
        getImageBitmap()?.let {
            val x = _imageIndex.roundToInt() % framesPerRow
            val y = _imageIndex.roundToInt() / framesPerRow
            scope.drawImage(
                image = it,
                srcOffset = IntOffset(frameSize.width * x, frameSize.height * y),
                srcSize = frameSize,
                colorFilter = colorFilter,
            )
        }
    }
}