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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ColorFieldTest {

    @Test
    fun anIncompleteHexCodeStaysWhileTypingAndACompleteOneIsReported() = uiTest {
        val changes = mutableListOf<Long>()
        showing { ColorField(color = 0xFFFF0000, onColorChange = { changes += it }) }
        onNodeWithText("#FF0000").assertExists()
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("#FF000")
        waitForIdle()
        onNodeWithText("#FF000").assertExists()
        assertEquals(emptyList(), changes)
        field.performTextReplacement("#00FF00")
        waitForIdle()
        assertEquals(listOf(0xFF00FF00), changes)
        saveSnapshot("color-field")
    }

    @Test
    fun aColorChangedElsewhereReplacesTheTextEvenWhileTyping() = uiTest {
        var color by mutableStateOf(0xFFFF0000)
        showing { ColorField(color = color, onColorChange = { color = it }) }
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("#00FF00")
        waitForIdle()
        color = 0xFFFF0000
        waitForIdle()
        onNodeWithText("#FF0000").assertExists()
    }
}
