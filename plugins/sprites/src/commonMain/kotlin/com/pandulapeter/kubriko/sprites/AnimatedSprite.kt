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
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.sprites.SpriteResource.Rotation
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Represents an animated sprite consisting of multiple frames within a single [ImageBitmap], or within one region of an
 * atlas page (see [fromAtlas]).
 *
 * It provides methods for stepping through frames and drawing the current frame, or showing it through a
 * [SpriteLook] ([applyTo]).
 *
 * @param getImageBitmap A function that returns the [ImageBitmap] containing the animation frames.
 * @param frameSize The size of a single frame in pixels.
 * @param frameCount The total number of frames in the animation.
 * @param framesPerRow The number of frames per row in the [ImageBitmap].
 * @param framesPerSecond The target speed of the animation.
 * @param orientation The rotation to be applied to the sprite.
 */
class AnimatedSprite private constructor(
    private val getImageBitmap: () -> ImageBitmap?,
    private val getSheetRegion: (() -> SpriteRegion?)?,
    frameSize: IntSize,
    val frameCount: Int,
    private val framesPerRow: Int,
    private val framesPerSecond: Float,
    private val orientation: Rotation,
) {
    /** Creates an animated sprite whose frames lie in the sheet bitmap [getImageBitmap] returns. */
    constructor(
        getImageBitmap: () -> ImageBitmap?,
        frameSize: IntSize,
        frameCount: Int,
        framesPerRow: Int,
        framesPerSecond: Float = 60f,
        orientation: Rotation = Rotation.NONE,
    ) : this(
        getImageBitmap = getImageBitmap,
        getSheetRegion = null,
        frameSize = frameSize,
        frameCount = frameCount,
        framesPerRow = framesPerRow,
        framesPerSecond = framesPerSecond,
        orientation = orientation,
    )

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
                // Kotlin's % keeps the dividend's sign, so a backward step past frame 0 needs the extra wrap.
                ((_frameIndex % frameCount) + frameCount) % frameCount
            } else {
                if (_frameIndex < 0) 0f else frameCount - 1f
            }
        }
    }

    internal fun getXIndex(frameIndex: Int) = when (orientation) {
        Rotation.NONE ->
            frameIndex % framesPerRow

        Rotation.DEGREES_180 ->
            framesPerRow - 1 - (frameIndex % framesPerRow)

        Rotation.DEGREES_90 ->
            numberOfRows - 1 - frameIndex / framesPerRow

        Rotation.DEGREES_270 ->
            frameIndex / framesPerRow
    }

    internal fun getYIndex(frameIndex: Int) = when (orientation) {
        Rotation.NONE ->
            frameIndex / framesPerRow

        Rotation.DEGREES_180 ->
            numberOfRows - 1 - frameIndex / framesPerRow

        Rotation.DEGREES_90 ->
            frameIndex % framesPerRow

        Rotation.DEGREES_270 ->
            framesPerRow - 1 - frameIndex % framesPerRow
    }

    /**
     * Shows the current frame through [look]: the sheet (or the atlas page holding it) becomes its texture, and the
     * frame the part of it drawn. While the sheet is not loaded, the look's texture is cleared, so it draws nothing.
     * Call it after stepping the animation; it allocates nothing, and changes the look only when the frame changed.
     */
    fun applyTo(look: SpriteLook) {
        val region = getSheetRegion?.invoke()
        val image = if (getSheetRegion == null) getImageBitmap() else region?.page
        if (image == null) {
            look.texture = null
            return
        }
        look.texture = image
        look.srcOffset = frameOffset(frameIndex, region)
        look.srcSize = orientedFrameSize
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
        val region = getSheetRegion?.invoke()
        val image = (if (getSheetRegion == null) getImageBitmap() else region?.page) ?: return
        scope.drawImage(
            image = image,
            srcOffset = frameOffset(frameIndex, region),
            srcSize = orientedFrameSize,
            colorFilter = colorFilter,
            blendMode = blendMode,
            filterQuality = filterQuality,
        )
    }

    /**
     * Draws the current frame of the animation.
     */
    fun draw(
        canvas: Canvas,
        paint: Paint,
    ) {
        val region = getSheetRegion?.invoke()
        val image = (if (getSheetRegion == null) getImageBitmap() else region?.page) ?: return
        canvas.drawImageRect(
            image = image,
            srcOffset = frameOffset(frameIndex, region),
            srcSize = orientedFrameSize,
            paint = paint,
        )
    }

    private fun frameOffset(frameIndex: Int, sheetRegion: SpriteRegion?) = IntOffset(
        x = orientedFrameSize.width * getXIndex(frameIndex) + if (sheetRegion == null) 0 else sheetRegion.offset.x,
        y = orientedFrameSize.height * getYIndex(frameIndex) + if (sheetRegion == null) 0 else sheetRegion.offset.y,
    )

    /** Creates animated sprites from sprite sheets packed onto atlas pages. */
    companion object {
        /**
         * Creates an [AnimatedSprite] whose sheet is a region of an atlas page (see [SpriteAtlas], which packs a sheet
         * whole), so its frames are regions of that page and batched actors showing them share its runs.
         *
         * @param getRegion A function that returns the sheet's region, null while its atlas is loading; typically
         *   `{ spriteManager.getRegion(resource) }`.
         * @param frameSize The size of a single frame in pixels.
         * @param frameCount The total number of frames in the animation.
         * @param framesPerRow The number of frames per row in the sheet.
         * @param framesPerSecond The target speed of the animation.
         * @param orientation The rotation the sheet was loaded with.
         */
        fun fromAtlas(
            getRegion: () -> SpriteRegion?,
            frameSize: IntSize,
            frameCount: Int,
            framesPerRow: Int,
            framesPerSecond: Float = 60f,
            orientation: Rotation = Rotation.NONE,
        ) = AnimatedSprite(
            getImageBitmap = { getRegion()?.page },
            getSheetRegion = getRegion,
            frameSize = frameSize,
            frameCount = frameCount,
            framesPerRow = framesPerRow,
            framesPerSecond = framesPerSecond,
            orientation = orientation,
        )
    }
}
