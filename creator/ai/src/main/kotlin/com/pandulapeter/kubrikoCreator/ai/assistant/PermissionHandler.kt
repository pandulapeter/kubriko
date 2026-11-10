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
public fun interface PermissionHandler {

    /**
     * Chooses one of [options] for the agent's [toolCall], or cancels it.
     */
    public suspend fun answer(toolCall: SessionUpdate.ToolCallUpdate, options: List<PermissionOption>): RequestPermissionOutcome

    public companion object {

        /**
         * Rejects every request once, or cancels it if the agent offers no way to reject it.
         */
        internal val Reject = PermissionHandler { _, options ->
            options.firstOrNull { it.kind == PermissionOptionKind.REJECT_ONCE }
                ?.let { RequestPermissionOutcome.Selected(it.optionId) }
                ?: RequestPermissionOutcome.Cancelled
        }

        /**
         * Allows Kubriko Creator's own tools ([CreatorToolName]) once when their kind changes no files and runs nothing,
         * since their changes show up in the editor and can be undone, and leaves the rest to [other]: what Claude Agent's
         * `allowedTools` does, for agents that ask. The title is the agent's own, so a call titled as one of Creator's
         * tools but of a kind that edits, deletes, moves, or runs something is left to [other].
         */
        internal fun allowingCreatorTools(other: PermissionHandler) = PermissionHandler { toolCall, options ->
            val allow = options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ONCE } ?: options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ALWAYS }
            val isCreatorTool = CreatorToolName.of(toolCall.title) != null && toolCall.kind !in changingKinds
            if (allow != null && isCreatorTool) RequestPermissionOutcome.Selected(allow.optionId) else other.answer(toolCall, options)
        }

        /**
         * The kinds of tool calls that edit, delete, or move files or run commands.
         */
        internal val changingKinds = setOf(ToolKind.EDIT, ToolKind.DELETE, ToolKind.MOVE, ToolKind.EXECUTE)
    }
}
