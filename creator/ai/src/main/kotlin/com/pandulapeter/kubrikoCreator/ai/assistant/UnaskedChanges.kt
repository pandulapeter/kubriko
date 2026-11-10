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

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import java.util.concurrent.ConcurrentHashMap

/**
 * Follows a conversation's tool calls to tell when the agent edited, deleted, or moved files with its own tools without
 * asking the user first: what an agent whose mode lets it change files does, which the user can then only go back on
 * through version control. Commands don't count, since nothing tells a harmless one (`ls`) from the rest; nor does a call
 * made in the conversation's read-only mode, or of a kind the user allowed always.
 */
internal class UnaskedChanges {
    private val asked = ConcurrentHashMap.newKeySet<ToolCallId>()
    private val calls = ConcurrentHashMap<ToolCallId, Call>()
    private val allowedAlways = ConcurrentHashMap.newKeySet<ToolKind>()

    private data class Call(
        val kind: ToolKind?,
        val title: String?,
    )

    /**
     * Notes that the agent asked the user about [toolCall], and returns it with the kind and title the agent gave the call
     * before, where it leaves them out of the question.
     */
    fun asked(toolCall: SessionUpdate.ToolCallUpdate): SessionUpdate.ToolCallUpdate {
        asked += toolCall.toolCallId
        val known = calls[toolCall.toolCallId] ?: return toolCall
        return toolCall.copy(kind = toolCall.kind ?: known.kind, title = toolCall.title ?: known.title)
    }

    /**
     * Notes the user's [outcome] for [toolCall]: allowing it always lets the agent make calls of its kind without asking.
     */
    fun answered(toolCall: SessionUpdate.ToolCallUpdate, options: List<PermissionOption>, outcome: RequestPermissionOutcome) {
        val chosen = (outcome as? RequestPermissionOutcome.Selected)?.optionId ?: return
        val kind = toolCall.kind ?: return
        if (options.any { it.optionId == chosen && it.kind == PermissionOptionKind.ALLOW_ALWAYS }) allowedAlways += kind
    }

    /**
     * Follows [update], and tells whether it completed a call that changed something without asking.
     */
    fun completesUnaskedChange(update: SessionUpdate, isReadOnly: () -> Boolean): Boolean {
        val (id, call, status) = when (update) {
            is SessionUpdate.ToolCall -> Triple(update.toolCallId, Call(update.kind, update.title), update.status)
            is SessionUpdate.ToolCallUpdate -> calls[update.toolCallId].let { known ->
                Triple(update.toolCallId, Call(update.kind ?: known?.kind, update.title ?: known?.title), update.status)
            }
            else -> return false
        }
        if (status != ToolCallStatus.COMPLETED && status != ToolCallStatus.FAILED) {
            calls[id] = call
            return false
        }
        calls.remove(id)
        val wasAsked = asked.remove(id)
        return status == ToolCallStatus.COMPLETED &&
            !wasAsked &&
            call.kind in fileChangingKinds &&
            call.kind !in allowedAlways &&
            CreatorToolName.of(call.title) == null &&
            !isReadOnly()
    }

    private companion object {
        val fileChangingKinds = setOf(ToolKind.EDIT, ToolKind.DELETE, ToolKind.MOVE)
    }
}
