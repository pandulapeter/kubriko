/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberFormattingTest {

    private val previousLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() = Locale.setDefault(previousLocale)

    @Test
    fun formattingIgnoresTheDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("hu-HU"))
        assertEquals("1.50", formatEditorNumber(1.5f, shouldRound = false))
        Locale.setDefault(Locale.forLanguageTag("de-DE"))
        assertEquals("1.50", formatEditorNumber(1.5f, shouldRound = false))
    }

    @Test
    fun roundedValuesAreIntegers() {
        assertEquals("3", formatEditorNumber(2.6f, shouldRound = true))
    }

    @Test
    fun parsingAcceptsDotsAndCommas() {
        assertEquals(1.5f, parseEditorNumber("1.5"))
        assertEquals(1.5f, parseEditorNumber("1,5"))
        assertEquals(45f, parseEditorNumber(" 45.00 "))
        assertEquals(-0.25f, parseEditorNumber("-0.25"))
    }

    @Test
    fun parsingRejectsInvalidAndNonFiniteInput() {
        listOf("", "-", "abc", "45°", "NaN", "Infinity").forEach { assertNull(parseEditorNumber(it), it) }
    }
}
