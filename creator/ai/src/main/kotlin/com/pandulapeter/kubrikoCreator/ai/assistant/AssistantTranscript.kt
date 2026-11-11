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
import com.agentclientprotocol.model.PlanEntryStatus
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry

/**
 * A conversation as the assistant panel shows it, built up from what the user sends and the agent reports.
 *
 * @param plan The agent's latest plan, shown as a checklist.
 * @param isPlanNew Whether the agent reported [plan] after the user's last message and changed nothing since, in a turn
 *   that ended as the agent meant it to, so the user hasn't answered it yet and nothing of it has been done.
 * @param isPlanFromCreator Whether the agent lays its plan out with Kubriko Creator's `set_plan`, whose plans then take the
 *   place of any it reports through ACP, so that two plans of one agent never take turns on the checklist.
 * @param usage What the conversation uses and cost, where the agent reports it.
 */
public data class AssistantTranscript(
    val items: List<TranscriptItem> = emptyList(),
    val plan: List<PlanEntry> = emptyList(),
    val isPlanNew: Boolean = false,
    val isPlanFromCreator: Boolean = false,
    val usage: ConversationUsage? = null,
) {
    /**
     * The first thing the user asked, which names the conversation.
     */
    public val title: String? get() = items.firstNotNullOfOrNull { (it as? TranscriptItem.UserMessage)?.text }

    /**
     * Whether the agent laid out a plan in its last turn and started none of it, which the user approves (or changes)
     * before it goes on. An agent may never mark its plan's steps as done, so a change it made after laying the plan out
     * counts as starting it too.
     */
    public val isPlanAwaitingApproval: Boolean get() = isPlanNew && plan.isNotEmpty() && plan.all { it.status == PlanEntryStatus.PENDING }

    internal fun withUserMessage(text: String) = copy(items = items + TranscriptItem.UserMessage(text), isPlanNew = false)

    internal fun withUpdate(update: SessionUpdate): AssistantTranscript = when (update) {
        is SessionUpdate.AgentMessageChunk -> update.content.text()?.let { text ->
            copy(items = items.appending(text, { (it as? TranscriptItem.AgentMessage)?.text }, TranscriptItem::AgentMessage))
        } ?: this
        is SessionUpdate.AgentThoughtChunk -> update.content.text()?.let { text ->
            copy(items = items.appending(text, { (it as? TranscriptItem.Thought)?.text }, TranscriptItem::Thought))
        } ?: this
        is SessionUpdate.UserMessageChunk -> update.content.text()?.let { text ->
            copy(items = items.appending(text, { (it as? TranscriptItem.UserMessage)?.text }, TranscriptItem::UserMessage), isPlanNew = false)
        } ?: this
        is SessionUpdate.ToolCall -> withToolCall(TranscriptItem.ToolCall(update.toolCallId, update.title, update.kind, update.status)).let { transcript ->
            if (transcript.isPlanNew && changesProject(update.title)) transcript.copy(isPlanNew = false) else transcript
        }
        is SessionUpdate.ToolCallUpdate -> {
            val previous = items.firstOrNull { it is TranscriptItem.ToolCall && it.id == update.toolCallId } as? TranscriptItem.ToolCall
            withToolCall(
                TranscriptItem.ToolCall(
                    id = update.toolCallId,
                    title = update.title ?: previous?.title.orEmpty(),
                    kind = update.kind ?: previous?.kind,
                    status = update.status ?: previous?.status,
                ),
            ).let { transcript ->
                val title = update.title
                if (transcript.isPlanNew && title != null && title != previous?.title && changesProject(title)) transcript.copy(isPlanNew = false) else transcript
            }
        }
        is SessionUpdate.PlanUpdate -> if (isPlanFromCreator) this else withPlan(update.entries)
        is SessionUpdate.PlanRemoved -> if (isPlanFromCreator) this else copy(plan = emptyList(), isPlanNew = false)
        is SessionUpdate.UsageUpdate -> copy(usage = ConversationUsage(update.used, update.size, update.cost?.amount, update.cost?.currency))
        else -> this
    }

    /**
     * Shows [entries], the plan the agent laid out with Kubriko Creator's `set_plan`, from now on in place of any it reports
     * through ACP.
     */
    internal fun withCreatorPlan(entries: List<PlanEntry>) = withPlan(entries).copy(isPlanFromCreator = true)

    private fun withPlan(entries: List<PlanEntry>) = copy(plan = entries, isPlanNew = true)

    /**
     * Notes how a turn ended, unless the agent simply finished it.
     */
    internal fun withTurnEnd(reason: StopReason) = if (reason == StopReason.END_TURN) this else copy(items = items + TranscriptItem.TurnEnded(reason), isPlanNew = false)

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

    internal fun withFailure(reason: FailureReason, details: String? = null) = copy(items = items + TranscriptItem.Failure(reason, details), isPlanNew = false)

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

/**
 * Whether the tool call titled [title] is one of Kubriko Creator's that changes something, which only its tools can.
 */
private fun changesProject(title: String) = CreatorToolName.of(title)?.let { name -> creatorToolsChanging[name] } == true

private val creatorToolsChanging by lazy { CommandRegistry.editor.tools.associate { it.name to !it.isReadOnly } }
