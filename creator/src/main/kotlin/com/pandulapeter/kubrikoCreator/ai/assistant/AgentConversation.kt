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

import com.agentclientprotocol.client.ClientSession
import com.agentclientprotocol.common.ClientSessionOperations
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PromptResponse
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement

/**
 * One conversation (an ACP session) with an [AcpAgent]. It outlives the agent's process: after a crash the agent
 * loads it again where it can, and otherwise it is lost.
 */
internal class AgentConversation internal constructor(
    val id: SessionId,
    internal val parameters: SessionCreationParameters,
    private val agent: AcpAgent,
    private val permissions: PermissionHandler,
) {
    private val _updates = MutableSharedFlow<SessionUpdate>(extraBufferCapacity = UPDATE_BUFFER_SIZE)

    /**
     * What the agent reports: its messages, thoughts, tool calls, and plans, from prompts and between them. The history an
     * agent replays when the conversation is loaded again after a crash is left out, since it was reported already.
     */
    val updates = _updates.asSharedFlow()
    private val _isLost = MutableStateFlow(false)

    /**
     * Whether the agent restarted without being able to load this conversation again, which then can't be continued.
     */
    val isLost = _isLost.asStateFlow()

    @Volatile
    private var session: ClientSession? = null

    @Volatile
    private var isReplaying = false

    internal val operations = object : ClientSessionOperations {
        override suspend fun requestPermissions(
            toolCall: SessionUpdate.ToolCallUpdate,
            permissions: List<PermissionOption>,
            _meta: JsonElement?,
        ) = RequestPermissionResponse(this@AgentConversation.permissions.answer(toolCall, permissions))

        override suspend fun notify(notification: SessionUpdate, _meta: JsonElement?) {
            if (!isReplaying) _updates.emit(notification)
        }
    }

    /**
     * Sends [content] and suspends until the agent has finished its turn, starting the agent first if it isn't running.
     * Cancelling the calling coroutine abandons the turn without telling the agent; [cancel] stops it.
     *
     * @throws ConversationLostException If the conversation was lost.
     */
    suspend fun prompt(content: List<ContentBlock>): PromptResponse {
        var response: PromptResponse? = null
        agent.sessionOf(this).prompt(content).collect { event ->
            when (event) {
                is Event.SessionUpdateEvent -> _updates.emit(event.update)
                is Event.PromptResponseEvent -> response = event.response
            }
        }
        return checkNotNull(response) { "The agent ended the prompt without a response." }
    }

    /**
     * Asks the agent to stop its current turn, which then ends with [com.agentclientprotocol.model.StopReason.CANCELLED].
     */
    suspend fun cancel() {
        session?.cancel()
    }

    /**
     * Ends the conversation. The agent forgets it if it can.
     */
    suspend fun close() = agent.close(this)

    internal fun currentSession() = session

    internal fun attach(session: ClientSession) {
        this.session = session
    }

    internal suspend fun <T> replaying(load: suspend () -> T): T {
        isReplaying = true
        try {
            return load()
        } finally {
            isReplaying = false
        }
    }

    internal fun markLost() {
        session = null
        _isLost.value = true
    }

    private companion object {
        const val UPDATE_BUFFER_SIZE = 256
    }
}
