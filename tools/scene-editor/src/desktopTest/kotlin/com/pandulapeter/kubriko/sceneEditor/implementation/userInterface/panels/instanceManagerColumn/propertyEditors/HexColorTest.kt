/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HexColorTest {

    @Test
    fun colorIsFormattedAsSixUppercaseHexDigits() {
        assertEquals("FF8000", Color(red = 1f, green = 0.5f, blue = 0f).toHexString())
    }

    @Test
    fun sixHexDigitsParseIntoAColorWithTheGivenAlpha() {
        assertEquals(Color(red = 1f, green = 128 / 255f, blue = 0f, alpha = 0.3f), "FF8000".parseHexColor(0.3f))
    }

    @Test
    fun invalidHexTextDoesNotParse() {
        assertNull("FFF".parseHexColor(1f))
        assertNull("GG0000".parseHexColor(1f))
    }

    @Test
    fun hexInputKeepsTheLastSixUppercaseHexDigits() {
        assertEquals("CDEF99", sanitizeHexInput("#ab-12cdEF99"))
        assertEquals("", sanitizeHexInput(""))
    }

    @Test
    fun formattingAndParsingRoundTrip() {
        listOf(
            Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
            Color(red = 1f, green = 1f, blue = 1f, alpha = 0.5f),
            Color(red = 18 / 255f, green = 52 / 255f, blue = 86 / 255f, alpha = 1f),
            Color(red = 171 / 255f, green = 205 / 255f, blue = 239 / 255f, alpha = 0f),
        ).forEach { color ->
            assertEquals(color, color.toHexString().parseHexColor(color.alpha))
        }
    }
}
