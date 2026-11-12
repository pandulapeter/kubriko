/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationMatch
import com.pandulapeter.kubrikoCreator.ai.assistant.KeptMessage
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversation
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.foundation.TextSnippet
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ConversationSearchTest {
    private val faster = SavedConversation("/game", "claude-acp", "a", "Make the ball faster", 2)
    private val bricks = SavedConversation("/game", "other-acp", "b", "Add gold bricks", 1)
    private val own = ConversationMatch(faster, "ball", 0, "Make the ball faster", 0, isFromUser = true, isHeldByAnotherAgent = false, snippet = TextSnippet("Make the ball faster", listOf(9..12)))
    private val others = ConversationMatch(bricks, "ball", 1, "The ball breaks them.", 0, isFromUser = false, isHeldByAnotherAgent = true, snippet = TextSnippet("The ball breaks them.", listOf(4..7)))

    @Test
    fun theMessageFoundIsTheOneKeptWithItsTextWithAsManyKeptAlikeAfterIt() {
        val items = listOf(
            TranscriptItem.UserMessage("Make the ball faster"),
            TranscriptItem.AgentMessage("Done."),
            TranscriptItem.UserMessage("Make the ball faster"),
            TranscriptItem.AgentMessage("Make the ball faster"),
        )
        assertEquals(2, jumpIndexOf(items, own))
        assertEquals(0, jumpIndexOf(items, own.copy(laterDuplicates = 1)))
        assertNull(jumpIndexOf(items, own.copy(messageText = "Something else")), "not in the conversation")
        val long = "x".repeat(KeptMessage.MESSAGE_LENGTH + 5)
        assertEquals(0, jumpIndexOf(listOf(TranscriptItem.AgentMessage(long)), own.copy(isFromUser = false, messageText = long.take(KeptMessage.MESSAGE_LENGTH))), "as far as it was kept")
    }

    @Test
    fun choosingAMatchOfTheAgentChosenShowsItsConversationAndOneOfAnotherAgentIsRead() {
        val search = ConversationSearchState().apply { open() }
        val shown = mutableListOf<ConversationMatch>()
        search.choose(others) { shown += it }
        assertEquals(others, search.reading)
        assertTrue(search.isOpen, "reading another agent's conversation leaves the search open")
        search.choose(own) { shown += it }
        assertEquals(listOf(own), shown)
        assertEquals(own, search.jump)
        assertFalse(search.isOpen)
        search.onJumped()
        assertNull(search.jump)
    }

    @Test
    fun aJumpIsForgottenWhenAnotherConversationShows() {
        val search = ConversationSearchState()
        search.choose(own) {}
        search.onConversationShown(null)
        assertEquals(own, search.jump, "the conversation hasn't started yet")
        search.onConversationShown(own.conversation.id)
        assertEquals(own, search.jump)
        search.onConversationShown("other")
        assertNull(search.jump)
    }

    @Test
    fun theMatchesAreListedWithTheirConversationsAndWhoSaidIt() = uiTest(width = 380, height = 400) {
        val chosen = mutableListOf<ConversationMatch>()
        var query by mutableStateOf("")
        showing {
            ConversationSearchView(
                query = query,
                onQueryChange = { query = it },
                search = { asked -> listOf(own, others).filter { it.query == asked } },
                onChosen = { chosen += it },
                onClose = {},
            )
        }
        onNodeWithText("Type to find what you asked", substring = true).assertExists()
        onNode(hasSetTextAction()).performTextInput("ball")
        awaitText("Make the ball faster · You")
        onNodeWithText("Add gold bricks · Agent · held by another agent").assertExists()
        saveSnapshot("conversation-search")
        onNodeWithText("The ball breaks them.").performClick()
        assertEquals(listOf(others), chosen)
    }

    @Test
    fun nothingFoundIsSaid() = uiTest(width = 380, height = 300) {
        var query by mutableStateOf("")
        showing { ConversationSearchView(query = query, onQueryChange = { query = it }, search = { emptyList() }, onChosen = {}, onClose = {}) }
        onNode(hasSetTextAction()).performTextInput("zebra")
        awaitText("No conversation holds zebra.")
    }

    @Test
    fun anotherAgentsConversationIsReadAtTheMessageFound() = uiTest(width = 800, height = 600) {
        var isDismissed = false
        showing {
            KeptConversationDialog(
                match = others,
                readMessages = { listOf(KeptMessage(true, "Add gold bricks"), KeptMessage(false, "The ball breaks them.")) },
                onDismiss = { isDismissed = true },
            )
        }
        awaitText("The ball breaks them.")
        onNodeWithText("Another agent holds this conversation", substring = true).assertExists()
        saveSnapshot("conversation-kept")
        onNodeWithText("Close").performClick()
        assertTrue(isDismissed)
    }

    @Test
    fun theSearchOpensFromThePanelAndGoesToTheConversationOfAMatch() = uiTest(width = 420, height = 600) {
        val model = FakeAssistantPanelModel().apply { conversationMatches = listOf(own) }
        showing { AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, forms = rememberAssistantForms()) }
        onNodeWithContentDescription("Search conversations").performClick()
        onNode(hasSetTextAction()).performTextInput("ball")
        awaitText("Make the ball faster · You")
        onNodeWithText("Make the ball faster · You").performClick()
        waitUntil(timeoutMillis = 5_000) { "show: a" in model.calls }
        onNodeWithContentDescription("Back to the conversation").assertDoesNotExist()
    }

    private fun ComposeUiTest.awaitText(text: String) = waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun theTranscriptScrollsToTheMessageFoundOnceItArrivesAndHandsTheJumpBackEitherWay() = uiTest(width = 420, height = 300) {
        var items by mutableStateOf<List<TranscriptItem>>(List(30) { TranscriptItem.AgentMessage("Line $it") })
        var jumps = 0
        showing { AssistantTranscriptList(items = items, isBusy = false, steps = emptyMap(), onShowStep = {}, jumpTo = own, onJumped = { jumps++ }) }
        waitForIdle()
        assertEquals(0, jumps, "waiting for the history")
        items = listOf<TranscriptItem>(TranscriptItem.UserMessage("Make the ball faster")) + items
        waitUntil(timeoutMillis = 5_000) { jumps == 1 }
        onNodeWithText("Make the ball faster").assertIsDisplayed()
    }

    @Test
    fun aMessageThatNeverArrivesIsGivenUpOn() = uiTest(width = 420, height = 300) {
        var jumps = 0
        showing { AssistantTranscriptList(items = listOf(TranscriptItem.AgentMessage("Done.")), isBusy = false, steps = emptyMap(), onShowStep = {}, jumpTo = own, onJumped = { jumps++ }) }
        waitUntil(timeoutMillis = 5_000) { jumps == 1 }
    }

    @Test
    fun aMatchFoundBeforeTheAgentChangedIsReadRatherThanContinuedWithAnotherAgent() = uiTest(width = 420, height = 600) {
        val model = FakeAssistantPanelModel().apply {
            conversationMatches = listOf(own.copy(conversation = own.conversation.copy(agentId = agent.value.id)))
            keptMessages = listOf(KeptMessage(true, "Make the ball faster"))
        }
        showing { AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, forms = rememberAssistantForms()) }
        onNodeWithContentDescription("Search conversations").performClick()
        onNode(hasSetTextAction()).performTextInput("ball")
        awaitText("Make the ball faster · You")
        model.agent.value = AgentChoice.Custom("other-agent")
        awaitText("Make the ball faster · You · held by another agent")
        onNodeWithText("Make the ball faster · You · held by another agent").performClick()
        awaitText("Another agent holds this conversation", substring = true)
        assertTrue(model.calls.none { it.startsWith("show") })
    }

    private fun ComposeUiTest.awaitText(text: String, substring: Boolean) =
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
}
