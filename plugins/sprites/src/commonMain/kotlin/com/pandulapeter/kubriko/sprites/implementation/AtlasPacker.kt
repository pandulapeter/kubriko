/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.implementation

/**
 * Lays sprites out on atlas pages from their sizes alone. Each sprite takes a cell of its own size plus [padding] on
 * every side, which the page builder fills with the sprite's edge pixels; cells never overlap, and no page grows past
 * `maximumPageSize` in either direction. Sprites are placed tallest first, each on the first page with room for it, at
 * the lowest and then leftmost spot a skyline of that page allows; a page is as large as what it holds.
 */
internal object AtlasPacker {

    fun pack(widths: IntArray, heights: IntArray, maximumPageSize: Int, padding: Int): AtlasLayout {
        require(widths.size == heights.size) { "Every sprite needs both a width and a height." }
        require(padding >= 0) { "The padding can't be negative." }
        require(maximumPageSize > padding * 2) { "A page must have room for more than its padding." }
        val count = widths.size
        val pageIndices = IntArray(count) { AtlasLayout.NOT_ON_A_PAGE }
        val xs = IntArray(count)
        val ys = IntArray(count)
        val pages = mutableListOf<Skyline>()
        val order = (0 until count).sortedWith(
            compareByDescending<Int> { heights[it] }.thenByDescending { widths[it] }.thenBy { it },
        )
        for (index in order) {
            val cellWidth = widths[index] + padding * 2
            val cellHeight = heights[index] + padding * 2
            if (widths[index] <= 0 || heights[index] <= 0 || cellWidth > maximumPageSize || cellHeight > maximumPageSize) continue
            var pageIndex = 0
            var position = Skyline.NO_ROOM
            while (pageIndex < pages.size) {
                position = pages[pageIndex].place(cellWidth, cellHeight)
                if (position != Skyline.NO_ROOM) break
                pageIndex++
            }
            if (position == Skyline.NO_ROOM) {
                pages.add(Skyline(maximumPageSize))
                position = pages[pageIndex].place(cellWidth, cellHeight)
            }
            pageIndices[index] = pageIndex
            xs[index] = Skyline.xOf(position) + padding
            ys[index] = Skyline.yOf(position) + padding
        }
        return AtlasLayout(
            pageIndices = pageIndices,
            xs = xs,
            ys = ys,
            pageWidths = IntArray(pages.size) { pages[it].usedWidth },
            pageHeights = IntArray(pages.size) { pages[it].usedHeight },
        )
    }
}

/**
 * Where [AtlasPacker] put each sprite: the page ([pageIndices], or [NOT_ON_A_PAGE] for a sprite too large for any page,
 * or of no size) and the top-left corner of the sprite itself on it ([xs] and [ys], inside its padding), plus the size
 * of every page.
 */
internal class AtlasLayout(
    val pageIndices: IntArray,
    val xs: IntArray,
    val ys: IntArray,
    val pageWidths: IntArray,
    val pageHeights: IntArray,
) {
    val pageCount get() = pageWidths.size

    companion object {
        const val NOT_ON_A_PAGE = -1
    }
}

/**
 * The top edge of what a square page of [size] holds, column by column, as segments of equal height sorted by their
 * left edge and covering the whole width.
 */
private class Skyline(private val size: Int) {
    private val segmentXs = mutableListOf(0)
    private val segmentYs = mutableListOf(0)
    private val segmentWidths = mutableListOf(size)

    var usedWidth = 0
        private set
    var usedHeight = 0
        private set

    /** Places a cell of [width] × [height] and returns its top-left corner (see [xOf] and [yOf]), or [NO_ROOM]. */
    fun place(width: Int, height: Int): Long {
        var bestIndex = -1
        var bestX = 0
        var bestY = 0
        var bestBottom = Int.MAX_VALUE
        for (index in segmentXs.indices) {
            val x = segmentXs[index]
            if (x + width > size) break
            val y = topOfSpan(index, width)
            val bottom = y + height
            if (bottom <= size && bottom < bestBottom) {
                bestIndex = index
                bestX = x
                bestY = y
                bestBottom = bottom
            }
        }
        if (bestIndex < 0) return NO_ROOM
        raise(bestIndex, bestX, bestBottom, width)
        usedWidth = maxOf(usedWidth, bestX + width)
        usedHeight = maxOf(usedHeight, bestBottom)
        return (bestX.toLong() shl 32) or bestY.toLong()
    }

    /** The lowest a cell of [width] starting at segment [index] can sit: the highest of the segments it spans. */
    private fun topOfSpan(index: Int, width: Int): Int {
        var y = 0
        var remaining = width
        var current = index
        while (remaining > 0) {
            y = maxOf(y, segmentYs[current])
            remaining -= segmentWidths[current]
            current++
        }
        return y
    }

    private fun raise(index: Int, x: Int, y: Int, width: Int) {
        segmentXs.add(index, x)
        segmentYs.add(index, y)
        segmentWidths.add(index, width)
        val end = x + width
        val next = index + 1
        while (next < segmentXs.size && segmentXs[next] < end) {
            val overlap = end - segmentXs[next]
            if (overlap >= segmentWidths[next]) {
                removeSegment(next)
            } else {
                segmentXs[next] += overlap
                segmentWidths[next] -= overlap
                break
            }
        }
        var current = 0
        while (current < segmentXs.size - 1) {
            if (segmentYs[current] == segmentYs[current + 1]) {
                segmentWidths[current] += segmentWidths[current + 1]
                removeSegment(current + 1)
            } else {
                current++
            }
        }
    }

    private fun removeSegment(index: Int) {
        segmentXs.removeAt(index)
        segmentYs.removeAt(index)
        segmentWidths.removeAt(index)
    }

    companion object {
        const val NO_ROOM = -1L

        fun xOf(position: Long) = (position ushr 32).toInt()

        fun yOf(position: Long) = position.toInt()
    }
}
