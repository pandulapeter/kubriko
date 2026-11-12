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

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchHighlightTest {

    private val style = SpanStyle(fontWeight = FontWeight.Bold)

    @Test
    fun theMatchedLettersAreStyled() = assertEquals(
        listOf(0 to 2, 4 to 5),
        highlightedMatch("Brick", "brk", style).spanStyles.map { it.start to it.end },
    )

    @Test
    fun aNameNotMatchingIsLeftAsItIs() {
        val text = highlightedMatch("Brick", "x", style)
        assertEquals("Brick", text.text)
        assertEquals(emptyList(), text.spanStyles)
    }

    @Test
    fun rangesPastTheTextAreClamped() = assertEquals(
        listOf(3 to 5),
        highlighted("Brick", listOf(3..9, 7..8), style).spanStyles.map { it.start to it.end },
    )
}
