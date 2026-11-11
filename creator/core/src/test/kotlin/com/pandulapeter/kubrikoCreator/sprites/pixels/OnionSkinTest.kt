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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnionSkinTest {
    private val white = 0xFFFFFFFF.toInt()
    private fun frameWith(x: Int) = PixelImage(4, 1).with(listOf(Pixel(x, 0, white)))
    private val frames = listOf(frameWith(0), frameWith(1), frameWith(2), frameWith(3))

    private fun alphaOf(color: Int) = color ushr 24
    private fun redOf(color: Int) = color ushr 16 and 0xFF
    private fun blueOf(color: Int) = color and 0xFF

    @Test
    fun theFramesBeforeAreReddishAndTheFramesAfterBluishAndFaint() {
        val skin = OnionSkin.of(frames, index = 1, reach = 1, opacity = 0.5f)!!
        assertEquals(PixelImage.TRANSPARENT, skin[1, 0], "the frame shown isn't part of it")
        assertEquals(128, alphaOf(skin[0, 0]))
        assertTrue(redOf(skin[0, 0]) > blueOf(skin[0, 0]))
        assertTrue(blueOf(skin[2, 0]) > redOf(skin[2, 0]))
        assertEquals(PixelImage.TRANSPARENT, skin[3, 0], "two frames away is past its reach")
    }

    @Test
    fun fartherFramesAreFainterAndItDoesntGoRound() {
        val skin = OnionSkin.of(frames, index = 3, reach = 3, opacity = 0.6f)!!
        assertTrue(alphaOf(skin[2, 0]) > alphaOf(skin[1, 0]))
        assertTrue(alphaOf(skin[1, 0]) > alphaOf(skin[0, 0]))
        assertEquals(PixelImage.TRANSPARENT, skin[3, 0])
    }

    @Test
    fun nearerFramesGoOverFartherOnes() {
        val same = listOf(frameWith(0), frameWith(0), frameWith(0))
        val skin = OnionSkin.of(same, index = 2, reach = 2, opacity = 0.5f)!!
        assertTrue(alphaOf(skin[0, 0]) > 128, "both show, one over the other")
    }

    @Test
    fun thereIsNothingWithoutOtherFramesOrStrength() {
        assertNull(OnionSkin.of(frames.take(1), index = 0, reach = 3, opacity = 0.5f))
        assertNull(OnionSkin.of(frames, index = 1, reach = 0, opacity = 0.5f))
        assertNull(OnionSkin.of(frames, index = 1, reach = 2, opacity = 0f))
        assertNull(OnionSkin.of(listOf(PixelImage(2, 1), PixelImage(2, 1)), index = 0, reach = 1, opacity = 0.5f))
    }
}
