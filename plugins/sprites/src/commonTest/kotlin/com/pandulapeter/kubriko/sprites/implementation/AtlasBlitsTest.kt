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

/**
 * Carries out the blits on pixel arrays, stretching with nearest-neighbor sampling as the page builder's canvas does,
 * so the padding is checked pixel by pixel without a Skia runtime.
 */
class AtlasBlitsTest {

    @Test
    fun eachPaddingPixelRepeatsTheNearestEdgePixelOfItsOwnSprite() {
        val random = Random(7)
        repeat(30) {
            val count = random.nextInt(1, 40)
            val widths = IntArray(count) { random.nextInt(1, 30) }
            val heights = IntArray(count) { random.nextInt(1, 30) }
            val padding = random.nextInt(0, 4)
            val layout = AtlasPacker.pack(widths, heights, maximumPageSize = 128, padding = padding)

            for (page in 0 until layout.pageCount) {
                val pageWidth = layout.pageWidths[page]
                val pixels = IntArray(pageWidth * layout.pageHeights[page])
                val members = widths.indices.filter { layout.pageIndices[it] == page }
                for (sprite in members) {
                    forEachAtlasBlit(widths[sprite], heights[sprite], layout.xs[sprite], layout.ys[sprite], padding) { srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight ->
                        for (y in 0 until dstHeight) {
                            for (x in 0 until dstWidth) {
                                val sourceX = srcX + x * srcWidth / dstWidth
                                val sourceY = srcY + y * srcHeight / dstHeight
                                pixels[(dstY + y) * pageWidth + dstX + x] = pixelOf(sprite, sourceX, sourceY)
                            }
                        }
                    }
                }
                val expected = IntArray(pixels.size)
                for (sprite in members) {
                    for (y in -padding until heights[sprite] + padding) {
                        for (x in -padding until widths[sprite] + padding) {
                            val sourceX = x.coerceIn(0, widths[sprite] - 1)
                            val sourceY = y.coerceIn(0, heights[sprite] - 1)
                            expected[(layout.ys[sprite] + y) * pageWidth + layout.xs[sprite] + x] = pixelOf(sprite, sourceX, sourceY)
                        }
                    }
                }
                assertEquals(expected.toList(), pixels.toList())
            }
        }
    }

    @Test
    fun withoutPaddingOnlyTheSpriteItselfIsCopied() {
        val blits = mutableListOf<List<Int>>()

        forEachAtlasBlit(5, 3, 7, 9, padding = 0) { srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight ->
            blits += listOf(srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight)
        }

        assertEquals(listOf(listOf(0, 0, 5, 3, 7, 9, 5, 3)), blits)
    }

    /** A pixel value that names its sprite and coordinates, never 0 (the untouched page). */
    private fun pixelOf(sprite: Int, x: Int, y: Int) = ((sprite + 1) shl 16) or (y shl 8) or x
}
