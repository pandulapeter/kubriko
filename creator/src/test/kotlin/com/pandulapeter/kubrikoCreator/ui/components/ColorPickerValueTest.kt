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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorPickerValueTest {

    @Test
    fun itStartsWithTheColorAndItsHexCode() {
        val value = ColorPickerValue.of(0xFF6060AA)
        assertEquals("#6060AA", value.text)
        assertEquals(0xFF6060AA, value.argb)
        assertEquals("#806060AA", ColorPickerValue.of(0x806060AA).text)
    }

    @Test
    fun theHexCodeFollowsTheSliders() {
        val value = ColorPickerValue.of(0xFFFF0000).let { it.withColor(it.color.copy(hue = 120f)) }
        assertEquals("#00FF00", value.text)
        assertEquals("#8000FF00", value.withColor(value.color.copy(alpha = 128 / 255f)).text)
    }

    @Test
    fun theColorFollowsATypedHexCodeOnceItIsOne() {
        val value = ColorPickerValue.of(0xFFFF0000).withText("#00F")
        assertEquals("#00F", value.text)
        assertFalse(value.isTextValid)
        assertEquals(0xFFFF0000, value.argb)
        val typed = value.withText("#0000FF")
        assertTrue(typed.isTextValid)
        assertEquals(0xFF0000FF, typed.argb)
    }

    @Test
    fun aTypedGrayKeepsTheHueTheSlidersHad() {
        val value = ColorPickerValue.of(0xFF00FF00).withText("#808080")
        assertEquals(120f, value.color.hue)
    }

    @Test
    fun withoutAlphaTheColorStaysOpaque() {
        val value = ColorPickerValue.of(0x806060AA, isAlphaEnabled = false)
        assertEquals("#6060AA", value.text)
        assertEquals(0xFF123456, value.withText("#80123456").argb)
        assertEquals(0xFF6060AA, value.withColor(value.color.copy(alpha = 0f)).argb)
    }
}
