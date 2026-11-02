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
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BufferedTextFieldTest {

    @Test
    fun aLateModelDoesNotOverwriteWhatIsTyped() = uiTest {
        var value by mutableStateOf("")
        val changes = mutableListOf<String>()
        showing { BufferedTextField(value = value, onValueChange = { changes += it }) }
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("a")
        waitForIdle()
        field.performTextReplacement("ab")
        waitForIdle()
        value = "a"
        waitForIdle()
        onNodeWithText("ab").assertExists()
        value = "ab"
        waitForIdle()
        onNodeWithText("ab").assertExists()
        assertEquals(listOf("a", "ab"), changes)
    }

    @Test
    fun aValueChangedElsewhereShowsWhenNotTyping() = uiTest {
        var value by mutableStateOf("Hello")
        showing { BufferedTextField(value = value, onValueChange = {}) }
        onNodeWithText("Hello").assertExists()
        value = "World"
        waitForIdle()
        onNodeWithText("World").assertExists()
    }

    @Test
    fun aValueChangedElsewhereReplacesTheTextEvenWhileTyping() = uiTest {
        var value by mutableStateOf("")
        showing { BufferedTextField(value = value, onValueChange = { value = it }) }
        val field = onNode(hasSetTextAction())
        field.requestFocus()
        field.performTextReplacement("Ball")
        waitForIdle()
        value = "Paddle"
        waitForIdle()
        onNodeWithText("Paddle").assertExists()
        onNodeWithText("Ball").assertDoesNotExist()
    }
}
