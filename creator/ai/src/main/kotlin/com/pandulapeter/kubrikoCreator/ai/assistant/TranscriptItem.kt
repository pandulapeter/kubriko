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
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord

/**
 * One entry of a conversation as the assistant panel shows it.
 */
public sealed interface TranscriptItem {

    /**
     * A prompt the user sent.
     */
    public data class UserMessage(
        val text: String,
    ) : TranscriptItem

    /**
     * What the agent wrote, in Markdown, its chunks joined.
     */
    public data class AgentMessage(
        val text: String,
    ) : TranscriptItem

    /**
     * The agent's reasoning, as it shares it.
     */
    public data class Thought(
        val text: String,
    ) : TranscriptItem

    /**
     * @param title The agent's title of the call: for one of Kubriko Creator's tools, usually its MCP name.
     */
    public data class ToolCall(
        val id: ToolCallId,
        val title: String,
        val kind: ToolKind? = null,
        val status: ToolCallStatus? = null,
    ) : TranscriptItem {

        /**
         * The name of the Kubriko Creator tool the call runs, or null if it runs one of the agent's own.
         */
        public val creatorToolName: String? get() = CreatorToolName.of(title)
    }

    /**
     * What a turn changed in the project, shown before the agent's final answer.
     *
     * @param sinceRevision The project's revision when the turn began.
     */
    public data class TurnChanges(
        val sinceRevision: Int,
        val changes: List<ChangeRecord>,
    ) : TranscriptItem

    /**
     * Keep and Undo for what a turn changed, after the agent's final answer, until one is chosen, and then which one was.
     *
     * @param sinceRevision The project's revision when the turn began.
     */
    public data class TurnReview(
        val sinceRevision: Int,
        val state: State = State.OFFERED,
    ) : TranscriptItem {
        /**
         * Whether the turn's changes are still offered for review, kept, or undone.
         */
        public enum class State {
            OFFERED,
            KEPT,
            UNDONE,

            /**
             * Undone up to a later change of someone else's, which stopped it.
             */
            PARTLY_UNDONE,
        }
    }

    /**
     * A turn that ended other than by the agent finishing it: stopped by the user, refused, or cut short.
     */
    public data class TurnEnded(
        val reason: StopReason,
    ) : TranscriptItem

    /**
     * Something that went wrong with a prompt.
     *
     * @param details The error's own words, if it had any.
     */
    public data class Failure(
        val reason: FailureReason,
        val details: String? = null,
    ) : TranscriptItem
}
