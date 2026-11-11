/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelShapes

/**
 * The pixels a stroke of [tool] through [points] (the pixels the pointer passed, in order) turns to [color] on [frame]:
 * only those inside it whose color changes, so a stroke that changes nothing makes no step.
 *
 * @param isFilled Whether a rectangle or an ellipse is filled rather than outlined.
 * @param isContiguous Whether a fill reaches only the pixels touching the first one.
 */
internal fun strokePixels(
    tool: SpriteTool,
    points: List<IntOffset>,
    frame: PixelImage,
    color: Int,
    isFilled: Boolean = false,
    isContiguous: Boolean = true,
): List<Pixel> {
    val first = points.firstOrNull() ?: return emptyList()
    val last = points.last()
    val covered = when (tool) {
        SpriteTool.PENCIL, SpriteTool.ERASER -> listOf(first.x to first.y) + points.zipWithNext { from, to -> PixelShapes.line(from.x, from.y, to.x, to.y) }.flatten()
        SpriteTool.LINE -> PixelShapes.line(first.x, first.y, last.x, last.y)
        SpriteTool.RECTANGLE -> PixelShapes.rectangle(first.x, first.y, last.x, last.y, isFilled)
        SpriteTool.ELLIPSE -> PixelShapes.ellipse(first.x, first.y, last.x, last.y, isFilled)
        SpriteTool.FILL -> PixelShapes.floodFill(frame, first.x, first.y, isContiguous)
        SpriteTool.PICKER, SpriteTool.SELECT -> emptyList()
    }
    val drawn = if (tool == SpriteTool.ERASER) PixelImage.TRANSPARENT else color
    return covered.distinct().filter { (x, y) -> frame.contains(x, y) && frame[x, y] != drawn }.map { (x, y) -> Pixel(x, y, drawn) }
}

/**
 * The selection of [frame] dragged from [from] to [to] (pixels, in any order), both included, cut to the frame, or null
 * when it covers none of it.
 */
internal fun selectionOf(from: IntOffset, to: IntOffset, frame: PixelImage): IntRect? {
    val left = minOf(from.x, to.x).coerceAtLeast(0)
    val top = minOf(from.y, to.y).coerceAtLeast(0)
    val right = (maxOf(from.x, to.x) + 1).coerceAtMost(frame.width)
    val bottom = (maxOf(from.y, to.y) + 1).coerceAtMost(frame.height)
    return if (left < right && top < bottom) IntRect(left, top, right, bottom) else null
}

/**
 * Pixels lifted off a frame (or pasted) that follow the pointer until they are put down: [image] with its top left at
 * [position], and where it was lifted from ([liftedFrom]), which is cleared when it is put down, or null for pasted ones.
 */
internal data class FloatingPixels(
    val image: PixelImage,
    val position: IntOffset,
    val liftedFrom: IntRect? = null,
) {
    val bounds: IntRect get() = IntRect(position.x, position.y, position.x + image.width, position.y + image.height)

    /**
     * The pixels putting these down on [frame] draws: what they were lifted from turned see-through, then each of them,
     * see-through ones too, where they lie inside the frame.
     */
    fun pixelsOn(frame: PixelImage): List<Pixel> {
        val cleared = liftedFrom?.let { area ->
            (area.top until area.bottom).flatMap { y -> (area.left until area.right).map { x -> Pixel(x, y, PixelImage.TRANSPARENT) } }
        }.orEmpty()
        val placed = (0 until image.height).flatMap { y ->
            (0 until image.width).map { x -> Pixel(position.x + x, position.y + y, image[x, y]) }
        }
        return (cleared + placed).filter { frame.contains(it.x, it.y) }
    }
}
