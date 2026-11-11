/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How an image is cut into the frames of an animation, as Kubriko's `AnimatedSprite` plays it: frames of one size, in
 * rows of [framesPerRow] from the top left. Kept in the sprite's file in `.kubriko/sprites` ([SpriteFolderStore]), since
 * the image can't hold it.
 */
@Serializable
@Description("How a sprite's image is cut into the frames of an animation: frames of one size, in rows from the top left.")
public data class SpriteSheet(
    @Description("The width of one frame, in pixels.")
    @SerialName("frameWidth") val frameWidth: Int,
    @Description("The height of one frame, in pixels.")
    @SerialName("frameHeight") val frameHeight: Int,
    @Description("How many frames the animation has.")
    @SerialName("frameCount") val frameCount: Int,
    @Description("How many frames one row of the image holds.")
    @SerialName("framesPerRow") val framesPerRow: Int,
    @Description("How many frames the animation shows in a second, at normal speed.")
    @SerialName("framesPerSecond") val framesPerSecond: Float = DEFAULT_FRAMES_PER_SECOND,
    @Description("The point of a frame its Actor rotates and scales around, in pixels from the frame's left edge, or none for the middle.")
    @SerialName("originX") val originX: Int? = null,
    @Description("The point of a frame its Actor rotates and scales around, in pixels from the frame's top edge, or none for the middle.")
    @SerialName("originY") val originY: Int? = null,
) {
    /**
     * Why the sheet can't be played, or null if it can: every size and count positive, and the origin inside a frame.
     */
    public val problem: SpriteSheetProblem?
        get() = when {
            frameWidth < 1 || frameHeight < 1 -> SpriteSheetProblem.FRAME_SIZE
            frameCount < 1 || framesPerRow < 1 -> SpriteSheetProblem.FRAME_COUNT
            !(framesPerSecond > 0f) || framesPerSecond > MAXIMUM_FRAMES_PER_SECOND -> SpriteSheetProblem.FRAMES_PER_SECOND
            originX?.let { it !in 0..frameWidth } == true || originY?.let { it !in 0..frameHeight } == true -> SpriteSheetProblem.ORIGIN
            else -> null
        }

    /**
     * Where an Actor of [width] by [height] scene units drawn as this sheet turns around and is placed by, from its top
     * left: the origin, scaled from a frame to the Actor, or null when it has none, which is the middle, where Kubriko puts
     * a body's pivot unless told otherwise.
     */
    public fun pivotIn(width: Float, height: Float): Pair<Float, Float>? {
        if (originX == null && originY == null) return null
        val x = originX?.let { width * it / frameWidth } ?: (width / 2)
        val y = originY?.let { height * it / frameHeight } ?: (height / 2)
        return x to y
    }

    /**
     * The number of rows the frames take.
     */
    public val rowCount: Int get() = (frameCount + framesPerRow - 1) / framesPerRow

    /**
     * Why the sheet doesn't fit an image of [width] by [height] pixels, or null if every frame is inside it.
     */
    public fun problemWithImage(width: Int, height: Int): SpriteSheetProblem? = when {
        frameWidth * minOf(framesPerRow, frameCount) > width || frameHeight * rowCount > height -> SpriteSheetProblem.LARGER_THAN_IMAGE
        else -> null
    }

    public companion object {
        public const val DEFAULT_FRAMES_PER_SECOND: Float = 10f
        public const val MAXIMUM_FRAMES_PER_SECOND: Float = 240f

        /**
         * A sheet of one row of square frames as tall as an image of [width] by [height] pixels, which is how most
         * animation strips are laid out.
         */
        public fun guessFor(width: Int, height: Int): SpriteSheet {
            val frameSize = height.coerceIn(1, width.coerceAtLeast(1))
            val frameCount = (width / frameSize).coerceAtLeast(1)
            return SpriteSheet(frameWidth = frameSize, frameHeight = frameSize, frameCount = frameCount, framesPerRow = frameCount)
        }
    }
}
