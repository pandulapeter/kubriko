/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * What a scenario asserts about a screenshot without a person looking at it: where a color is, how much of it there is,
 * and how much changed since the frame before.
 */
internal object ImageChecks {

    /**
     * How far each of red, green, and blue may be from a color for a pixel to count as that color, which absorbs the
     * rounding of software rendering and of PNG's color handling.
     */
    const val DEFAULT_TOLERANCE = 8

    /**
     * Whether [pixel] (ARGB) is [color] (RGB, alpha ignored) within [tolerance].
     */
    fun matches(pixel: Int, color: Int, tolerance: Int = DEFAULT_TOLERANCE) = channels.all { shift ->
        abs((pixel shr shift and 0xFF) - (color shr shift and 0xFF)) <= tolerance
    }

    /**
     * How many pixels of [region] (the whole image by default) are [color].
     */
    fun BufferedImage.countOf(color: Int, region: Rectangle = bounds, tolerance: Int = DEFAULT_TOLERANCE): Int {
        val area = region.intersection(bounds)
        var count = 0
        for (y in area.y until area.y + area.height) {
            for (x in area.x until area.x + area.width) {
                if (matches(getRGB(x, y), color, tolerance)) count++
            }
        }
        return count
    }

    /**
     * The smallest rectangle holding every pixel that is [color], or null if none is.
     */
    fun BufferedImage.boundsOf(color: Int, tolerance: Int = DEFAULT_TOLERANCE): Rectangle? {
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matches(getRGB(x, y), color, tolerance)) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        return if (left == Int.MAX_VALUE) null else Rectangle(left, top, right - left + 1, bottom - top + 1)
    }

    /**
     * The share of pixels, from 0 to 1, that differ between two images of the same size beyond [tolerance].
     *
     * @throws IllegalArgumentException If their sizes differ, which already says more than a share could.
     */
    fun differenceBetween(first: BufferedImage, second: BufferedImage, tolerance: Int = DEFAULT_TOLERANCE): Float {
        require(first.width == second.width && first.height == second.height) {
            "The images differ in size: ${first.width}×${first.height} and ${second.width}×${second.height}."
        }
        var different = 0
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                if (!matches(first.getRGB(x, y), second.getRGB(x, y), tolerance)) different++
            }
        }
        return different.toFloat() / (first.width * first.height)
    }

    private val BufferedImage.bounds get() = Rectangle(0, 0, width, height)
    private val channels = intArrayOf(16, 8, 0)
}
