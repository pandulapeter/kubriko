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

import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One conversation of the assistant panel: the [AgentConversation] it runs on, started right away by [start], and
 * everything the panel shows of it.
 *
 * @param parentScope Where the conversation's work runs: the main thread, which the panel's state is read on.
 * @param end Ends the conversation [start] started.
 */
internal class AssistantChat(
    parentScope: CoroutineScope,
    private val start: suspend (PermissionHandler) -> AgentConversation,
    private val end: suspend (AgentConversation) -> Unit,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    private val _state = MutableStateFlow<ChatState>(ChatState.Starting)
    val state = _state.asStateFlow()
    private val _transcript = MutableStateFlow(AssistantTranscript())
    val transcript = _transcript.asStateFlow()
    private val _isBusy = MutableStateFlow(false)

    /**
     * Whether a prompt runs, until the agent ends its turn.
     */
    val isBusy = _isBusy.asStateFlow()
    private val _permissionRequest = MutableStateFlow<PermissionRequest?>(null)

    /**
     * What the agent waits for the user to allow or reject.
     */
    val permissionRequest = _permissionRequest.asStateFlow()
    private val _availableCommands = MutableStateFlow(emptyList<AvailableCommand>())

    /**
     * The slash commands the agent offers.
     */
    val availableCommands = _availableCommands.asStateFlow()
    private val _configOptions = MutableStateFlow(emptyList<SessionConfigOption>())

    /**
     * The agent's options for the conversation, such as its model.
     */
    val configOptions: StateFlow<List<SessionConfigOption>> = _configOptions.asStateFlow()

    @Volatile
    private var conversation: AgentConversation? = null
    private var pendingAnswer: CompletableDeferred<RequestPermissionOutcome>? = null
    private var isClosed = false

    private val permissions = PermissionHandler { toolCall, options ->
        val answer = CompletableDeferred<RequestPermissionOutcome>()
        pendingAnswer?.complete(RequestPermissionOutcome.Cancelled)
        pendingAnswer = answer
        _permissionRequest.value = PermissionRequest(toolCall, options)
        try {
            answer.await()
        } finally {
            if (pendingAnswer === answer) {
                pendingAnswer = null
                _permissionRequest.value = null
            }
        }
    }

    init {
        connect()
    }

    /**
     * Starts the conversation again after it failed to start.
     */
    fun retry() {
        if (_state.value is ChatState.Failed && conversation == null) connect()
    }

    /**
     * Sends [text] to the agent, unless a prompt runs or the conversation isn't ready.
     */
    fun send(text: String) {
        val conversation = conversation ?: return
        if (_isBusy.value || text.isBlank() || _state.value != ChatState.Ready) return
        _transcript.update { it.withUserMessage(text) }
        _isBusy.value = true
        scope.launch {
            try {
                val response = conversation.prompt(listOf(ContentBlock.Text(text))) { update -> _transcript.update { it.withUpdate(update) } }
                _transcript.update { it.withTurnEnd(response.stopReason) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: ConversationLostException) {
                _transcript.update { it.withFailure(FailureReason.CONVERSATION_LOST) }
                _state.value = ChatState.Failed(FailureReason.CONVERSATION_LOST)
            } catch (exception: AgentStartException) {
                _transcript.update { it.withFailure(FailureReason.AGENT_NOT_STARTED, exception.errorOutput.lastOrNull()) }
            } catch (exception: Exception) {
                _transcript.update { it.withFailure(FailureReason.PROMPT_FAILED, exception.message) }
            } finally {
                _isBusy.value = false
            }
        }
    }

    /**
     * Asks the agent to stop its turn, rejecting what it waits to be allowed.
     */
    fun stop() {
        pendingAnswer?.complete(RequestPermissionOutcome.Cancelled)
        val conversation = conversation ?: return
        if (_isBusy.value) scope.launch { conversation.cancel() }
    }

    /**
     * Answers the agent's [permissionRequest] with [option].
     */
    fun answer(option: PermissionOption) {
        pendingAnswer?.complete(RequestPermissionOutcome.Selected(option.optionId))
    }

    fun setConfigOption(id: SessionConfigId, value: SessionConfigOptionValue) {
        val conversation = conversation ?: return
        scope.launch {
            try {
                conversation.setConfigOption(id, value)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _transcript.update { it.withFailure(FailureReason.PROMPT_FAILED, exception.message) }
            }
        }
    }

    /**
     * Ends the conversation.
     */
    suspend fun close() {
        isClosed = true
        pendingAnswer?.complete(RequestPermissionOutcome.Cancelled)
        scope.cancel()
        conversation?.let { end(it) }
        conversation = null
    }

    private fun connect() {
        _state.value = ChatState.Starting
        scope.launch {
            val started = try {
                start(permissions)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: NodeMissingException) {
                _state.value = ChatState.Failed(FailureReason.NODE_MISSING)
                return@launch
            } catch (exception: AgentStartException) {
                _state.value = ChatState.Failed(FailureReason.AGENT_NOT_STARTED, exception.errorOutput)
                return@launch
            } catch (exception: Exception) {
                _state.value = ChatState.Failed(FailureReason.AGENT_NOT_STARTED, listOfNotNull(exception.message))
                return@launch
            }
            conversation = started
            _state.value = ChatState.Ready
            scope.launch(start = CoroutineStart.UNDISPATCHED) { started.updates.collect { update -> _transcript.update { it.withUpdate(update) } } }
            scope.launch(start = CoroutineStart.UNDISPATCHED) { started.configOptions.collect { _configOptions.value = it } }
            scope.launch(start = CoroutineStart.UNDISPATCHED) { started.availableCommands.collect { _availableCommands.value = it } }
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                started.isLost.collect { isLost -> if (isLost && !isClosed) _state.value = ChatState.Failed(FailureReason.CONVERSATION_LOST) }
            }
        }
    }
}
