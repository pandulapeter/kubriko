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

import com.agentclientprotocol.agent.AgentInfo
import com.agentclientprotocol.agent.AgentSession
import com.agentclientprotocol.agent.AgentSupport
import com.agentclientprotocol.agent.client
import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.AgentCapabilities
import com.agentclientprotocol.model.AuthEnvVar
import com.agentclientprotocol.model.AuthMethod
import com.agentclientprotocol.model.AuthMethodId
import com.agentclientprotocol.model.AuthenticateResponse
import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.CloseSessionResponse
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.McpCapabilities
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionId
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.PlanEntry
import com.agentclientprotocol.model.PlanEntryPriority
import com.agentclientprotocol.model.PlanEntryStatus
import com.agentclientprotocol.model.PromptResponse
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionCapabilities
import com.agentclientprotocol.model.SessionCloseCapabilities
import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionCategory
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.agentclientprotocol.model.SessionConfigValueId
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionMode
import com.agentclientprotocol.model.SessionModeId
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.SetSessionConfigOptionResponse
import com.agentclientprotocol.model.SetSessionModeResponse
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.agentclientprotocol.protocol.JsonRpcException
import com.agentclientprotocol.rpc.JsonRpcErrorCode
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

/**
 * An ACP agent built on the Kotlin SDK's agent side, answering prompts by their text:
 * - `hello`: two message chunks, then the end of the turn;
 * - `edit`: asks for permission to edit, then says which option was chosen (or `cancelled`);
 * - `wait`: works until cancelled;
 * - `crash`: the process ends with exit code 1;
 * - `later`: ends the turn, then sends a message outside any prompt;
 * - `tool`: a plan, a tool call of Kubriko Creator's that completes, and a message;
 * - `mode`: says whether the conversation is in its read-only mode (`read-only`) or not (`writable`).
 *
 * While [isSignedIn] is false, every prompt is refused with the `authRequired` error. The agent offers an agent sign-in
 * method and a terminal one, whose `_meta["terminal-auth"]` runs [TERMINAL_LOGIN].
 *
 * Every conversation offers a `model` option, `sonnet` or `opus`, and announces a `compact` slash command as soon as it starts.
 *
 * @param readOnlyMode How the agent offers a read-only mode, if it does, which conversations start without.
 * @param supportsHttpMcp Whether the agent advertises taking HTTP MCP servers.
 * @param exitsAtStart The exit code the process ends with right after starting, or null if it doesn't.
 * @param sessionClose Run when a conversation is closed (`session/close`), which the agent then advertises; without
 *   it, the agent can't close conversations.
 * @param crashExitDelay How long after its streams close a `crash` ends the process, as a real process exits a moment
 *   after its output ends.
 */
public class FakeAgent(
    private val supportsLoading: Boolean = true,
    private val supportsHttpMcp: Boolean = false,
    val exitsAtStart: Int? = null,
    private val answersInitialize: Boolean = true,
    private val isSignedIn: AtomicBoolean = AtomicBoolean(true),
    private val sessionClose: (suspend () -> Unit)? = null,
    private val crashExitDelay: Duration = Duration.ZERO,
    private val readOnlyMode: ReadOnlyOffer = ReadOnlyOffer.NONE,
) {
    /**
     * How an agent offers a read-only mode: not at all, as a value of its `sandbox` config option (`auto` or `read-only`),
     * or as one of its session modes (`default` or `read-only`).
     */
    enum class ReadOnlyOffer {
        NONE,
        CONFIG_OPTION,
        MODE,
    }

    val clientInfos = CopyOnWriteArrayList<ClientInfo>()
    val newSessions = CopyOnWriteArrayList<SessionCreationParameters>()
    val loadedSessions = CopyOnWriteArrayList<SessionId>()
    val loadedSessionParameters = CopyOnWriteArrayList<SessionCreationParameters>()
    val prompts = CopyOnWriteArrayList<String>()

    /**
     * What came before the user's text in the prompts: what Kubriko Creator told the agent on top.
     */
    val leadingTexts = CopyOnWriteArrayList<String>()

    /**
     * What else came before the user's text: links to what the user was looking at.
     */
    val leadingBlocks = CopyOnWriteArrayList<ContentBlock>()
    val authenticated = CopyOnWriteArrayList<AuthMethodId>()

    fun support(channel: FakeAgentChannel) = object : AgentSupport {
        override suspend fun initialize(clientInfo: ClientInfo): AgentInfo {
            clientInfos += clientInfo
            if (!answersInitialize) awaitCancellation()
            channel.writeError("Fake agent started.")
            return AgentInfo(
                capabilities = AgentCapabilities(
                    loadSession = supportsLoading,
                    mcpCapabilities = McpCapabilities(http = supportsHttpMcp),
                    sessionCapabilities = SessionCapabilities(close = sessionClose?.let { SessionCloseCapabilities() }),
                ),
                authMethods = listOf(
                    AuthMethod.AgentAuth(AuthMethodId(AGENT_LOGIN), "Fake login"),
                    AuthMethod.TerminalAuth(
                        id = AuthMethodId("terminal-login"),
                        name = "Terminal login",
                        args = listOf("--cli", "login"),
                        _meta = buildJsonObject {
                            putJsonObject("terminal-auth") {
                                put("command", TERMINAL_LOGIN)
                                putJsonArray("args") { add("login") }
                            }
                        },
                    ),
                    AuthMethod.EnvVarAuth(AuthMethodId("variables"), "Variables", vars = listOf(AuthEnvVar("FAKE_API_KEY"))),
                ),
            )
        }

        override suspend fun authenticate(methodId: AuthMethodId, _meta: JsonElement?): AuthenticateResponse {
            authenticated += methodId
            isSignedIn.set(true)
            return AuthenticateResponse()
        }

        override suspend fun createSession(sessionParameters: SessionCreationParameters): AgentSession {
            newSessions += sessionParameters
            return Session(SessionId("session-${sessionCount.incrementAndGet()}"), channel, isLoaded = false)
        }

        override suspend fun loadSession(sessionId: SessionId, sessionParameters: SessionCreationParameters): AgentSession {
            loadedSessions += sessionId
            loadedSessionParameters += sessionParameters
            return Session(sessionId, channel, isLoaded = true)
        }
    }

    private inner class Session(
        override val sessionId: SessionId,
        private val channel: FakeAgentChannel,
        private val isLoaded: Boolean,
    ) : AgentSession {
        private var model = "sonnet"
        private var sandbox = AUTO
        private var mode = DEFAULT_MODE

        override val availableModes
            get() = if (readOnlyMode == ReadOnlyOffer.MODE) listOf(SessionMode(SessionModeId(DEFAULT_MODE), "Default"), SessionMode(SessionModeId(READ_ONLY), "Read only")) else emptyList()

        override val defaultMode get() = SessionModeId(DEFAULT_MODE)

        override suspend fun setMode(modeId: SessionModeId, _meta: JsonElement?): SetSessionModeResponse {
            mode = modeId.value
            currentCoroutineContext().client.notify(SessionUpdate.CurrentModeUpdate(modeId))
            return SetSessionModeResponse()
        }

        private val isReadOnly get() = sandbox == READ_ONLY || mode == READ_ONLY

        override val configOptions
            get() = listOfNotNull(
                SessionConfigOption.select(
                    id = MODEL,
                    name = "Model",
                    currentValue = model,
                    options = SessionConfigSelectOptions.Flat(
                        listOf(
                            SessionConfigSelectOption(SessionConfigValueId("sonnet"), "Sonnet"),
                            SessionConfigSelectOption(SessionConfigValueId("opus"), "Opus"),
                        ),
                    ),
                ),
                SessionConfigOption.select(
                    id = SANDBOX,
                    name = "Sandbox",
                    currentValue = sandbox,
                    options = SessionConfigSelectOptions.Flat(
                        listOf(
                            SessionConfigSelectOption(SessionConfigValueId(AUTO), "Auto"),
                            SessionConfigSelectOption(SessionConfigValueId(READ_ONLY), "Read only"),
                        ),
                    ),
                    category = SessionConfigOptionCategory.MODE,
                ).takeIf { readOnlyMode == ReadOnlyOffer.CONFIG_OPTION },
            )

        override suspend fun setConfigOption(configId: SessionConfigId, value: SessionConfigOptionValue, _meta: JsonElement?): SetSessionConfigOptionResponse {
            val text = (value as SessionConfigOptionValue.StringValue).value
            if (configId.value == SANDBOX) sandbox = text else model = text
            return SetSessionConfigOptionResponse(configOptions)
        }

        override suspend fun close(_meta: JsonElement?): CloseSessionResponse {
            sessionClose?.invoke()
            return CloseSessionResponse()
        }

        override suspend fun postInitialize() {
            currentCoroutineContext().client.notify(SessionUpdate.AvailableCommandsUpdate(listOf(AvailableCommand(COMPACT, "Compact the conversation"))))
        }

        override suspend fun replayHistory() {
            if (isLoaded) currentCoroutineContext().client.notify(message(HISTORY))
        }

        override suspend fun prompt(content: List<ContentBlock>, _meta: JsonElement?) = flow {
            val text = (content.last() as ContentBlock.Text).text
            prompts += text
            content.dropLast(1).forEach { block ->
                when (block) {
                    is ContentBlock.Text -> leadingTexts += block.text
                    else -> leadingBlocks += block
                }
            }
            if (!isSignedIn.get()) throw JsonRpcException(JsonRpcErrorCode.AUTH_REQUIRED.code, "Authentication required")
            when (text) {
                "hello" -> {
                    emit(Event.SessionUpdateEvent(message("Hi")))
                    emit(Event.SessionUpdateEvent(message(" there")))
                }
                "edit" -> {
                    val outcome = currentCoroutineContext().client.requestPermissions(
                        SessionUpdate.ToolCallUpdate(ToolCallId("edit-1"), title = "Edit Game.kt", kind = ToolKind.EDIT),
                        listOf(
                            PermissionOption(PermissionOptionId("allow"), "Allow", PermissionOptionKind.ALLOW_ONCE),
                            PermissionOption(PermissionOptionId("reject"), "Reject", PermissionOptionKind.REJECT_ONCE),
                        ),
                    ).outcome
                    emit(Event.SessionUpdateEvent(message((outcome as? RequestPermissionOutcome.Selected)?.optionId?.value ?: "cancelled")))
                }
                "tool" -> {
                    emit(Event.SessionUpdateEvent(SessionUpdate.PlanUpdate(listOf(PlanEntry("List the Scenes", PlanEntryPriority.HIGH, PlanEntryStatus.IN_PROGRESS)))))
                    emit(Event.SessionUpdateEvent(SessionUpdate.ToolCall(ToolCallId("tool-1"), "mcp__kubriko-creator__list_scenes", status = ToolCallStatus.PENDING)))
                    emit(Event.SessionUpdateEvent(SessionUpdate.ToolCallUpdate(ToolCallId("tool-1"), status = ToolCallStatus.COMPLETED)))
                    emit(Event.SessionUpdateEvent(message("Done")))
                }
                "mode" -> emit(Event.SessionUpdateEvent(message(if (isReadOnly) READ_ONLY else "writable")))
                "wait" -> awaitCancellation()
                "crash" -> {
                    channel.writeError("Fake agent crashed.")
                    channel.crash(1, exitDelay = crashExitDelay)
                    awaitCancellation()
                }
                "later" -> {
                    val client = currentCoroutineContext().client
                    channel.agentScope.launch {
                        delay(50)
                        client.notify(message(LATER))
                    }
                }
            }
            emit(Event.PromptResponseEvent(PromptResponse(StopReason.END_TURN)))
        }
    }

    companion object {
        const val HISTORY = "history"
        const val LATER = "later"
        const val MODEL = "model"
        const val COMPACT = "compact"
        const val SANDBOX = "sandbox"
        const val READ_ONLY = "read-only"
        private const val AUTO = "auto"
        private const val DEFAULT_MODE = "default"
        const val AGENT_LOGIN = "fake-login"
        const val TERMINAL_LOGIN = "fake-terminal-login"
        private val sessionCount = AtomicInteger()

        fun message(text: String) = SessionUpdate.AgentMessageChunk(ContentBlock.Text(text))
    }
}
