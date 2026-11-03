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

import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.boundsOf
import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.countOf
import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageChecksTest {

    /**
     * Ten by eight pixels of grey, with a red rectangle from (2, 3) to (5, 4) and one almost red pixel at (9, 7).
     */
    private fun picture() = BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 0 until 8) for (x in 0 until 10) setRGB(x, y, 0xFF808080.toInt())
        for (y in 3..4) for (x in 2..5) setRGB(x, y, 0xFFFF0000.toInt())
        setRGB(9, 7, 0xFFF80505.toInt())
    }

    @Test
    fun aColorMatchesWithinTheToleranceOnEveryChannelIgnoringAlpha() {
        assertTrue(ImageChecks.matches(0xFFF80808.toInt(), 0xFF0000))
        assertTrue(ImageChecks.matches(0x00FF0000, 0xFF0000))
        assertFalse(ImageChecks.matches(0xFFF60000.toInt(), 0xFF0000))
        assertFalse(ImageChecks.matches(0xFFFF0009.toInt(), 0xFF0000))
        assertTrue(ImageChecks.matches(0xFFFF0009.toInt(), 0xFF0000, tolerance = 9))
    }

    @Test
    fun pixelsOfAColorAreCountedInTheImageOrARegion() {
        assertEquals(9, picture().countOf(0xFF0000))
        assertEquals(8, picture().countOf(0xFF0000, tolerance = 0))
        assertEquals(4, picture().countOf(0xFF0000, region = Rectangle(0, 0, 4, 5)))
        assertEquals(1, picture().countOf(0xFF0000, region = Rectangle(9, 7, 20, 20)))
        assertEquals(71, picture().countOf(0x808080))
    }

    @Test
    fun theBoundsOfAColorHoldEveryPixelOfIt() {
        assertEquals(Rectangle(2, 3, 4, 2), picture().boundsOf(0xFF0000, tolerance = 0))
        assertEquals(Rectangle(2, 3, 8, 5), picture().boundsOf(0xFF0000))
        assertNull(picture().boundsOf(0x00FF00))
    }

    @Test
    fun theDifferenceIsTheShareOfPixelsThatChanged() {
        val changed = picture().apply { setRGB(0, 0, 0xFF000000.toInt()) }
        assertEquals(0f, ImageChecks.differenceBetween(picture(), picture()))
        assertEquals(1f / 80, ImageChecks.differenceBetween(picture(), changed))
        assertFailsWith<IllegalArgumentException> { ImageChecks.differenceBetween(picture(), BufferedImage(8, 10, BufferedImage.TYPE_INT_ARGB)) }
    }
}
