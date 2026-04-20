/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.sprites.SpriteResource.Rotation
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Represents an animated sprite consisting of multiple frames within a single [ImageBitmap].
 *
 * It provides methods for stepping through frames and drawing the current frame.
 *
 * @param getImageBitmap A function that returns the [ImageBitmap] containing the animation frames.
 * @param frameSize The size of a single frame in pixels.
 * @param frameCount The total number of frames in the animation.
 * @param framesPerRow The number of frames per row in the [ImageBitmap].
 * @param framesPerSecond The target speed of the animation.
 * @param orientation The rotation to be applied to the sprite.
 */
class AnimatedSprite(
    private val getImageBitmap: () -> ImageBitmap?,
    frameSize: IntSize,
    val frameCount: Int,
    private val framesPerRow: Int,
    private val framesPerSecond: Float = 60f,
    private val orientation: Rotation = Rotation.NONE,
) {
    /**
     * Whether the [ImageBitmap] has been loaded.
     */
    val isLoaded get() = getImageBitmap() != null
    private val numberOfRows = ceil(frameCount / framesPerRow.toFloat()).toInt()
    private val orientedFrameSize = when (orientation) {
        Rotation.NONE,
        Rotation.DEGREES_180 -> frameSize

        Rotation.DEGREES_90,
        Rotation.DEGREES_270 -> IntSize(width = frameSize.height, height = frameSize.width)

    }
    private var _frameIndex = 0f

    /**
     * The index of the current frame.
     */
    var frameIndex
        get() = floor(_frameIndex).roundToInt()
        set(value) {
            _frameIndex = value.toFloat()
        }

    /**
     * Whether the current frame is the last frame in the animation.
     */
    val isLastFrame get() = frameIndex == frameCount - 1

    /**
     * Whether the current frame is the first frame in the animation.
     */
    val isFirstFrame get() = frameIndex == 0

    /**
     * Advances the animation frame based on the elapsed time.
     *
     * @param deltaTimeInMilliseconds The time elapsed since the last update.
     * @param speed The speed multiplier for the animation.
     * @param shouldLoop Whether the animation should restart when it reaches the last frame.
     */
    fun stepForward(
        deltaTimeInMilliseconds: Int,
        speed: Float = 1f,
        shouldLoop: Boolean = false,
    ) {
        _frameIndex += (speed * framesPerSecond * deltaTimeInMilliseconds) / 1000
        normalizeImageIndex(shouldLoop)
    }

    /**
     * Reverses the animation frame based on the elapsed time.
     *
     * @param deltaTimeInMilliseconds The time elapsed since the last update.
     * @param speed The speed multiplier for the animation.
     * @param shouldLoop Whether the animation should restart when it reaches the first frame.
     */
    fun stepBackwards(
        deltaTimeInMilliseconds: Int,
        speed: Float = 1f,
        shouldLoop: Boolean = false,
    ) {
        _frameIndex -= (speed * framesPerSecond * deltaTimeInMilliseconds) / 1000
        normalizeImageIndex(shouldLoop)
    }

    private fun normalizeImageIndex(shouldLoop: Boolean) {
        if (frameIndex >= frameCount - 1 || frameIndex < 0) {
            _frameIndex = if (shouldLoop) {
                _frameIndex % frameCount
            } else {
                if (_frameIndex < 0) 0f else frameCount - 1f
            }
        }
    }

    private fun getOrientedFramePosition(frameIndex: Int): Pair<Int, Int> = when (orientation) {
        Rotation.NONE ->
            frameIndex % framesPerRow to frameIndex / framesPerRow

        Rotation.DEGREES_180 ->
            framesPerRow - 1 - (frameIndex % framesPerRow) to numberOfRows - 1 - frameIndex / framesPerRow

        Rotation.DEGREES_90 ->
            numberOfRows - 1 - frameIndex / framesPerRow to frameIndex % framesPerRow

        Rotation.DEGREES_270 ->
            frameIndex / framesPerRow to framesPerRow - 1 - frameIndex % framesPerRow

    }

    private fun getXIndex(frameIndex: Int) = when (orientation) {
        Rotation.NONE ->
            frameIndex % framesPerRow

        Rotation.DEGREES_180 ->
            framesPerRow - 1 - (frameIndex % framesPerRow)

        Rotation.DEGREES_90 ->
            numberOfRows - 1 - frameIndex / framesPerRow

        Rotation.DEGREES_270 ->
            frameIndex / framesPerRow
    }

    private fun getYIndex(frameIndex: Int) = when (orientation) {
        Rotation.NONE ->
            frameIndex / framesPerRow

        Rotation.DEGREES_180 ->
            numberOfRows - 1 - frameIndex / framesPerRow

        Rotation.DEGREES_90 ->
            frameIndex % framesPerRow

        Rotation.DEGREES_270 ->
            framesPerRow - 1 - frameIndex / framesPerRow
    }

    /**
     * Draws the current frame of the animation.
     */
    fun draw(
        scope: DrawScope,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality,
    ) {
        getImageBitmap()?.let {
            frameIndex.also { index ->
                scope.drawImage(
                    image = it,
                    srcOffset = IntOffset(
                        orientedFrameSize.width * getXIndex(index),
                        orientedFrameSize.height * getYIndex(index)
                    ),
                    srcSize = orientedFrameSize,
                    colorFilter = colorFilter,
                    blendMode = blendMode,
                    filterQuality = filterQuality,
                )
            }
        }
    }

    /**
     * Draws the current frame of the animation.
     */
    fun draw(
        canvas: Canvas,
        paint: Paint,
    ) {
        getImageBitmap()?.let {
            frameIndex.also { index ->
                canvas.drawImageRect(
                    image = it,
                    srcOffset = IntOffset(
                        orientedFrameSize.width * getXIndex(index),
                        orientedFrameSize.height * getYIndex(index)
                    ),
                    srcSize = orientedFrameSize,
                    paint = paint,
                )
            }
        }
    }
}
