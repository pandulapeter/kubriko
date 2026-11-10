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

import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import java.util.concurrent.ConcurrentHashMap

/**
 * Follows a conversation's tool calls to tell when the agent edited, deleted, or moved files or ran a command with its own
 * tools without asking the user first: what an agent whose mode lets it change files does, which the user can then only
 * go back on through version control. A call made while the conversation is in its read-only mode doesn't count, since
 * that mode keeps even its commands off the files.
 */
internal class UnaskedChanges {
    private val asked = ConcurrentHashMap.newKeySet<ToolCallId>()
    private val calls = ConcurrentHashMap<ToolCallId, Call>()

    private data class Call(
        val kind: ToolKind?,
        val title: String?,
    )

    /**
     * Notes that the agent asked the user about the call [id].
     */
    fun asked(id: ToolCallId) {
        asked += id
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
            call.kind in PermissionHandler.changingKinds &&
            CreatorToolName.of(call.title) == null &&
            !isReadOnly()
    }
}
