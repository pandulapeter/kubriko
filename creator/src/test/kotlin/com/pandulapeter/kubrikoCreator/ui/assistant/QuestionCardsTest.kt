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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class QuestionCardsTest {

    @Test
    fun aQuestionIsAnsweredByAButtonOrByTypingAnotherAnswer() = uiTest {
        val answers = mutableListOf<Pair<Long, String>>()
        showing {
            QuestionCards(
                questions = listOf(UserQuestions.Question(1, "How does the hero get around?", listOf("Runs and jumps", "Flies", "Swims"))),
                onAnswer = { id, answer -> answers += id to answer },
            )
        }
        saveSnapshot("question-card")
        onNodeWithText("Flies").performClick()
        onNode(hasSetTextAction()).performTextInput("Rides a dragon")
        onNodeWithText("Answer").performClick()
        assertEquals(listOf(1L to "Flies", 1L to "Rides a dragon"), answers)
    }

    @Test
    fun theAssistantsQuestionIsAnsweredUnderTheToolCallThatAskedIt() = uiTest {
        val answers = mutableListOf<Triple<Long, String, Boolean>>()
        showing {
            AssistantTranscriptList(
                items = listOf(TranscriptItem.UserMessage("Let's make a game."), askingCall(ToolCallStatus.IN_PROGRESS)),
                isBusy = true,
                steps = emptyMap(),
                onShowStep = {},
                questions = TranscriptQuestions(mapOf(ToolCallId("toolu_1") to question), emptyList()),
                onAnswer = { question, answer, isLate -> answers += Triple(question.id, answer, isLate) },
            )
        }
        saveSnapshot("question-in-conversation")
        onNodeWithText("Another answer").assertDoesNotExist()
        onNodeWithText("The agent stopped waiting", substring = true).assertDoesNotExist()
        onNodeWithText("Working", substring = true).assertDoesNotExist()
        onNodeWithText("Flies").performClick()
        assertEquals(listOf(Triple(1L, "Flies", false)), answers)
    }

    @Test
    fun aQuestionTheAgentStoppedWaitingForIsAnsweredOnceTheConversationTakesAMessage() = uiTest {
        val answers = mutableListOf<Triple<Long, String, Boolean>>()
        var canSendMessage by mutableStateOf(false)
        showing {
            AssistantTranscriptList(
                items = listOf(askingCall(ToolCallStatus.FAILED)),
                isBusy = true,
                steps = emptyMap(),
                onShowStep = {},
                questions = TranscriptQuestions(mapOf(ToolCallId("toolu_1") to question), emptyList()),
                canSendMessage = canSendMessage,
                onAnswer = { question, answer, isLate -> answers += Triple(question.id, answer, isLate) },
            )
        }
        saveSnapshot("question-late")
        onNodeWithText("The agent stopped waiting", substring = true).assertExists()
        onNodeWithText("Another answer").assertExists()
        onNodeWithText("Flies").assertIsNotEnabled()
        canSendMessage = true
        onNodeWithText("Flies").assertIsEnabled().performClick()
        assertEquals(listOf(Triple(1L, "Flies", true)), answers)
    }

    private val question = UserQuestions.Question(1, "How does the hero get around?", listOf("Runs and jumps", "Flies"), AiStepOrigin("tools", "toolu_1"))

    private fun askingCall(status: ToolCallStatus) = TranscriptItem.ToolCall(ToolCallId("toolu_1"), "mcp__kubriko-creator__ask_user", status = status)
}
