/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites.pixels

import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet

/**
 * A sprite's image cut into the frames its [sheet] says, or one frame, the whole image, without a sheet: what the editor
 * and the AI's drawing tools work on, frame by frame, and put back together into the one image the game reads.
 */
public class SpriteFrames private constructor(
    private val image: PixelImage,
    private val cutBy: SpriteSheet?,
    public val sheet: SpriteSheet?,
    public val frames: List<PixelImage>,
) {
    public val frameWidth: Int get() = frames.first().width
    public val frameHeight: Int get() = frames.first().height

    /**
     * These frames with frame [index] replaced by [frame], of the same size.
     */
    public fun withFrame(index: Int, frame: PixelImage): SpriteFrames {
        require(frame.width == frameWidth && frame.height == frameHeight) { "A frame is $frameWidth by $frameHeight pixels." }
        return SpriteFrames(image, cutBy, sheet, frames.toMutableList().also { it[index] = frame })
    }

    /**
     * [frames] in place of these, of the same size, laid out [framesPerRow] to a row: frames added, removed, or put in
     * another order. A sprite not cut into frames becomes one that is, played at the default speed.
     */
    public fun withFrames(frames: List<PixelImage>, framesPerRow: Int): SpriteFrames {
        require(frames.isNotEmpty() && frames.all { it.width == frameWidth && it.height == frameHeight }) { "A sprite has frames of $frameWidth by $frameHeight pixels." }
        val changedSheet = (sheet ?: SpriteSheet(frameWidth = frameWidth, frameHeight = frameHeight, frameCount = 1, framesPerRow = 1))
            .copy(frameCount = frames.size, framesPerRow = framesPerRow.coerceIn(1, frames.size))
        return SpriteFrames(image, cutBy, changedSheet, frames)
    }

    /**
     * These frames played at [framesPerSecond] (their sheet's speed), or these unchanged when they aren't cut into frames.
     */
    public fun withFramesPerSecond(framesPerSecond: Float): SpriteFrames = sheet?.let { SpriteFrames(image, cutBy, it.copy(framesPerSecond = framesPerSecond), frames) } ?: this

    /**
     * These frames with [operation] applied to frame [index], or to each of them for null; null when the frames it leaves
     * aren't all of one size (one frame of several turned to another size), which a sheet can't hold. A changed frame
     * size changes the sheet's. When every frame changes (or the only one), the sheet's origin follows the pixels as [origin] moves it (given
     * the sheet as it was); otherwise it stays, since the frames share it.
     */
    public fun mapped(
        index: Int?,
        origin: (SpriteSheet) -> Pair<Int?, Int?> = { it.originX to it.originY },
        operation: (PixelImage) -> PixelImage,
    ): SpriteFrames? {
        val mapped = frames.mapIndexed { frameIndex, frame -> if (index == null || frameIndex == index) operation(frame) else frame }
        val first = mapped.first()
        if (mapped.any { it.width != first.width || it.height != first.height }) return null
        val changedSheet = sheet?.let { sheet ->
            val (originX, originY) = if (index == null || frames.size == 1) origin(sheet) else sheet.originX to sheet.originY
            sheet.copy(
                frameWidth = first.width,
                frameHeight = first.height,
                originX = originX?.coerceIn(0, first.width),
                originY = originY?.coerceIn(0, first.height),
            )
        }
        return SpriteFrames(image, cutBy, changedSheet, mapped)
    }

    /**
     * The frames put back together as the sheet lays them out: over the image they came from while they are laid out the
     * same (so what lies outside every frame stays), or on a new image just large enough for them.
     */
    public fun toImage(): PixelImage {
        val sheet = sheet ?: return frames.single()
        val isCutTheSame = cutBy?.frameWidth == sheet.frameWidth && cutBy.frameHeight == sheet.frameHeight &&
            cutBy.frameCount == sheet.frameCount && cutBy.framesPerRow == sheet.framesPerRow
        val base = if (isCutTheSame && sheet.problemWithImage(image.width, image.height) == null) {
            image
        } else {
            PixelImage(sheet.frameWidth * minOf(sheet.framesPerRow, sheet.frameCount), sheet.frameHeight * sheet.rowCount)
        }
        return frames.foldIndexed(base) { index, result, frame ->
            result.withPasted(frame, (index % sheet.framesPerRow) * sheet.frameWidth, (index / sheet.framesPerRow) * sheet.frameHeight)
        }
    }

    public companion object {
        /**
         * [image] cut into frames as [sheet] says (frames reaching past it are see-through there), or one frame without one.
         */
        public fun of(image: PixelImage, sheet: SpriteSheet?): SpriteFrames = SpriteFrames(
            image = image,
            cutBy = sheet,
            sheet = sheet,
            frames = if (sheet == null) {
                listOf(image)
            } else {
                List(sheet.frameCount) { index ->
                    image.cropped((index % sheet.framesPerRow) * sheet.frameWidth, (index / sheet.framesPerRow) * sheet.frameHeight, sheet.frameWidth, sheet.frameHeight)
                }
            },
        )
    }
}
