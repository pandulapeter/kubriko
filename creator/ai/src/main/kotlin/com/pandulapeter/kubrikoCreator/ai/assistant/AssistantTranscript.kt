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
import com.agentclientprotocol.model.PlanEntry
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord

/**
 * A conversation as the assistant panel shows it, built up from what the user sends and the agent reports.
 *
 * @param plan The agent's latest plan, shown as a checklist.
 * @param usage What the conversation uses and cost, where the agent reports it.
 */
public data class AssistantTranscript(
    val items: List<TranscriptItem> = emptyList(),
    val plan: List<PlanEntry> = emptyList(),
    val usage: ConversationUsage? = null,
) {
    /**
     * The first thing the user asked, which names the conversation.
     */
    public val title: String? get() = items.firstNotNullOfOrNull { (it as? TranscriptItem.UserMessage)?.text }

    internal fun withUserMessage(text: String) = copy(items = items + TranscriptItem.UserMessage(text))

    internal fun withUpdate(update: SessionUpdate): AssistantTranscript = when (update) {
        is SessionUpdate.AgentMessageChunk -> update.content.text()?.let { text ->
            copy(items = items.appending(text, { (it as? TranscriptItem.AgentMessage)?.text }, TranscriptItem::AgentMessage))
        } ?: this
        is SessionUpdate.AgentThoughtChunk -> update.content.text()?.let { text ->
            copy(items = items.appending(text, { (it as? TranscriptItem.Thought)?.text }, TranscriptItem::Thought))
        } ?: this
        is SessionUpdate.UserMessageChunk -> update.content.text()?.let { text ->
            copy(items = items.appending(text, { (it as? TranscriptItem.UserMessage)?.text }, TranscriptItem::UserMessage))
        } ?: this
        is SessionUpdate.ToolCall -> withToolCall(TranscriptItem.ToolCall(update.toolCallId, update.title, update.kind, update.status))
        is SessionUpdate.ToolCallUpdate -> {
            val previous = items.firstOrNull { it is TranscriptItem.ToolCall && it.id == update.toolCallId } as? TranscriptItem.ToolCall
            withToolCall(
                TranscriptItem.ToolCall(
                    id = update.toolCallId,
                    title = update.title ?: previous?.title.orEmpty(),
                    kind = update.kind ?: previous?.kind,
                    status = update.status ?: previous?.status,
                ),
            )
        }
        is SessionUpdate.PlanUpdate -> copy(plan = update.entries)
        is SessionUpdate.PlanRemoved -> copy(plan = emptyList())
        is SessionUpdate.UsageUpdate -> copy(usage = ConversationUsage(update.used, update.size, update.cost?.amount, update.cost?.currency))
        else -> this
    }

    /**
     * Notes how a turn ended, unless the agent simply finished it.
     */
    internal fun withTurnEnd(reason: StopReason) = if (reason == StopReason.END_TURN) this else copy(items = items + TranscriptItem.TurnEnded(reason))

    /**
     * Shows what [changes] a turn made before the agent's final answer, and offers to keep or undo them at the end,
     * unless there are none.
     */
    internal fun withTurnChanges(sinceRevision: Int, changes: List<ChangeRecord>): AssistantTranscript {
        if (changes.isEmpty()) return this
        val lastIndex = items.indexOfLast { it !is TranscriptItem.TurnEnded }
        val summaryIndex = if (items.getOrNull(lastIndex) is TranscriptItem.AgentMessage) lastIndex else lastIndex + 1
        return copy(
            items = items.subList(0, summaryIndex) + TranscriptItem.TurnChanges(sinceRevision, changes) + items.subList(summaryIndex, items.size) +
                TranscriptItem.TurnReview(sinceRevision),
        )
    }

    /**
     * Marks the changes of the turn that began at [sinceRevision] as [state].
     */
    internal fun withTurnReviewState(sinceRevision: Int, state: TranscriptItem.TurnReview.State) = copy(
        items = items.map { if (it is TranscriptItem.TurnReview && it.sinceRevision == sinceRevision) it.copy(state = state) else it },
    )

    internal fun withFailure(reason: FailureReason, details: String? = null) = copy(items = items + TranscriptItem.Failure(reason, details))

    internal fun withoutLastItem() = copy(items = items.dropLast(1))

    /**
     * Whether the last prompt was refused until the user signs in.
     */
    public val needsSignIn: Boolean get() = (items.lastOrNull() as? TranscriptItem.Failure)?.reason == FailureReason.AUTH_REQUIRED

    private fun withToolCall(toolCall: TranscriptItem.ToolCall): AssistantTranscript {
        val index = items.indexOfFirst { it is TranscriptItem.ToolCall && it.id == toolCall.id }
        return copy(items = if (index < 0) items + toolCall else items.toMutableList().also { it[index] = toolCall })
    }
}

/**
 * [items] with [text] added to the last item if it is of the same kind ([textOf] reads it), as the agent streams a
 * message in chunks, or as a new item otherwise.
 */
private fun List<TranscriptItem>.appending(
    text: String,
    textOf: (TranscriptItem) -> String?,
    create: (String) -> TranscriptItem,
): List<TranscriptItem> {
    val previous = lastOrNull()?.let(textOf) ?: return this + create(text)
    return dropLast(1) + create(previous + text)
}

private fun ContentBlock.text() = (this as? ContentBlock.Text)?.text
