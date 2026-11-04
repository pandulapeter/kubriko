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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DescriptionPreviewTest {

    @Test
    fun theDescriptionIsSavedOnceWhenTheDialogIsSaved() = uiTest {
        val changes = mutableListOf<String>()
        var description by mutableStateOf<String?>("The ball")
        showing { DescriptionPreview(name = "Ball", description = description, maximumLength = 200, onDescriptionChange = { changes += it; description = it }) }
        onNodeWithText("The ball").performClick()
        onNodeWithText("What Ball is for").assertExists()
        val field = onNode(hasSetTextAction())
        field.performTextReplacement("The ball the player")
        field.performTextReplacement("The ball the player bounces")
        waitForIdle()
        assertEquals(emptyList(), changes)
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertEquals(listOf("The ball the player bounces"), changes)
        onNodeWithText("What Ball is for").assertDoesNotExist()
        onNodeWithText("The ball the player bounces").assertExists()
    }

    @Test
    fun cancellingOrSavingTheSameTextChangesNothing() = uiTest {
        val changes = mutableListOf<String>()
        showing { DescriptionPreview(name = "Ball", description = "The ball", maximumLength = 200, onDescriptionChange = { changes += it }) }
        onNodeWithText("The ball").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Something else")
        onNodeWithText("Cancel").performClick()
        onNodeWithText("The ball").performClick()
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertEquals(emptyList(), changes)
    }

    @Test
    fun enterSaves() = uiTest {
        val changes = mutableListOf<String>()
        showing { DescriptionPreview(name = "Ball", description = null, maximumLength = 200, onDescriptionChange = { changes += it }) }
        onNodeWithText("For").performClick()
        val field = onNode(hasSetTextAction())
        field.performTextReplacement("The ball")
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf("The ball"), changes)
    }

    @Test
    fun aDescriptionStaysOneLineAndWithinItsLength() {
        assertEquals("one two three", descriptionLine("one\ntwo\r\nthree", maximumLength = 200))
        assertEquals("abc", descriptionLine("abcdef", maximumLength = 3))
    }
}
