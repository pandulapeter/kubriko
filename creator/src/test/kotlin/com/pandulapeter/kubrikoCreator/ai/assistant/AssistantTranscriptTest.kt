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

import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.PlanEntry
import com.agentclientprotocol.model.PlanEntryPriority
import com.agentclientprotocol.model.PlanEntryStatus
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssistantTranscriptTest {

    private fun AssistantTranscript.with(vararg updates: SessionUpdate) = updates.fold(this) { transcript, update -> transcript.withUpdate(update) }

    private fun message(text: String) = SessionUpdate.AgentMessageChunk(ContentBlock.Text(text))

    @Test
    fun streamedChunksJoinUntilSomethingElseComesBetween() {
        val transcript = AssistantTranscript()
            .withUserMessage("Make the Ball faster")
            .with(
                SessionUpdate.AgentThoughtChunk(ContentBlock.Text("The Ball's ")),
                SessionUpdate.AgentThoughtChunk(ContentBlock.Text("speed")),
                message("On "),
                message("it."),
                SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__read_actor_type"),
                message("Done."),
            )
        assertEquals(
            listOf(
                TranscriptItem.UserMessage("Make the Ball faster"),
                TranscriptItem.Thought("The Ball's speed"),
                TranscriptItem.AgentMessage("On it."),
                TranscriptItem.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__read_actor_type"),
                TranscriptItem.AgentMessage("Done."),
            ),
            transcript.items,
        )
        assertEquals("Make the Ball faster", transcript.title)
    }

    @Test
    fun toolCallUpdatesChangeOnlyWhatTheyName() {
        val transcript = AssistantTranscript().with(
            SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__place_actor", ToolKind.OTHER, ToolCallStatus.PENDING),
            SessionUpdate.ToolCallUpdate(ToolCallId("1"), status = ToolCallStatus.COMPLETED),
            SessionUpdate.ToolCallUpdate(ToolCallId("2"), title = "Read", status = ToolCallStatus.FAILED),
        )
        assertEquals(
            listOf(
                TranscriptItem.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__place_actor", ToolKind.OTHER, ToolCallStatus.COMPLETED),
                TranscriptItem.ToolCall(ToolCallId("2"), "Read", status = ToolCallStatus.FAILED),
            ),
            transcript.items,
        )
        assertEquals("place_actor", (transcript.items[0] as TranscriptItem.ToolCall).creatorToolName)
        assertNull((transcript.items[1] as TranscriptItem.ToolCall).creatorToolName)
    }

    @Test
    fun plansAndCommandsAreTheLatestOnes() {
        val entry = PlanEntry("Add a Paddle", PlanEntryPriority.HIGH, PlanEntryStatus.IN_PROGRESS)
        val command = AvailableCommand("compact", "Compact the conversation")
        val transcript = AssistantTranscript().with(
            SessionUpdate.PlanUpdate(listOf(entry.copy(status = PlanEntryStatus.PENDING))),
            SessionUpdate.PlanUpdate(listOf(entry)),
            SessionUpdate.AvailableCommandsUpdate(listOf(command)),
        )
        assertEquals(listOf(entry), transcript.plan)
        assertEquals(listOf(command), transcript.commands)
        assertEquals(emptyList(), transcript.items)
        assertEquals(emptyList(), transcript.withUpdate(SessionUpdate.PlanRemoved("plan")).plan)
    }

    @Test
    fun onlyTurnsTheAgentDidntFinishAreNoted() {
        val transcript = AssistantTranscript().withTurnEnd(StopReason.END_TURN).withTurnEnd(StopReason.CANCELLED)
        assertEquals(listOf<TranscriptItem>(TranscriptItem.TurnEnded(StopReason.CANCELLED)), transcript.items)
        assertNull(transcript.title)
    }
}
