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

class PixelOperationsTest {
    private val image = image("abc", "def")

    @Test
    fun anImageIsFlippedTurnedAndShiftedAround() {
        assertEquals(listOf("cba", "fed"), PixelOperations.flippedHorizontally(image).rows())
        assertEquals(listOf("def", "abc"), PixelOperations.flippedVertically(image).rows())
        assertEquals(listOf("da", "eb", "fc"), PixelOperations.rotated(image, isClockwise = true).rows())
        assertEquals(listOf("cf", "be", "ad"), PixelOperations.rotated(image, isClockwise = false).rows())
        assertEquals(listOf("fde", "cab"), PixelOperations.shifted(image, 1, -1).rows())
    }

    @Test
    fun aCanvasGrowsOrShrinksAroundItsAnchorAndAnImageScalesByWholePixels() {
        assertEquals(listOf(".....", ".abc.", ".def."), PixelOperations.resizedCanvas(image, 5, 3, CanvasAnchor.BOTTOM).rows())
        assertEquals(listOf("ab"), PixelOperations.resizedCanvas(image, 2, 1, CanvasAnchor.TOP_LEFT).rows())
        assertEquals(listOf("f"), PixelOperations.resizedCanvas(image, 1, 1, CanvasAnchor.BOTTOM_RIGHT).rows())
        assertEquals(listOf("aabbcc", "aabbcc", "ddeeff", "ddeeff"), PixelOperations.scaled(image, 2).rows())
    }
}
