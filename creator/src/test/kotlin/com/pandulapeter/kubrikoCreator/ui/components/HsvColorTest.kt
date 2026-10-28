/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class HsvColorTest {

    @Test
    fun colorsSurviveTheRoundTrip() {
        listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF6060AA, 0x80203040, 0xFFFFFFFF, 0xFF000000, 0x00000000, 0xFF5AB06A, 0xFFFFFF00).forEach { argb ->
            assertEquals(argb, HsvColor.fromArgb(argb).toArgb(), "%08X".format(argb))
        }
    }

    @Test
    fun hueSaturationAndBrightnessAreRead() {
        assertEquals(HsvColor(0f, 1f, 1f), HsvColor.fromArgb(0xFFFF0000))
        assertEquals(HsvColor(120f, 1f, 1f), HsvColor.fromArgb(0xFF00FF00))
        assertEquals(HsvColor(240f, 1f, 0.5019608f, alpha = 0.5019608f), HsvColor.fromArgb(0x80000080))
    }

    @Test
    fun aGrayKeepsTheHueItIsGiven() {
        assertEquals(200f, HsvColor.fromArgb(0xFF808080, hue = 200f).hue)
        assertEquals(0f, HsvColor.fromArgb(0xFF808080).saturation)
    }

    @Test
    fun theWholeHueCircleIsCovered() {
        assertEquals(0xFFFF0000, HsvColor(360f, 1f, 1f).toArgb())
        assertEquals(0xFF00FFFF, HsvColor(180f, 1f, 1f).toArgb())
        assertEquals(0xFFFF00FF, HsvColor(300f, 1f, 1f).toArgb())
    }
}
