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
import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PromptResponse
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
     * What the agent reports: its messages, thoughts, tool calls, and plans, between prompts and from prompts that take no
     * `onUpdate` of their own. The history an
     * agent replays when the conversation is loaded again after a crash is left out, since it was reported already.
     */
    val updates = _updates.asSharedFlow()
    private val _availableCommands = MutableStateFlow(emptyList<AvailableCommand>())

    /**
     * The slash commands the agent offers, which it may announce as soon as the conversation starts, before anything
     * collects [updates].
     */
    val availableCommands = _availableCommands.asStateFlow()
    private val _isLost = MutableStateFlow(false)

    /**
     * Whether the agent restarted without being able to load this conversation again, which then can't be continued.
     */
    val isLost = _isLost.asStateFlow()

    private val session = MutableStateFlow<ClientSession?>(null)

    /**
     * The options the agent offers for the conversation (its model, mode, or effort), with their current values.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val configOptions: Flow<List<SessionConfigOption>> = session.flatMapLatest { it?.configOptions ?: flowOf(emptyList()) }

    @Volatile
    private var isReplaying = false

    internal val operations = object : ClientSessionOperations {
        override suspend fun requestPermissions(
            toolCall: SessionUpdate.ToolCallUpdate,
            permissions: List<PermissionOption>,
            _meta: JsonElement?,
        ) = RequestPermissionResponse(this@AgentConversation.permissions.answer(toolCall, permissions))

        override suspend fun notify(notification: SessionUpdate, _meta: JsonElement?) {
            note(notification)
            if (!isReplaying) _updates.emit(notification)
        }
    }

    /**
     * Sends [content] and suspends until the agent has finished its turn, starting the agent first if it isn't running.
     * Cancelling the calling coroutine abandons the turn without telling the agent; [cancel] stops it.
     *
     * @param onUpdate Takes what the agent reports during the turn, in order and before this returns, in place of [updates].
     * @throws ConversationLostException If the conversation was lost.
     */
    suspend fun prompt(content: List<ContentBlock>, onUpdate: (suspend (SessionUpdate) -> Unit)? = null): PromptResponse {
        var response: PromptResponse? = null
        agent.sessionOf(this).prompt(content).collect { event ->
            when (event) {
                is Event.SessionUpdateEvent -> {
                    note(event.update)
                    onUpdate?.invoke(event.update) ?: _updates.emit(event.update)
                }
                is Event.PromptResponseEvent -> response = event.response
            }
        }
        return checkNotNull(response) { "The agent ended the prompt without a response." }
    }

    /**
     * Asks the agent to stop its current turn, which then ends with [com.agentclientprotocol.model.StopReason.CANCELLED].
     */
    suspend fun cancel() {
        session.value?.cancel()
    }

    /**
     * Changes one of the [configOptions].
     *
     * @throws ConversationLostException If the conversation was lost.
     */
    suspend fun setConfigOption(id: SessionConfigId, value: SessionConfigOptionValue) {
        agent.sessionOf(this).setConfigOption(id, value)
    }

    /**
     * Ends the conversation. The agent forgets it if it can.
     */
    suspend fun close() = agent.close(this)

    private fun note(update: SessionUpdate) {
        if (update is SessionUpdate.AvailableCommandsUpdate) _availableCommands.value = update.availableCommands
    }

    internal fun currentSession() = session.value

    internal fun attach(session: ClientSession) {
        this.session.value = session
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
        session.value = null
        _isLost.value = true
    }

    private companion object {
        const val UPDATE_BUFFER_SIZE = 256
    }
}
