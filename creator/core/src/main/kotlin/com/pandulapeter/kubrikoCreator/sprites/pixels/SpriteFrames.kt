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
     * These frames with [operation] applied to frame [index], or to each of them for null; null when the frames it leaves
     * aren't all of one size (one frame of several turned to another size), which a sheet can't hold. A changed frame
     * size changes the sheet's, keeping its origin where it was in the frame, as a part of its size.
     */
    public fun mapped(index: Int?, operation: (PixelImage) -> PixelImage): SpriteFrames? {
        val mapped = frames.mapIndexed { frameIndex, frame -> if (index == null || frameIndex == index) operation(frame) else frame }
        val first = mapped.first()
        if (mapped.any { it.width != first.width || it.height != first.height }) return null
        val resizedSheet = sheet?.takeIf { first.width != frameWidth || first.height != frameHeight }?.let { sheet ->
            sheet.copy(
                frameWidth = first.width,
                frameHeight = first.height,
                originX = sheet.originX?.let { (it.toLong() * first.width / sheet.frameWidth).toInt() },
                originY = sheet.originY?.let { (it.toLong() * first.height / sheet.frameHeight).toInt() },
            )
        }
        return SpriteFrames(image, cutBy, resizedSheet ?: sheet, mapped)
    }

    /**
     * The frames put back together as the sheet lays them out: over the image they came from while their size is the same
     * (so what lies outside every frame stays), or on a new image just large enough for them.
     */
    public fun toImage(): PixelImage {
        val sheet = sheet ?: return frames.single()
        val isCutTheSame = cutBy?.frameWidth == sheet.frameWidth && cutBy.frameHeight == sheet.frameHeight
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
