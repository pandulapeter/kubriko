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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.helpers.extensions.markImmutable
import com.pandulapeter.kubriko.helpers.extensions.meanArgb

/**
 * Smaller copies of a texture, each half the size of the one before it, for drawing it smaller than its own size.
 *
 * `drawImage` samples the full-resolution bitmap however small it shows on screen, so a texture drawn at a fraction of
 * its size reads far more pixels than it shows and shimmers as it moves. Drawing the [level] that [forScale] picks
 * instead keeps both in check. Levels are built on first use, each once, by halving the one before it, and are marked
 * immutable (see [markImmutable]). Share one chain between everything drawing the same texture, through
 * [TextureMipChainCache] or [SpriteManager.getMipChain], so each level is built only once.
 *
 * A level's size is its predecessor's halved and rounded down (never below one pixel), so a region of the [base] in
 * pixels maps onto level `k` by dividing it by `2^k`.
 *
 * Not thread-safe: build and read levels from one thread, typically the one drawing.
 *
 * @param base The full-resolution texture, level 0. It is marked immutable.
 * @param levelCount How many levels the chain holds, the base included; at least 1.
 */
class TextureMipChain internal constructor(
    base: ImageBitmap,
    val levelCount: Int,
    private val downscale: (source: ImageBitmap, width: Int, height: Int) -> ImageBitmap,
    private val finish: (ImageBitmap) -> ImageBitmap,
) {

    /**
     * Creates a chain over [base] holding [levelCount] levels, the base included.
     */
    constructor(base: ImageBitmap, levelCount: Int = DEFAULT_LEVEL_COUNT) : this(base, levelCount, ::downscaleWithCanvas, ImageBitmap::markImmutable)

    init {
        require(levelCount >= 1) { "A mip chain holds at least its base level." }
    }

    /** The full-resolution texture, level 0. */
    val base = finish(base)

    private val levels = arrayOfNulls<ImageBitmap>(levelCount).also { it[0] = this.base }

    /**
     * The texture's alpha-weighted mean color as ARGB (see [meanArgb]), read once from the smallest level: what the
     * texture should be drawn as when it is too small on screen to show any of itself.
     */
    val meanArgb: Int by lazy { level(levelCount - 1).meanArgb() }

    /**
     * The level at [index] (0 is the [base]), built now if it hasn't been yet. Once a level has shrunk to a single
     * pixel the ones after it are that same bitmap.
     */
    fun level(index: Int): ImageBitmap {
        levels[index]?.let { return it }
        val source = level(index - 1)
        val built = if (source.width <= 1 && source.height <= 1) {
            source
        } else {
            downscale(source, (source.width / 2).coerceAtLeast(1), (source.height / 2).coerceAtLeast(1)).let(finish)
        }
        levels[index] = built
        return built
    }

    /**
     * The level for drawing the texture at [scale] screen pixels per texel of the [base]: the smallest one that still
     * has at least a texel per pixel. See [levelIndexForScale].
     */
    fun forScale(scale: Float): ImageBitmap = level(levelIndexForScale(scale))

    /**
     * The index of the level [forScale] picks: level `k` once [scale] is at most `1 / 2^k`, so a texture drawn at
     * half its size or less takes level 1, at a quarter or less level 2, and so on up to the last level.
     */
    fun levelIndexForScale(scale: Float): Int {
        var index = 0
        var threshold = 0.5f
        while (index < levelCount - 1 && scale <= threshold) {
            index++
            threshold *= 0.5f
        }
        return index
    }

    companion object {
        /** The number of levels a chain holds unless told otherwise: the base, a half, and a quarter. */
        const val DEFAULT_LEVEL_COUNT = 3
    }
}

private val downscalePaint = Paint().apply {
    color = Color.White
    filterQuality = FilterQuality.Low
}

/** Halving with bilinear filtering averages each two-by-two block, which is what a mip level is. */
private fun downscaleWithCanvas(source: ImageBitmap, width: Int, height: Int): ImageBitmap {
    val target = ImageBitmap(width, height)
    Canvas(target).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = downscalePaint,
    )
    return target
}
