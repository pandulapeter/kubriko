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

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AtlasPackerTest {

    @Test
    fun aSingleSpriteSitsInsideItsPaddingOnAPageOfItsPaddedSize() {
        val layout = AtlasPacker.pack(intArrayOf(10), intArrayOf(6), maximumPageSize = 64, padding = 2)

        assertEquals(0, layout.pageIndices[0])
        assertEquals(2, layout.xs[0])
        assertEquals(2, layout.ys[0])
        assertEquals(14, layout.pageWidths.single())
        assertEquals(10, layout.pageHeights.single())
    }

    @Test
    fun equalSpritesFillAPageWithoutGaps() {
        val layout = AtlasPacker.pack(IntArray(64) { 16 }, IntArray(64) { 16 }, maximumPageSize = 128, padding = 0)

        assertEquals(1, layout.pageCount)
        assertEquals(128, layout.pageWidths[0])
        assertEquals(128, layout.pageHeights[0])
        assertValid(layout, IntArray(64) { 16 }, IntArray(64) { 16 }, maximumPageSize = 128, padding = 0)
    }

    @Test
    fun spritesThatDontFitOnOnePageSpillOntoTheNext() {
        val widths = IntArray(5) { 60 }
        val heights = IntArray(5) { 60 }

        val layout = AtlasPacker.pack(widths, heights, maximumPageSize = 128, padding = 1)

        assertEquals(2, layout.pageCount)
        assertEquals(4, layout.pageIndices.count { it == 0 })
        assertEquals(1, layout.pageIndices.count { it == 1 })
        assertEquals(62, layout.pageWidths[1])
        assertValid(layout, widths, heights, maximumPageSize = 128, padding = 1)
    }

    @Test
    fun aLaterSmallSpriteFillsAGapOnAnEarlierPage() {
        val widths = intArrayOf(100, 100, 20)
        val heights = intArrayOf(100, 100, 20)

        val layout = AtlasPacker.pack(widths, heights, maximumPageSize = 128, padding = 0)

        assertEquals(2, layout.pageCount)
        assertEquals(0, layout.pageIndices[2])
        assertValid(layout, widths, heights, maximumPageSize = 128, padding = 0)
    }

    @Test
    fun spritesTooLargeForAPagePaddingIncludedOrOfNoSizeAreLeftOffEveryPage() {
        val widths = intArrayOf(62, 63, 10, 0)
        val heights = intArrayOf(10, 10, 63, 5)

        val layout = AtlasPacker.pack(widths, heights, maximumPageSize = 64, padding = 1)

        assertEquals(0, layout.pageIndices[0])
        assertEquals(AtlasLayout.NOT_ON_A_PAGE, layout.pageIndices[1])
        assertEquals(AtlasLayout.NOT_ON_A_PAGE, layout.pageIndices[2])
        assertEquals(AtlasLayout.NOT_ON_A_PAGE, layout.pageIndices[3])
        assertEquals(1, layout.pageCount)
    }

    @Test
    fun noSpritesMakeNoPages() {
        assertEquals(0, AtlasPacker.pack(IntArray(0), IntArray(0), maximumPageSize = 64, padding = 2).pageCount)
    }

    @Test
    fun theLayoutDependsOnTheSizesAloneNotOnTheOrderTheyComeIn() {
        val first = AtlasPacker.pack(intArrayOf(10, 30, 20), intArrayOf(10, 30, 20), maximumPageSize = 64, padding = 1)
        val second = AtlasPacker.pack(intArrayOf(20, 10, 30), intArrayOf(20, 10, 30), maximumPageSize = 64, padding = 1)

        assertEquals(first.xs[1], second.xs[2])
        assertEquals(first.ys[0], second.ys[1])
        assertEquals(first.xs[2], second.xs[0])
    }

    @Test
    fun randomSpriteSetsArePackedWithoutOverlapsWithinTheirPages() {
        val random = Random(42)
        repeat(50) {
            val count = random.nextInt(1, 120)
            val widths = IntArray(count) { random.nextInt(1, 90) }
            val heights = IntArray(count) { random.nextInt(1, 90) }
            val padding = random.nextInt(0, 4)

            val layout = AtlasPacker.pack(widths, heights, maximumPageSize = 256, padding = padding)

            assertValid(layout, widths, heights, maximumPageSize = 256, padding = padding)
            assertTrue(layout.pageIndices.all { it >= 0 })
        }
    }

    @Test
    fun settingsThatLeaveNoRoomAreRefused() {
        assertFailsWith<IllegalArgumentException> { AtlasPacker.pack(intArrayOf(1), intArrayOf(1), maximumPageSize = 4, padding = 2) }
        assertFailsWith<IllegalArgumentException> { AtlasPacker.pack(intArrayOf(1), intArrayOf(1), maximumPageSize = 4, padding = -1) }
        assertFailsWith<IllegalArgumentException> { AtlasPacker.pack(intArrayOf(1), IntArray(0), maximumPageSize = 4, padding = 0) }
    }

    private fun assertValid(layout: AtlasLayout, widths: IntArray, heights: IntArray, maximumPageSize: Int, padding: Int) {
        for (page in 0 until layout.pageCount) {
            assertTrue(layout.pageWidths[page] <= maximumPageSize && layout.pageHeights[page] <= maximumPageSize)
            val members = widths.indices.filter { layout.pageIndices[it] == page }
            assertTrue(members.isNotEmpty(), "Page $page holds nothing")
            assertEquals(layout.pageWidths[page], members.maxOf { layout.xs[it] + widths[it] + padding })
            assertEquals(layout.pageHeights[page], members.maxOf { layout.ys[it] + heights[it] + padding })
            for (first in members) {
                assertTrue(layout.xs[first] - padding >= 0 && layout.ys[first] - padding >= 0)
                for (second in members) {
                    if (first < second) {
                        val isApart = layout.xs[first] + widths[first] + padding <= layout.xs[second] - padding ||
                            layout.xs[second] + widths[second] + padding <= layout.xs[first] - padding ||
                            layout.ys[first] + heights[first] + padding <= layout.ys[second] - padding ||
                            layout.ys[second] + heights[second] + padding <= layout.ys[first] - padding
                        assertTrue(isApart, "Sprites $first and $second overlap on page $page")
                    }
                }
            }
        }
    }
}
