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
class NumberFieldTest {

    @Test
    fun anUnparseableTextStaysWhileTyping() = uiTest {
        var value by mutableStateOf(5f)
        showing { NumberField(label = "Speed", value = value, onValueChange = { value = it }) }
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("")
        waitForIdle()
        field.performTextReplacement("-")
        waitForIdle()
        onNodeWithText("-").assertExists()
        assertEquals(5f, value)
        field.performTextReplacement("-3")
        waitForIdle()
        assertEquals(-3f, value)
        onNodeWithText("-3").assertExists()
        saveSnapshot("number-field")
    }

    @Test
    fun aLateModelDoesNotOverwriteWhatIsTyped() = uiTest {
        var value by mutableStateOf(5f)
        val changes = mutableListOf<Float>()
        showing { NumberField(label = "Speed", value = value, onValueChange = { changes += it }) }
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("1")
        waitForIdle()
        field.performTextReplacement("12")
        waitForIdle()
        onNodeWithText("12").assertExists()
        value = 1f
        waitForIdle()
        onNodeWithText("12").assertExists()
        value = 12f
        waitForIdle()
        onNodeWithText("12").assertExists()
        assertEquals(listOf(1f, 12f), changes)
    }

    @Test
    fun aValueChangedElsewhereShowsWhenNotTyping() = uiTest {
        var value by mutableStateOf(5f)
        showing { NumberField(label = "Speed", value = value, onValueChange = { value = it }) }
        onNodeWithText("5").assertExists()
        value = 7f
        waitForIdle()
        onNodeWithText("7").assertExists()
    }

    @Test
    fun aValueChangedElsewhereReplacesTheTextEvenWhileTyping() = uiTest {
        var value by mutableStateOf(5f)
        showing { NumberField(label = "Speed", value = value, onValueChange = { value = it }) }
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("12")
        waitForIdle()
        value = 5f
        waitForIdle()
        onNodeWithText("5").assertExists()
    }
}
