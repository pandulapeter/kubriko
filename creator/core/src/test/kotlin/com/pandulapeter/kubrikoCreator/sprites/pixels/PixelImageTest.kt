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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PixelImageTest {

    @Test
    fun pixelsAreSetCutAndPastedAndWhatFallsOutsideIsLeftOut() {
        val image = image("ab", "cd")
        assertEquals(listOf("zb", "cd"), image.with(listOf(Pixel(0, 0, colorOf('z')), Pixel(5, 5, colorOf('y')))).rows())
        assertEquals(listOf("ab", "cd"), image.rows(), "a change makes a copy")
        assertEquals(listOf("d.", ".."), image.cropped(1, 1, 2, 2).rows())
        assertEquals(listOf("..a", "..c"), image("...", "...").withPasted(image, 2, 0).rows())
    }

    @Test
    fun aPngComesBackAsTheSamePixelsSeeThroughOnesToo() {
        val image = PixelImage(2, 1, intArrayOf(0x80FF0000.toInt(), PixelImage.TRANSPARENT))
        assertEquals(image, PixelImage.of(image.toPng()))
        assertNull(PixelImage.of(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun theColorsUsedAreTheMostUsedFirstWithoutSeeThroughOnes() {
        assertEquals(listOf(colorOf('b'), colorOf('a')), image("ab.", "bb.").colors())
    }

    @Test
    fun anImageHasPixels() {
        assertFailsWith<IllegalArgumentException> { PixelImage(0, 4) }
        assertFailsWith<IllegalArgumentException> { PixelImage(2, 2, IntArray(3)) }
    }
}
