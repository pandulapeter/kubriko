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
import com.agentclientprotocol.model.SessionModeId
import com.agentclientprotocol.model.SessionUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration.Companion.seconds

/**
 * One conversation (an ACP session) with an [AcpAgent]. It outlives the agent's process: after a crash the agent
 * loads it again where it can, and otherwise it is lost.
 *
 * @param prefersReadOnly Whether the conversation is switched to the agent's read-only mode ([ReadOnlyMode]) whenever it
 *   starts or is loaded again, where the agent offers one.
 */
public class AgentConversation internal constructor(
    internal val id: SessionId,
    internal val parameters: SessionCreationParameters,
    private val agent: AcpAgent,
    private val permissions: PermissionHandler,
    @Volatile private var firstPromptLead: String? = null,
    private val prefersReadOnly: Boolean = false,
) {
    private val _updates = MutableSharedFlow<SessionUpdate>(extraBufferCapacity = UPDATE_BUFFER_SIZE)

    /**
     * What the agent reports: its messages, thoughts, tool calls, and plans, between prompts and from prompts that take no
     * `onUpdate` of their own. The history an
     * agent replays when the conversation is loaded again after a crash is left out, since it was reported already.
     */
    internal val updates = _updates.asSharedFlow()
    private val _availableCommands = MutableStateFlow(emptyList<AvailableCommand>())

    /**
     * The slash commands the agent offers, which it may announce as soon as the conversation starts, before anything
     * collects [updates].
     */
    internal val availableCommands = _availableCommands.asStateFlow()
    private val _isLost = MutableStateFlow(false)

    /**
     * Whether the agent restarted without being able to load this conversation again, which then can't be continued.
     */
    internal val isLost = _isLost.asStateFlow()

    private val session = MutableStateFlow<ClientSession?>(null)
    private val unaskedChanges = UnaskedChanges()
    private val _hasChangedFilesUnasked = MutableStateFlow(false)

    /**
     * Whether the agent changed files or ran a command with its own tools without asking, since the conversation started
     * or was loaded ([UnaskedChanges]).
     */
    internal val hasChangedFilesUnasked = _hasChangedFilesUnasked.asStateFlow()

    /**
     * The options the agent offers for the conversation (its model, mode, or effort), with their current values.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val configOptions: Flow<List<SessionConfigOption>> = session.flatMapLatest { it?.configOptions ?: flowOf(emptyList()) }

    @Volatile
    private var isReplaying = false

    @Volatile
    private var historyCollector: (suspend (SessionUpdate) -> Unit)? = null

    internal val operations = object : ClientSessionOperations {
        override suspend fun requestPermissions(
            toolCall: SessionUpdate.ToolCallUpdate,
            permissions: List<PermissionOption>,
            _meta: JsonElement?,
        ): RequestPermissionResponse {
            unaskedChanges.asked(toolCall.toolCallId)
            return RequestPermissionResponse(this@AgentConversation.permissions.answer(toolCall, permissions))
        }

        override suspend fun notify(notification: SessionUpdate, _meta: JsonElement?) {
            note(notification)
            historyCollector?.let { collect ->
                collect(notification)
                return
            }
            if (isReplaying) return
            follow(notification)
            _updates.emit(notification)
        }
    }

    /**
     * Sends [content] and suspends until the agent has finished its turn, starting the agent first if it isn't running.
     * Cancelling the calling coroutine abandons the turn without telling the agent; [cancel] stops it.
     *
     * @param onUpdate Takes what the agent reports during the turn, in order and before this returns, in place of [updates].
     * @throws ConversationLostException If the conversation was lost.
     */
    internal suspend fun prompt(content: List<ContentBlock>, onUpdate: (suspend (SessionUpdate) -> Unit)? = null): PromptResponse {
        var response: PromptResponse? = null
        val lead = firstPromptLead
        agent.sessionOf(this).prompt(listOfNotNull(lead?.let(ContentBlock::Text)) + content).collect { event ->
            firstPromptLead = null
            when (event) {
                is Event.SessionUpdateEvent -> {
                    note(event.update)
                    follow(event.update)
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
    internal suspend fun cancel() {
        session.value?.cancel()
    }

    /**
     * Changes one of the [configOptions].
     *
     * @throws ConversationLostException If the conversation was lost.
     */
    internal suspend fun setConfigOption(id: SessionConfigId, value: SessionConfigOptionValue) {
        agent.sessionOf(this).setConfigOption(id, value)
    }

    /**
     * Ends the conversation. The agent forgets it if it can.
     */
    public suspend fun close(): Unit = agent.close(this)

    private fun follow(update: SessionUpdate) {
        if (unaskedChanges.completesUnaskedChange(update) { isReadOnly }) _hasChangedFilesUnasked.value = true
    }

    private fun note(update: SessionUpdate) {
        if (update is SessionUpdate.AvailableCommandsUpdate) _availableCommands.value = update.availableCommands
    }

    internal fun currentSession() = session.value

    /**
     * Whether the conversation is in the agent's read-only mode now.
     */
    internal val isReadOnly: Boolean
        get() {
            val session = session.value ?: return false
            val switched = modeSwitchedFrom
            return ReadOnlyMode.isActive(session) || (switched?.session === session && session.currentMode.value == switched.previous)
        }

    /**
     * The session mode the conversation left for [ReadOnlyMode.Mode], while the agent hasn't reported another mode since:
     * the SDK follows the mode only by the agent's reports, which an agent needn't send for a change the client asked for.
     */
    private class SwitchedMode(
        val session: ClientSession,
        val previous: SessionModeId,
    )

    @Volatile
    private var modeSwitchedFrom: SwitchedMode? = null

    /**
     * Makes [session] the conversation's, switching it to the agent's read-only mode where it is preferred and offered. A
     * conversation the agent refuses to switch goes on as it is, since the user is still asked before its own changes.
     */
    internal suspend fun attach(session: ClientSession) {
        this.session.value = session
        if (!prefersReadOnly) return
        val mode = ReadOnlyMode.of(session) ?: return
        val previousMode = if (mode is ReadOnlyMode.Mode) session.currentMode.value else null
        try {
            // Waited for under the agent's lifecycle lock, so an agent that never answers can't hold up the others.
            val isSwitched = withTimeoutOrNull(MODE_SWITCH_WAIT) { mode.enter(session) } != null
            if (isSwitched && previousMode != null) modeSwitchedFrom = SwitchedMode(session, previousMode)
        } catch (exception: CancellationException) {
            // The SDK fails the request with a cancellation of its own when the agent stops meanwhile.
            if (!currentCoroutineContext().isActive) throw exception
        } catch (_: Exception) {
        }
    }

    internal suspend fun <T> replaying(load: suspend () -> T): T {
        isReplaying = true
        try {
            return load()
        } finally {
            isReplaying = false
        }
    }

    /**
     * Runs [load], handing what the agent replays meanwhile (the conversation's history) to [collect] rather than [updates].
     */
    internal suspend fun <T> collectingHistory(collect: suspend (SessionUpdate) -> Unit, load: suspend () -> T): T {
        historyCollector = collect
        try {
            return load()
        } finally {
            historyCollector = null
        }
    }

    internal fun markLost() {
        session.value = null
        _isLost.value = true
    }

    private companion object {
        const val UPDATE_BUFFER_SIZE = 256
        val MODE_SWITCH_WAIT = 5.seconds
    }
}
