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
import com.agentclientprotocol.model.ToolKind

/**
 * Answers an agent asking whether it may run a tool call.
 */
internal fun interface PermissionHandler {

    suspend fun answer(toolCall: SessionUpdate.ToolCallUpdate, options: List<PermissionOption>): RequestPermissionOutcome

    companion object {

        /**
         * Rejects every request once, or cancels it if the agent offers no way to reject it.
         */
        val Reject = PermissionHandler { _, options ->
            options.firstOrNull { it.kind == PermissionOptionKind.REJECT_ONCE }
                ?.let { RequestPermissionOutcome.Selected(it.optionId) }
                ?: RequestPermissionOutcome.Cancelled
        }

        /**
         * Allows Kubriko Creator's own tools ([CreatorToolName]) once, since their changes show up in the editor and can be
         * undone, and leaves the rest to [other]: what Claude Agent's `allowedTools` does, for agents that ask.
         */
        fun allowingCreatorTools(other: PermissionHandler) = PermissionHandler { toolCall, options ->
            val allow = options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ONCE } ?: options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ALWAYS }
            if (allow != null && CreatorToolName.of(toolCall.title) != null) RequestPermissionOutcome.Selected(allow.optionId) else other.answer(toolCall, options)
        }

        private val changingKinds = setOf(ToolKind.EDIT, ToolKind.DELETE, ToolKind.MOVE, ToolKind.EXECUTE)

        /**
         * Rejects requests for tool calls that edit, delete, or move files or run commands, and leaves the rest to
         * [other]: how an agent that has no switch for its own tools is kept off the project's files.
         */
        fun rejectingChanges(other: PermissionHandler) = PermissionHandler { toolCall, options ->
            if (toolCall.kind in changingKinds) Reject.answer(toolCall, options) else other.answer(toolCall, options)
        }
    }
}
