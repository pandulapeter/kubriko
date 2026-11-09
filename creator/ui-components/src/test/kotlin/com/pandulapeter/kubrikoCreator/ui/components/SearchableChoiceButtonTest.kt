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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchableChoiceButtonTest {
    private val shortList = listOf("Ball", "Bat", "Brick", "Paddle", "Wall")
    private val longList = (1..19).map { "Enemy" + it.toString().padStart(2, '0') } + "Player"

    @Test
    fun aShortListOpensWithoutAFilter() = uiTest(width = 400, height = 500) {
        showChoice(shortList) {}
        onNodeWithText("Choose").performClick()
        shortList.forEach { onNodeWithText(it).assertExists() }
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        saveSnapshot("searchable-choice-short")
    }

    @Test
    fun typingNarrowsALongListAndEnterPicksTheFirstMatch() = uiTest(width = 400, height = 500) {
        var selected: String? = null
        showChoice(longList) { selected = it }
        onNodeWithText("Choose").performClick()
        onNode(hasSetTextAction()).performTextInput("play")
        onNodeWithText("Player").assertExists()
        onNodeWithText("Enemy01").assertDoesNotExist()
        saveSnapshot("searchable-choice-filtered")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertEquals("Player", selected)
        assertEquals(1, onAllNodes(isRoot()).fetchSemanticsNodes().size)
    }

    @Test
    fun aListTallerThanTheWindowStaysInsideItAndScrolls() = uiTest(width = 400, height = 300) {
        showChoice((1..60).map { "Option$it" }) {}
        onNodeWithText("Choose").performClick()
        waitForIdle()
        assertTrue(onNodeWithText("Option1").fetchSemanticsNode().boundsInWindow.top >= 0f)
        onNodeWithText("Option60").assertDoesNotExist()
        saveSnapshot("searchable-choice-scrolling")
    }

    @Test
    fun enterWithNothingTypedPicksNothingAndKeepsTheListOpen() = uiTest(width = 400, height = 500) {
        var selected: String? = null
        showChoice(longList) { selected = it }
        onNodeWithText("Choose").performClick()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertNull(selected)
        onNodeWithText("Enemy01").assertExists()
        onNode(hasSetTextAction()).performTextInput("  ")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertNull(selected)
    }

    @Test
    fun clickingAnOptionPicksIt() = uiTest(width = 400, height = 500) {
        var selected: String? = null
        showChoice(shortList) { selected = it }
        onNodeWithText("Choose").performClick()
        onNodeWithText("Brick").performClick()
        assertEquals("Brick", selected)
        assertEquals(1, onAllNodes(isRoot()).fetchSemanticsNodes().size)
    }

    @Test
    fun nothingMatchingSaysSo() = uiTest(width = 400, height = 500) {
        var selected: String? = null
        showChoice(longList) { selected = it }
        onNodeWithText("Choose").performClick()
        onNode(hasSetTextAction()).performTextInput("zzz")
        onNodeWithText("Nothing matches.").assertExists()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertNull(selected)
    }

    @Test
    fun aHintTakesThePlaceOfAnEmptyPanelAndFollowsTheOptions() = uiTest(width = 400, height = 300) {
        var options by mutableStateOf(emptyList<String>())
        showChoice(options = { options }, hint = "Add some first.") {}
        onNodeWithText("Choose").performClick()
        onNodeWithText("Add some first.").assertExists()
        onNodeWithText("Nothing matches.").assertDoesNotExist()
        saveSnapshot("searchable-choice-hint")
        options = listOf("None")
        waitForIdle()
        onNodeWithText("None").assertExists()
        onNodeWithText("Add some first.").assertExists()
    }

    @Test
    fun aSearchMatchingNothingSaysSoEvenWithAHint() = uiTest(width = 400, height = 500) {
        showChoice(options = { longList }, hint = "Add some first.") {}
        onNodeWithText("Choose").performClick()
        onNode(hasSetTextAction()).performTextInput("zzz")
        onNodeWithText("Nothing matches.").assertExists()
    }

    @Test
    fun aHintStaysInSightBelowAListTallerThanTheWindow() = uiTest(width = 400, height = 300) {
        showChoice(options = { (1..60).map { "Option$it" } }, hint = "Add some first.") {}
        onNodeWithText("Choose").performClick()
        onNodeWithText("Add some first.").assertIsDisplayed()
        saveSnapshot("searchable-choice-hint-long")
    }

    private fun ComposeUiTest.showChoice(options: List<String>, onSelected: (String) -> Unit) = showChoice({ options }, hint = null, onSelected)

    private fun ComposeUiTest.showChoice(options: () -> List<String>, hint: String?, onSelected: (String) -> Unit) = showing {
        SearchableChoiceButton(
            label = "Choose",
            options = options(),
            optionText = { it },
            onSelected = onSelected,
            hint = hint,
        )
    }
}
