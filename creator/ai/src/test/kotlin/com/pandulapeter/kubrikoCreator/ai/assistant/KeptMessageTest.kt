/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.agentclientprotocol.model.ToolCallId
import kotlin.test.Test
import kotlin.test.assertEquals

class KeptMessageTest {

    @Test
    fun onlyWhatTheUserAskedAndTheAgentAnsweredIsKept() = assertEquals(
        listOf(KeptMessage(isFromUser = true, text = "Make the ball faster"), KeptMessage(isFromUser = false, text = "Done.")),
        KeptMessage.of(
            listOf(
                TranscriptItem.UserMessage("Make the ball faster"),
                TranscriptItem.Thought("Reading Ball"),
                TranscriptItem.ToolCall(ToolCallId("1"), "read_actor_type"),
                TranscriptItem.AgentMessage(" "),
                TranscriptItem.AgentMessage("Done."),
            ),
        ),
    )

    @Test
    fun aLongMessageIsCutAndOnlyTheNewestFitTogether() {
        val long = TranscriptItem.AgentMessage("x".repeat(KeptMessage.MESSAGE_LENGTH + 10))
        val kept = KeptMessage.of(listOf(TranscriptItem.UserMessage("first")) + List(KeptMessage.TOTAL_LENGTH / KeptMessage.MESSAGE_LENGTH) { long })
        assertEquals(KeptMessage.MESSAGE_LENGTH, kept.last().text.length)
        assertEquals(KeptMessage.TOTAL_LENGTH, kept.sumOf { it.text.length })
        assertEquals(false, kept.first().isFromUser, "the oldest went first")
    }
}
