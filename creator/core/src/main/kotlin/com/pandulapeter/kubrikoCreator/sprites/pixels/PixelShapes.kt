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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The pixels the sprite editor's tools cover, as points (`x` to `y`) for the pixels the stroke turns to one color, so the
 * tools and the AI's drawing share one path ([PixelImage.with]).
 */
public object PixelShapes {

    /**
     * The pixels of a line from [x0], [y0] to [x1], [y1], both ends included, one per step along its longer side.
     */
    public fun line(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> {
        val points = ArrayList<Pair<Int, Int>>()
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val stepX = if (x0 < x1) 1 else -1
        val stepY = if (y0 < y1) 1 else -1
        var x = x0
        var y = y0
        var error = dx + dy
        while (true) {
            points += x to y
            if (x == x1 && y == y1) return points
            val doubled = 2 * error
            if (doubled >= dy) {
                error += dy
                x += stepX
            }
            if (doubled <= dx) {
                error += dx
                y += stepY
            }
        }
    }

    /**
     * The pixels of the rectangle with the corners [x0], [y0] and [x1], [y1] (in any order), its outline or all of it.
     */
    public fun rectangle(x0: Int, y0: Int, x1: Int, y1: Int, isFilled: Boolean): List<Pair<Int, Int>> {
        val left = min(x0, x1)
        val right = max(x0, x1)
        val top = min(y0, y1)
        val bottom = max(y0, y1)
        return buildList {
            for (y in top..bottom) {
                for (x in left..right) {
                    if (isFilled || x == left || x == right || y == top || y == bottom) add(x to y)
                }
            }
        }
    }

    /**
     * The pixels of the ellipse inside the rectangle with the corners [x0], [y0] and [x1], [y1] (in any order), its outline
     * or all of it: a pixel is inside when its middle is, and on the outline when one of its four neighbours isn't.
     */
    public fun ellipse(x0: Int, y0: Int, x1: Int, y1: Int, isFilled: Boolean): List<Pair<Int, Int>> {
        val left = min(x0, x1)
        val right = max(x0, x1)
        val top = min(y0, y1)
        val bottom = max(y0, y1)
        val centerX = (left + right) / 2.0
        val centerY = (top + bottom) / 2.0
        val radiusX = (right - left + 1) / 2.0
        val radiusY = (bottom - top + 1) / 2.0
        fun isInside(x: Int, y: Int): Boolean {
            if (x !in left..right || y !in top..bottom) return false
            val normalizedX = (x - centerX) / radiusX
            val normalizedY = (y - centerY) / radiusY
            return normalizedX * normalizedX + normalizedY * normalizedY <= 1.0
        }
        return buildList {
            for (y in top..bottom) {
                for (x in left..right) {
                    if (!isInside(x, y)) continue
                    if (isFilled || !isInside(x - 1, y) || !isInside(x + 1, y) || !isInside(x, y - 1) || !isInside(x, y + 1)) add(x to y)
                }
            }
        }
    }

    /**
     * The pixels a flood fill from [x], [y] of [image] turns: those of the color there that touch it through others of
     * that color (side by side, not corner to corner), or every pixel of that color when it isn't [isContiguous]. None
     * outside the image.
     */
    public fun floodFill(image: PixelImage, x: Int, y: Int, isContiguous: Boolean = true): List<Pair<Int, Int>> {
        if (!image.contains(x, y)) return emptyList()
        val target = image[x, y]
        if (!isContiguous) {
            return buildList {
                for (pixelY in 0 until image.height) for (pixelX in 0 until image.width) if (image[pixelX, pixelY] == target) add(pixelX to pixelY)
            }
        }
        val isVisited = BooleanArray(image.width * image.height)
        val queue = ArrayDeque<Int>()
        val points = ArrayList<Pair<Int, Int>>()
        queue += y * image.width + x
        isVisited[y * image.width + x] = true
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val pixelX = index % image.width
            val pixelY = index / image.width
            points += pixelX to pixelY
            for ((neighbourX, neighbourY) in listOf(pixelX - 1 to pixelY, pixelX + 1 to pixelY, pixelX to pixelY - 1, pixelX to pixelY + 1)) {
                if (!image.contains(neighbourX, neighbourY)) continue
                val neighbour = neighbourY * image.width + neighbourX
                if (!isVisited[neighbour] && image[neighbourX, neighbourY] == target) {
                    isVisited[neighbour] = true
                    queue += neighbour
                }
            }
        }
        return points
    }
}
