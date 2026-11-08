/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Marks this bitmap as finished, so drawing it shares its pixels instead of copying them, and returns it.
 *
 * On the Skia targets (desktop, iOS, and the web) a mutable bitmap is copied into a new image on every draw, and its
 * GPU texture is keyed on that copy, so a mutable bitmap drawn every frame is copied and uploaded every frame. An
 * immutable one is wrapped in place and cached. Decoded images are immutable already; a bitmap drawn into (a baked
 * texture, a mip level, an atlas page) is not, and should be marked the moment it is complete. Nothing may draw into
 * it afterwards. A no-op on Android, whose canvas caches bitmaps by identity anyway.
 */
expect fun ImageBitmap.markImmutable(): ImageBitmap

/**
 * The alpha-weighted mean color of this bitmap, or of the region of it starting at ([left], [top]) and spanning
 * [width] × [height] pixels (clamped to the bitmap), as an ARGB integer: what it reads as when shown too small to make
 * out any detail. Transparent pixels contribute nothing to the color but lower the alpha, and a fully transparent or
 * empty region gives 0.
 *
 * Large regions are sampled on an evenly spaced grid of at most [MEAN_COLOR_SAMPLES_PER_AXIS] pixels per axis rather
 * than read whole. It still reads pixels back, so callers should compute it once and keep the result.
 */
fun ImageBitmap.meanArgb(left: Int = 0, top: Int = 0, width: Int = this.width, height: Int = this.height): Int {
    val startX = maxOf(left, 0)
    val startY = maxOf(top, 0)
    val regionWidth = minOf(left + width, this.width) - startX
    val regionHeight = minOf(top + height, this.height) - startY
    if (regionWidth <= 0 || regionHeight <= 0) return 0
    val columnStep = (regionWidth + MEAN_COLOR_SAMPLES_PER_AXIS - 1) / MEAN_COLOR_SAMPLES_PER_AXIS
    val rowStep = (regionHeight + MEAN_COLOR_SAMPLES_PER_AXIS - 1) / MEAN_COLOR_SAMPLES_PER_AXIS
    val row = IntArray(regionWidth)
    val accumulator = MeanColorAccumulator()
    var y = 0
    while (y < regionHeight) {
        readPixels(row, startX = startX, startY = startY + y, width = regionWidth, height = 1)
        var x = 0
        while (x < regionWidth) {
            accumulator.add(row[x])
            x += columnStep
        }
        y += rowStep
    }
    return accumulator.meanArgb()
}

/**
 * The most pixels per axis [meanArgb] reads, so a large bitmap costs a bounded number of reads.
 */
const val MEAN_COLOR_SAMPLES_PER_AXIS = 64

/** Sums ARGB pixels for [meanArgb], weighting every color channel by its pixel's alpha. */
internal class MeanColorAccumulator {
    private var count = 0L
    private var alphaSum = 0L
    private var redSum = 0L
    private var greenSum = 0L
    private var blueSum = 0L

    fun add(argb: Int) {
        val alpha = argb ushr 24
        count++
        alphaSum += alpha
        redSum += ((argb shr 16) and 0xFF) * alpha
        greenSum += ((argb shr 8) and 0xFF) * alpha
        blueSum += (argb and 0xFF) * alpha
    }

    fun meanArgb(): Int {
        if (alphaSum == 0L) return 0
        val alpha = (alphaSum / count).toInt()
        val red = (redSum / alphaSum).toInt()
        val green = (greenSum / alphaSum).toInt()
        val blue = (blueSum / alphaSum).toInt()
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
