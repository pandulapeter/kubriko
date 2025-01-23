/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.roundToInt

class AnimatedSprite(
    private val getImageBitmap: () -> ImageBitmap?,
    private val frameSize: IntSize,
    private val frameCount: Int,
    private val framesPerRow: Int, // TODO: Support different orientations
    private val framesPerSecond: Float = 60f,
) {
    val isLoaded get() = getImageBitmap() != null
    private var _imageIndex = 0f
    var imageIndex
        get() = floor(_imageIndex).roundToInt()
        set(value) {
            _imageIndex = value.toFloat()
        }
    val isLastFrame get() = imageIndex == frameCount - 1
    val isFirstFrame get() = imageIndex == 0

    // TODO: Support reverse by adding a step function

    fun stepForward(
        deltaTimeInMilliseconds: Int,
        speed: Float = 1f,
        shouldLoop: Boolean = false,
    ) {
        _imageIndex += (speed * framesPerSecond * deltaTimeInMilliseconds) / 1000
        normalizeImageIndex(shouldLoop)
    }

    fun stepBackwards(
        deltaTimeInMilliseconds: Int,
        speed: Float = 1f,
        shouldLoop: Boolean = false,
    ) {
        _imageIndex -= (speed * framesPerSecond * deltaTimeInMilliseconds) / 1000
        normalizeImageIndex(shouldLoop)
    }

    private fun normalizeImageIndex(shouldLoop: Boolean) {
        if (imageIndex >= frameCount - 1 || imageIndex < 0) {
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
        imageIndex.also { imageIndex ->
            getImageBitmap()?.let {
                val x = imageIndex % framesPerRow
                val y = imageIndex / framesPerRow
                scope.drawImage(
                    image = it,
                    srcOffset = IntOffset(frameSize.width * x, frameSize.height * y),
                    srcSize = frameSize,
                    colorFilter = colorFilter,
                )
            }
        }
    }
}