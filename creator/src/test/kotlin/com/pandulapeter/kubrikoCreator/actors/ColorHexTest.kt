/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorHexTest {

    @Test
    fun opaqueColorsAreShownWithoutTheirAlpha() {
        assertEquals("#6060AA", ColorHex.format(0xFF6060AA))
        assertEquals("#000001", ColorHex.format(0xFF000001))
    }

    @Test
    fun translucentColorsAreShownWithTheirAlpha() {
        assertEquals("#806060AA", ColorHex.format(0x806060AA))
        assertEquals("#00000000", ColorHex.format(0L))
    }

    @Test
    fun typedColorsBecomeOpaqueWithOrWithoutTheHash() {
        assertEquals(0xFF6060AA, ColorHex.parse("#6060aa"))
        assertEquals(0xFF6060AA, ColorHex.parse(" 6060AA "))
    }

    @Test
    fun typedColorsWithEightDigitsKeepTheirAlpha() {
        assertEquals(0x806060AA, ColorHex.parse("#806060aa"))
        assertEquals(0L, ColorHex.parse("00000000"))
    }

    @Test
    fun anythingElseIsRejected() {
        listOf("", "#6060A", "#6060AAF", "#6060AAFFF", "#GGGGGG", "red").forEach { assertNull(ColorHex.parse(it), it) }
    }
}
