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
import com.pandulapeter.kubrikoCreator.commands.UserQuestions

/**
 * Where a conversation shows the questions its agent asked: each under the tool call that asked it, and the ones whose
 * call isn't in the transcript (an agent that sends no call IDs) after everything else.
 */
internal data class TranscriptQuestions(
    val byToolCall: Map<ToolCallId, UserQuestions.Question>,
    val trailing: List<UserQuestions.Question>,
) {
    /**
     * Whether the conversation has a question nobody answered yet, whether or not its call still waits.
     */
    val isAnyOpen: Boolean get() = byToolCall.isNotEmpty() || trailing.isNotEmpty()

    companion object {
        val None = TranscriptQuestions(emptyMap(), emptyList())
    }
}

/**
 * The questions of the conversation whose tools are at [toolsId] among [questions], placed in [items].
 */
internal fun questionsOf(items: List<TranscriptItem>, questions: List<UserQuestions.Question>, toolsId: String?): TranscriptQuestions {
    if (toolsId == null) return TranscriptQuestions.None
    val own = questions.filter { it.origin?.conversationId == toolsId }
    if (own.isEmpty()) return TranscriptQuestions.None
    val callIds = items.mapNotNullTo(HashSet()) { (it as? TranscriptItem.ToolCall)?.id?.value }
    val (placed, trailing) = own.partition { it.origin?.toolCallId in callIds }
    return TranscriptQuestions(placed.associateBy { ToolCallId(it.origin!!.toolCallId!!) }, trailing)
}

/**
 * The questions the agent waits for the user to answer, rather than working, the oldest first: every one whose asking call
 * still runs, and those whose call isn't known.
 */
internal fun TranscriptQuestions.waitingQuestions(items: List<TranscriptItem>, isBusy: Boolean): List<UserQuestions.Question> {
    if (!isBusy) return emptyList()
    val placed = items.mapNotNull { item -> (item as? TranscriptItem.ToolCall)?.takeIf { !isLate(it, isBusy = true) }?.let { byToolCall[it.id] } }
    return (placed + trailing).sortedBy { it.id }
}

/**
 * Whether the agent stopped waiting for a question's answer: its turn ended, or the call that asked it ([toolCall], when
 * known) did, as a call the agent gave up on does.
 */
internal fun isLate(toolCall: TranscriptItem.ToolCall?, isBusy: Boolean) = !isBusy || toolCall?.status == ToolCallStatus.COMPLETED || toolCall?.status == ToolCallStatus.FAILED
