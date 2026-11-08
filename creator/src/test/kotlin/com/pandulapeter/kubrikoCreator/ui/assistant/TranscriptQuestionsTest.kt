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

import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptQuestionsTest {
    private fun call(id: String, status: ToolCallStatus? = ToolCallStatus.IN_PROGRESS) = TranscriptItem.ToolCall(ToolCallId(id), "mcp__kubriko-creator__ask_user", status = status)

    private fun question(id: Long, conversation: String?, toolCallId: String? = null) = UserQuestions.Question(
        id = id,
        text = "Which?",
        answers = listOf("A", "B"),
        origin = conversation?.let { AiStepOrigin(it, toolCallId) },
    )

    @Test
    fun aQuestionIsPlacedUnderTheCallThatAskedItAndOneWhoseCallIsntThereAfterEverything() {
        val placed = question(1, "one", "a")
        val withoutCall = question(2, "one")
        val ofAnUnknownCall = question(3, "one", "gone")
        val questions = listOf(placed, withoutCall, ofAnUnknownCall, question(4, "two", "a"), question(5, null))
        assertEquals(
            TranscriptQuestions(mapOf(ToolCallId("a") to placed), listOf(withoutCall, ofAnUnknownCall)),
            questionsOf(listOf(call("a")), questions, "one"),
        )
    }

    @Test
    fun aConversationNotStartedYetHasNoQuestions() {
        assertEquals(TranscriptQuestions.None, questionsOf(listOf(call("a")), listOf(question(1, "one", "a")), null))
    }

    @Test
    fun theAgentStoppedWaitingOnceItsCallOrItsTurnEnded() {
        assertFalse(isLate(call("a"), isBusy = true))
        assertFalse(isLate(toolCall = null, isBusy = true))
        assertTrue(isLate(call("a", ToolCallStatus.FAILED), isBusy = true))
        assertTrue(isLate(call("a", ToolCallStatus.COMPLETED), isBusy = true))
        assertTrue(isLate(call("a"), isBusy = false))
    }

    @Test
    fun theAgentWaitsForAnAnswerOnlyWhileItsTurnRunsAndTheAskingCallDoes() {
        val placed = TranscriptQuestions(mapOf(ToolCallId("a") to question(1, "one", "a")), emptyList())
        assertTrue(placed.isWaitingForAnswer(listOf(call("a")), isBusy = true))
        assertFalse(placed.isWaitingForAnswer(listOf(call("a", ToolCallStatus.FAILED)), isBusy = true))
        assertFalse(placed.isWaitingForAnswer(listOf(call("a")), isBusy = false))
        assertTrue(TranscriptQuestions(emptyMap(), listOf(question(2, "one"))).isWaitingForAnswer(emptyList(), isBusy = true))
        assertFalse(TranscriptQuestions.None.isWaitingForAnswer(listOf(call("a")), isBusy = true))
    }
}
