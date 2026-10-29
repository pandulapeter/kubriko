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

import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer

/**
 * One entry of a conversation as the assistant panel shows it.
 */
internal sealed interface TranscriptItem {

    data class UserMessage(
        val text: String,
    ) : TranscriptItem

    /**
     * What the agent wrote, in Markdown, its chunks joined.
     */
    data class AgentMessage(
        val text: String,
    ) : TranscriptItem

    data class Thought(
        val text: String,
    ) : TranscriptItem

    /**
     * @param title The agent's title of the call: for one of Kubriko Creator's tools, usually its MCP name.
     */
    data class ToolCall(
        val id: ToolCallId,
        val title: String,
        val kind: ToolKind? = null,
        val status: ToolCallStatus? = null,
    ) : TranscriptItem {

        /**
         * The name of the Kubriko Creator tool the call runs, or null if it runs one of the agent's own.
         */
        val creatorToolName get() = title.removePrefix(CREATOR_TOOL_PREFIX).takeIf { title.startsWith(CREATOR_TOOL_PREFIX) }

        private companion object {
            val CREATOR_TOOL_PREFIX = "mcp__${McpToolServer.SERVER_NAME}__"
        }
    }

    /**
     * A turn that ended other than by the agent finishing it: stopped by the user, refused, or cut short.
     */
    data class TurnEnded(
        val reason: StopReason,
    ) : TranscriptItem

    /**
     * Something that went wrong with a prompt.
     *
     * @param details The error's own words, if it had any.
     */
    data class Failure(
        val reason: FailureReason,
        val details: String? = null,
    ) : TranscriptItem
}
