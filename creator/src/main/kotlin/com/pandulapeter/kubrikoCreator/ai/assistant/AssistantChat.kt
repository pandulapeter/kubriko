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
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.protocol.JsonRpcException
import com.agentclientprotocol.rpc.JsonRpcErrorCode
import com.pandulapeter.kubrikoCreator.commands.ChangeLog
import com.pandulapeter.kubrikoCreator.context.ChangesSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * One conversation of the assistant panel: the [AgentConversation] it runs on, started right away by [start], and
 * everything the panel shows of it.
 *
 * @param parentScope Where the conversation's work runs: the main thread, which the panel's state is read on.
 * @param start Starts the conversation, or continues an earlier one, handing what came before to its second argument.
 * @param end Ends the conversation [start] started.
 * @param toolsIdOf The ID of the address at which a conversation reaches Kubriko Creator's tools.
 * @param undoTurn Reverts what the conversation at a tool address changed after a revision, returning how many of its
 *   changes stay ([com.pandulapeter.kubrikoCreator.commands.CommandExecutor.undoTurn]).
 * @param onTurnEnded Called after each turn with the conversation's ID, title, and the agent's last answer, so that it can
 *   be continued later.
 * @param changeLog The open project's changes, which a prompt sums up for the agent when someone else made them since
 *   its last turn ([ChangesSummary]).
 */
internal class AssistantChat(
    parentScope: CoroutineScope,
    private val start: suspend (PermissionHandler, onHistory: suspend (SessionUpdate) -> Unit) -> AgentConversation,
    private val end: suspend (AgentConversation) -> Unit,
    private val toolsIdOf: (AgentConversation) -> String? = { null },
    private val changeLog: () -> ChangeLog? = { null },
    private val onTurnEnded: suspend (id: String, title: String, lastAnswer: String?) -> Unit = { _, _, _ -> },
    private val undoTurn: suspend (toolsId: String, sinceRevision: Int) -> Int = { _, _ -> 0 },
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
    private val _conversationId = MutableStateFlow<String?>(null)

    /**
     * The agent's ID of the conversation, which continues it after a restart, once it has started.
     */
    val conversationId = _conversationId.asStateFlow()
    private val _toolsId = MutableStateFlow<String?>(null)

    /**
     * The ID of the address at which the conversation reaches Kubriko Creator's tools, which the AI steps it makes carry,
     * once it has started.
     */
    val toolsId = _toolsId.asStateFlow()

    @Volatile
    private var conversation: AgentConversation? = null
    private var seenRevision = 0
    private var pendingAnswer: CompletableDeferred<RequestPermissionOutcome>? = null
    private var isClosed = false
    private var lastContext = emptyList<ContentBlock>()

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
     * Sends [text] to the agent with [context] (what the user is looking at, [AttentionChip]) ahead of it, unless a
     * prompt runs or the conversation isn't ready.
     */
    fun send(text: String, context: List<ContentBlock> = emptyList()) {
        if (_isBusy.value || text.isBlank() || _state.value != ChatState.Ready || conversation == null) return
        _transcript.update { it.withUserMessage(text) }
        lastContext = context
        prompt(text)
    }

    /**
     * Sends the last prompt again if the agent refused it for want of a sign-in, now that the user has signed in.
     */
    fun resendAfterSignIn() {
        if (_isBusy.value || _state.value != ChatState.Ready || !_transcript.value.needsSignIn) return
        val text = _transcript.value.items.filterIsInstance<TranscriptItem.UserMessage>().lastOrNull()?.text ?: return
        _transcript.update { it.withoutLastItem() }
        prompt(text)
    }

    private fun prompt(text: String) {
        val conversation = conversation ?: return
        _isBusy.value = true
        val log = changeLog()
        val since = seenRevision
        val turnStart = log?.revision
        log?.let { seenRevision = it.revision }
        scope.launch {
            try {
                val changes = log?.let { ChangesSummary.of(it.since(since), since, _toolsId.value) }
                val content = listOfNotNull(changes?.let(ContentBlock::Text)) + lastContext + ContentBlock.Text(text)
                val response = conversation.prompt(content) { update -> _transcript.update { it.withUpdate(update) } }
                _transcript.update { it.withTurnEnd(response.stopReason) }
                if (log != null && turnStart != null) {
                    val toolsId = _toolsId.value
                    _transcript.update { transcript -> transcript.withTurnChanges(turnStart, log.since(turnStart).filter { it.step == null && it.origin?.conversationId == toolsId }) }
                }
                val title = _transcript.value.title
                if (title != null) onTurnEnded(conversation.id.value, title, _transcript.value.items.filterIsInstance<TranscriptItem.AgentMessage>().lastOrNull()?.text)
            } catch (exception: CancellationException) {
                if (!currentCoroutineContext().isActive) throw exception
                _transcript.update { it.withFailure(FailureReason.AGENT_STOPPED) }
            } catch (_: ConversationLostException) {
                _transcript.update { it.withFailure(FailureReason.CONVERSATION_LOST) }
                _state.value = ChatState.Failed(FailureReason.CONVERSATION_LOST)
            } catch (exception: AgentStartException) {
                _transcript.update { it.withFailure(FailureReason.AGENT_NOT_STARTED, exception.errorOutput.lastOrNull()) }
            } catch (exception: Exception) {
                _transcript.update {
                    if (exception.isAuthRequired()) it.withFailure(FailureReason.AUTH_REQUIRED) else it.withFailure(FailureReason.PROMPT_FAILED, exception.message)
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    /**
     * Keeps what the turn that began at [sinceRevision] changed.
     */
    fun keepChanges(sinceRevision: Int) = _transcript.update { it.withTurnChangesState(sinceRevision, TranscriptItem.TurnChanges.State.KEPT) }

    /**
     * Reverts what the turn that began at [sinceRevision] changed, up to a later change of someone else's.
     */
    fun undoChanges(sinceRevision: Int) {
        val toolsId = _toolsId.value ?: return
        scope.launch {
            val remaining = undoTurn(toolsId, sinceRevision)
            val state = if (remaining == 0) TranscriptItem.TurnChanges.State.UNDONE else TranscriptItem.TurnChanges.State.PARTLY_UNDONE
            _transcript.update { it.withTurnChangesState(sinceRevision, state) }
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
                if (!currentCoroutineContext().isActive) throw exception
                _transcript.update { it.withFailure(FailureReason.AGENT_STOPPED) }
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
                start(permissions) { update -> _transcript.update { it.withUpdate(update) } }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: NodeMissingException) {
                _state.value = ChatState.Failed(FailureReason.NODE_MISSING)
                return@launch
            } catch (_: ConversationLostException) {
                _state.value = ChatState.Failed(FailureReason.CONVERSATION_LOST)
                return@launch
            } catch (_: UvMissingException) {
                _state.value = ChatState.Failed(FailureReason.UV_MISSING)
                return@launch
            } catch (exception: AgentStartException) {
                _state.value = ChatState.Failed(FailureReason.AGENT_NOT_STARTED, exception.errorOutput)
                return@launch
            } catch (exception: Exception) {
                _state.value = ChatState.Failed(FailureReason.AGENT_NOT_STARTED, listOfNotNull(exception.message))
                return@launch
            }
            conversation = started
            _toolsId.value = toolsIdOf(started)
            _conversationId.value = started.id.value
            seenRevision = changeLog()?.revision ?: 0
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

/**
 * Whether this is the agent's `authRequired` error, which an agent answers a prompt with until the user signs in.
 */
private fun Throwable.isAuthRequired() = generateSequence(this) { it.cause }.any { it is JsonRpcException && it.code == JsonRpcErrorCode.AUTH_REQUIRED.code }
