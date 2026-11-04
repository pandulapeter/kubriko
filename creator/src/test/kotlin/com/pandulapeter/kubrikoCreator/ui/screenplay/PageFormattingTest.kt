/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class PageFormattingTest {

    @Test
    fun aSelectionIsWrappedAndStaysSelected() {
        assertEquals(TextFieldValue("a **knight** jumps", TextRange(4, 10)), PageFormatting.wrapped(TextFieldValue("a knight jumps", TextRange(2, 8)), "**"))
        assertEquals(TextFieldValue("a **** jumps", TextRange(4)), PageFormatting.wrapped(TextFieldValue("a  jumps", TextRange(2)), "**"))
    }

    @Test
    fun aSelectionBecomesALinkWithTheCursorWhereTheAddressGoes() =
        assertEquals(TextFieldValue("see [hero]() here", TextRange(11)), PageFormatting.linked(TextFieldValue("see hero here", TextRange(4, 8))))

    @Test
    fun linePrefixesGoOnEveryTouchedLineAndComeOffAgain() {
        val listed = PageFormatting.withLinePrefix(TextFieldValue("Runs\nJumps\nRolls", TextRange(2, 7)), "- ")
        assertEquals("- Runs\n- Jumps\nRolls", listed.text)
        assertEquals("Runs\nJumps\nRolls", PageFormatting.withLinePrefix(listed.copy(selection = TextRange(3, 9)), "- ").text)
        assertEquals("## Moves", PageFormatting.withLinePrefix(TextFieldValue("Moves", TextRange(0)), "## ").text)
    }
}
