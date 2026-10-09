/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorExtensionsTest {

    private fun assertHsv(hue: Float, saturation: Float, value: Float, color: Color) {
        val (actualHue, actualSaturation, actualValue) = color.toHSV()
        assertEquals(hue, actualHue, 1e-3f, "hue of $color")
        assertEquals(saturation, actualSaturation, 1e-3f, "saturation of $color")
        assertEquals(value, actualValue, 1e-3f, "value of $color")
    }

    @Test
    fun primaryAndSecondaryColorsHaveTheirHues() {
        assertHsv(0f, 1f, 1f, Color.Red)
        assertHsv(60f, 1f, 1f, Color.Yellow)
        assertHsv(120f, 1f, 1f, Color.Green)
        assertHsv(180f, 1f, 1f, Color.Cyan)
        assertHsv(240f, 1f, 1f, Color.Blue)
        assertHsv(300f, 1f, 1f, Color.Magenta)
    }

    @Test
    fun hueIsNeverNegative() {
        assertHsv(336f, 1f, 1f, Color(red = 1f, green = 0f, blue = 0.4f))
    }

    @Test
    fun greysHaveNoHueOrSaturation() {
        assertHsv(0f, 0f, 0.4f, Color(red = 0.4f, green = 0.4f, blue = 0.4f))
        assertHsv(0f, 0f, 1f, Color.White)
    }

    @Test
    fun blackHasNoSaturationOrValue() {
        assertHsv(0f, 0f, 0f, Color.Black)
    }

    @Test
    fun valueAndSaturationFollowBrightnessAndPurity() {
        assertHsv(0f, 0.5f, 0.8f, Color(red = 0.8f, green = 0.4f, blue = 0.4f))
    }
}
