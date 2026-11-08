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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class PageEditorTest {
    private val page = "# The hero\n\nA knight who jumps between **floating** islands.\n\n## Moves\n\n- Runs\n- Jumps\n\n## Planned\n\n- Wall jump\n"

    @Test
    fun aPageShowsRenderedAndAChunkClickedIsEditedInPlace() = uiTest(width = 900, height = 640) {
        val saved = mutableListOf<String>()
        showing { PageEditor(name = "hero", text = page, onTextChange = { saved += it }) }
        onNodeWithText("The hero").assertExists()
        saveSnapshot("page-editor")
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNodeWithText("## Moves").assertExists()
        onNodeWithText("Bold").assertExists()
        saveSnapshot("page-editor-editing")
        assertEquals(emptyList(), saved)
    }

    @Test
    fun anEmptyPageSaysHowToStartIt() = uiTest(width = 700, height = 300) {
        showing { PageEditor(name = "villain", text = "", onTextChange = {}) }
        onNodeWithText("This page is empty. Add a paragraph to start it.").assertExists()
        onNodeWithText("Add a paragraph").performClick()
        waitForIdle()
        onNodeWithText("Heading").assertExists()
    }

    @Test
    fun mentionsAreChipsThatOpenWhatTheyNameAndWebAddressesAreShownFirst() = uiTest(width = 900, height = 400) {
        val opened = mutableListOf<Mention>()
        val links = PageLinksState("screenplay/README.md", File(".")) { opened += it }
        links.looks = mapOf(
            "../.kubriko/actors/Player.json" to MentionLook(Mention.ActorType("Player", ".kubriko/actors/Player.json"), exists = true),
            "villain.md" to MentionLook(Mention.Page("villain", "screenplay/villain.md"), exists = false),
        )
        val text = "# The game\n\nThe [hero](../.kubriko/actors/Player.json) meets the [villain](villain.md).\n\n[Read more](https://example.com)\n"
        showing { PageEditor(name = "README", text = text, onTextChange = {}, links = links) }
        onNodeWithText("The hero meets the villain.").assertExists()
        saveSnapshot("page-editor-mentions")
        onNodeWithText("hero").performClick()
        waitForIdle()
        assertEquals(listOf<Mention>(Mention.ActorType("Player", ".kubriko/actors/Player.json")), opened)
        onNodeWithText("Bold").assertDoesNotExist()
        onNodeWithText("Read more").performTouchInput { click(Offset(8f, centerY)) }
        waitForIdle()
        onNodeWithText("https://example.com").assertExists()
        saveSnapshot("page-editor-address")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertNull(links.pendingAddress)
    }

    @Test
    fun typingAnAtSignOffersWhatThePageCanMentionAndEnterWritesTheLink() = uiTest(width = 900, height = 500) {
        val links = PageLinksState("screenplay/hero.md", File(".")) {}
        links.candidates = listOf("Ball", "Paddle", "Brick").map { MentionCandidate(it, null, Mention.ActorType(it, ".kubriko/actors/$it.json")) }
        showing { PageEditor(name = "hero", text = page, onTextChange = {}, links = links) }
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNode(hasSetTextAction()).performTextInput(" @b")
        waitForIdle()
        onNodeWithText("Ball").assertExists()
        onNodeWithText("Brick").assertExists()
        onNodeWithText("Paddle").assertDoesNotExist()
        saveSnapshot("page-editor-mention-suggestions")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.DirectionDown) }
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithText("## Moves [Brick](../.kubriko/actors/Brick.json)").assertExists()
        onNodeWithText("Ball").assertDoesNotExist()
    }
}
