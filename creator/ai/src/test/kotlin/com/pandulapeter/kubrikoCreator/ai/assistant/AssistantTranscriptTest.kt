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

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.Cost
import com.agentclientprotocol.model.PlanEntry
import com.agentclientprotocol.model.PlanEntryPriority
import com.agentclientprotocol.model.PlanEntryStatus
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun plansAreTheLatestOnes() {
        val entry = PlanEntry("Add a Paddle", PlanEntryPriority.HIGH, PlanEntryStatus.IN_PROGRESS)
        val transcript = AssistantTranscript().with(
            SessionUpdate.PlanUpdate(listOf(entry.copy(status = PlanEntryStatus.PENDING))),
            SessionUpdate.PlanUpdate(listOf(entry)),
        )
        assertEquals(listOf(entry), transcript.plan)
        assertEquals(emptyList(), transcript.items)
        assertEquals(emptyList(), transcript.withUpdate(SessionUpdate.PlanRemoved("plan")).plan)
    }

    @Test
    fun aPlanAwaitsApprovalUntilTheUserAnswersOrTheAgentStartsIt() {
        val pending = listOf(PlanEntry("Add Lives", PlanEntryPriority.MEDIUM, PlanEntryStatus.PENDING), PlanEntry("Lose a life", PlanEntryPriority.MEDIUM, PlanEntryStatus.PENDING))
        val proposed = AssistantTranscript().withUserMessage("Add lives").with(SessionUpdate.PlanUpdate(pending))
        assertTrue(proposed.isPlanAwaitingApproval)
        assertFalse(proposed.withUserMessage("Go ahead").isPlanAwaitingApproval)
        assertFalse(proposed.with(SessionUpdate.UserMessageChunk(ContentBlock.Text("Go ahead"))).isPlanAwaitingApproval)
        assertFalse(proposed.with(SessionUpdate.PlanUpdate(listOf(pending[0].copy(status = PlanEntryStatus.IN_PROGRESS), pending[1]))).isPlanAwaitingApproval)
        assertFalse(proposed.withUpdate(SessionUpdate.PlanRemoved("plan")).isPlanAwaitingApproval)
        assertFalse(AssistantTranscript().with(SessionUpdate.PlanUpdate(emptyList())).isPlanAwaitingApproval)
        assertTrue(proposed.withUserMessage("Fewer lives").with(SessionUpdate.PlanUpdate(pending)).isPlanAwaitingApproval)
    }

    @Test
    fun oncePlannedWithCreatorsToolTheAgentsAcpPlansNoLongerReplaceIt() {
        val creators = listOf(PlanEntry("Add Lives", PlanEntryPriority.MEDIUM, PlanEntryStatus.PENDING))
        val acp = listOf(PlanEntry("Read the Ball", PlanEntryPriority.MEDIUM, PlanEntryStatus.IN_PROGRESS))
        val transcript = AssistantTranscript().with(SessionUpdate.PlanUpdate(acp)).withCreatorPlan(creators)
        assertEquals(creators, transcript.plan)
        assertEquals(creators, transcript.with(SessionUpdate.PlanUpdate(acp), SessionUpdate.PlanRemoved("plan")).plan)
        assertTrue(transcript.isPlanAwaitingApproval)
    }

    @Test
    fun aPlanAnAgentStartedWithoutMarkingItOrWhoseTurnDidntEndAsMeantAwaitsNothing() {
        val proposed = AssistantTranscript().with(SessionUpdate.PlanUpdate(listOf(PlanEntry("Add Lives", PlanEntryPriority.MEDIUM, PlanEntryStatus.PENDING))))
        assertTrue(proposed.with(SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__list_scenes")).isPlanAwaitingApproval)
        assertTrue(proposed.with(SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__ask_user")).isPlanAwaitingApproval)
        assertFalse(proposed.with(SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__add_property")).isPlanAwaitingApproval)
        assertFalse(
            proposed.with(SessionUpdate.ToolCall(ToolCallId("1"), "Tool"), SessionUpdate.ToolCallUpdate(ToolCallId("1"), title = "mcp__kubriko-creator__add_property")).isPlanAwaitingApproval,
        )
        assertFalse(proposed.withTurnEnd(StopReason.CANCELLED).isPlanAwaitingApproval)
        assertFalse(proposed.withFailure(FailureReason.PROMPT_FAILED).isPlanAwaitingApproval)
        assertTrue(proposed.withTurnEnd(StopReason.END_TURN).isPlanAwaitingApproval)
    }

    @Test
    fun onlyTurnsTheAgentDidntFinishAreNoted() {
        val transcript = AssistantTranscript().withTurnEnd(StopReason.END_TURN).withTurnEnd(StopReason.CANCELLED)
        assertEquals(listOf<TranscriptItem>(TranscriptItem.TurnEnded(StopReason.CANCELLED)), transcript.items)
        assertNull(transcript.title)
    }

    @Test
    fun usageIsTheLatestReported() {
        val transcript = AssistantTranscript()
            .withUpdate(SessionUpdate.UsageUpdate(1_000, 200_000, null))
            .withUpdate(SessionUpdate.UsageUpdate(84_000, 200_000, Cost(0.12, "USD")))
        assertEquals(ConversationUsage(84_000, 200_000, 0.12, "USD"), transcript.usage)
    }

    @Test
    fun whatATurnChangedComesBeforeItsFinalAnswerAndKeepAndUndoAfterIt() {
        val toolCall = SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__add_block", ToolKind.OTHER, ToolCallStatus.COMPLETED)
        val transcript = AssistantTranscript().withUserMessage("faster").with(message("On it."), toolCall, message("Done."))
            .withTurnEnd(StopReason.MAX_TOKENS)
            .withTurnChanges(4, changes)
        assertEquals(
            listOf(
                TranscriptItem.UserMessage("faster"),
                TranscriptItem.AgentMessage("On it."),
                TranscriptItem.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__add_block", ToolKind.OTHER, ToolCallStatus.COMPLETED),
                TranscriptItem.TurnChanges(4, changes),
                TranscriptItem.AgentMessage("Done."),
                TranscriptItem.TurnEnded(StopReason.MAX_TOKENS),
                TranscriptItem.TurnReview(4),
            ),
            transcript.items,
        )
    }

    @Test
    fun aTurnWithoutAFinalAnswerEndsWithWhatItChanged() {
        val toolCall = SessionUpdate.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__add_block", ToolKind.OTHER, ToolCallStatus.COMPLETED)
        val transcript = AssistantTranscript().withUserMessage("faster").with(toolCall).withTurnChanges(4, changes)
        assertEquals(listOf(TranscriptItem.TurnChanges(4, changes), TranscriptItem.TurnReview(4)), transcript.items.drop(2))
        assertEquals(AssistantTranscript().withUserMessage("hi"), AssistantTranscript().withUserMessage("hi").withTurnChanges(4, emptyList()))
    }

    @Test
    fun onlyTheReviewOfTheTurnChosenChanges() {
        val transcript = AssistantTranscript().withTurnChanges(1, changes).withTurnChanges(4, changes)
            .withTurnReviewState(4, TranscriptItem.TurnReview.State.UNDONE)
        assertEquals(
            listOf(TranscriptItem.TurnReview(1), TranscriptItem.TurnReview(4, TranscriptItem.TurnReview.State.UNDONE)),
            transcript.items.filterIsInstance<TranscriptItem.TurnReview>(),
        )
    }

    private val changes = listOf(ChangeRecord(5, "add_block", CommandTarget.BlockType("Ball", 0, 0)))
}
