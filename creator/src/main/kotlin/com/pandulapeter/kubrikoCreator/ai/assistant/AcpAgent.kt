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
import com.agentclientprotocol.client.Client
import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.client.ClientSession
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.AcpMethod
import com.agentclientprotocol.model.AcpNotification
import com.agentclientprotocol.model.AuthMethodId
import com.agentclientprotocol.model.ClientCapabilities
import com.agentclientprotocol.model.Implementation
import com.agentclientprotocol.model.McpServer
import com.agentclientprotocol.model.PROTOCOL_VERSION_V1
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * An ACP agent Kubriko Creator talks to: one process, started on first use, hosting many conversations. A process that
 * ends unasked is started again with its conversations loaded again (lost where the agent can't load them), unless it
 * keeps crashing right after starting; it is then [AgentState.Failed] until it is used again.
 *
 * Creator advertises neither file system nor terminal access, since its own tools make every change.
 *
 * @param scope Where the agent's connection runs; cancelling it ends the agent.
 */
internal class AcpAgent(
    private val command: AgentCommand,
    private val launcher: AgentLauncher,
    private val clientInfo: ClientInfo,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : AutoCloseable {
    private val _state = MutableStateFlow<AgentState>(AgentState.Stopped)
    val state = _state.asStateFlow()
    private val _errorOutput = MutableStateFlow(emptyList<String>())

    /**
     * The last lines the current (or last) process wrote to its standard error.
     */
    val errorOutput = _errorOutput.asStateFlow()
    private val _authStatus = MutableStateFlow<AuthStatus?>(null)

    /**
     * Who the agent works as, where it reports that (Claude Agent does), or null while it hasn't.
     */
    val authStatus = _authStatus.asStateFlow()
    private val _signInMethods = MutableStateFlow(emptyList<SignInMethod>())

    /**
     * The ways of signing in the agent offered when it last started.
     */
    val signInMethods = _signInMethods.asStateFlow()
    private val lifecycle = Mutex()

    @Volatile
    private var connection: Connection? = null
    private val conversations = mutableListOf<AgentConversation>()
    private var crashesInARow = 0

    @Volatile
    private var isClosed = false

    private class Connection(
        val channel: AgentChannel,
        val scope: CoroutineScope,
        val client: Client,
        val info: AgentInfo,
        val errorReader: Job,
        val startedAt: TimeMark,
    )

    /**
     * Starts the agent unless it runs already.
     *
     * @throws AgentStartException If it couldn't be started.
     */
    suspend fun start(): AgentInfo = lifecycle.withLock { connected().info }

    /**
     * Starts a conversation in [workingDirectory], with [mcpServers] as the agent's tools.
     *
     * @param meta The `_meta` of `session/new`, through which agents take options of their own.
     * @throws AgentStartException If the agent couldn't be started.
     */
    suspend fun newConversation(
        workingDirectory: File,
        mcpServers: List<McpServer> = emptyList(),
        meta: JsonElement? = null,
        permissions: PermissionHandler = PermissionHandler.Reject,
    ): AgentConversation = lifecycle.withLock {
        val parameters = SessionCreationParameters(workingDirectory.absolutePath, mcpServers, _meta = meta)
        lateinit var conversation: AgentConversation
        val session = connected().client.newSession(parameters) { sessionId, _ ->
            conversation = AgentConversation(sessionId, parameters, this, permissions)
            conversation.operations
        }
        conversation.attach(session)
        conversations += conversation
        conversation
    }

    /**
     * Signs in with [method], one of [AgentInfo.authMethods] that the agent runs by itself.
     *
     * @throws AgentStartException If the agent couldn't be started.
     */
    suspend fun authenticate(method: AuthMethodId) {
        val client = lifecycle.withLock { connected().client }
        client.authenticate(method)
    }

    internal suspend fun sessionOf(conversation: AgentConversation): ClientSession = lifecycle.withLock {
        if (conversation !in conversations) throw ConversationLostException()
        connected()
        conversation.currentSession() ?: throw ConversationLostException()
    }

    internal suspend fun close(conversation: AgentConversation) {
        val session = lifecycle.withLock {
            if (!conversations.remove(conversation)) return
            conversation.currentSession().takeIf { connection?.info?.capabilities?.sessionCapabilities?.close != null }
        }
        conversation.markLost()
        try {
            session?.close()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
        }
    }

    private suspend fun connected(): Connection {
        check(!isClosed) { "The agent was closed." }
        connection?.let { return it }
        if (_state.value is AgentState.Failed) crashesInARow = 0
        val connection = connect()
        if (isClosed) {
            shutDown(connection.scope, connection.channel)
            error("The agent was closed.")
        }
        this.connection = connection
        scope.launch {
            val exitCode = withContext(ioDispatcher) { connection.channel.awaitExit() }
            onExit(connection, exitCode)
        }
        reload(connection)
        _state.value = AgentState.Running(connection.info)
        return connection
    }

    private suspend fun connect(): Connection {
        _state.value = AgentState.Starting
        _errorOutput.value = emptyList()
        val channel = try {
            withContext(ioDispatcher) { launcher.launch(command) }
        } catch (exception: IOException) {
            _state.value = AgentState.Failed(null, emptyList())
            throw AgentStartException(null, emptyList(), exception)
        }
        val connectionScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        val errorReader = connectionScope.launch(ioDispatcher) {
            channel.errors.bufferedReader().forEachLine { line -> _errorOutput.update { (it + line).takeLast(ERROR_OUTPUT_SIZE) } }
        }
        val reader = channel.fromAgent.bufferedReader()
        val writer = channel.toAgent.bufferedWriter()
        val transport = StdioTransport(
            parentScope = connectionScope,
            ioDispatcher = ioDispatcher,
            input = flow { while (true) emit(reader.readLine() ?: break) },
            output = { line ->
                writer.write(line)
                writer.newLine()
                writer.flush()
            },
            name = command.program,
        )
        val protocol = Protocol(connectionScope, transport)
        protocol.setNotificationHandlerRaw(authStatusUpdate, connectionScope.coroutineContext) { notification ->
            AuthStatus.parse(notification.params)?.let { _authStatus.value = it }
        }
        val client = Client(protocol)
        protocol.start()
        val exit = connectionScope.async(ioDispatcher) { channel.awaitExit() }
        val initializing = connectionScope.async { client.initialize(clientInfo) }
        val info = try {
            select<AgentInfo?> {
                initializing.onAwait { it }
                exit.onAwait { null }
            }
        } catch (exception: CancellationException) {
            // The SDK fails a request with a CancellationException of its own when the process closes its streams.
            if (currentCoroutineContext().isActive) {
                null
            } else {
                withContext(NonCancellable) { shutDown(connectionScope, channel) }
                throw exception
            }
        } catch (exception: Exception) {
            null
        }
        if (info == null) {
            val exitCode = withTimeoutOrNull(EXIT_WAIT) { exit.await() }
            withTimeoutOrNull(EXIT_WAIT) { errorReader.join() }
            shutDown(connectionScope, channel)
            val errorOutput = _errorOutput.value
            _state.value = AgentState.Failed(exitCode, errorOutput)
            throw AgentStartException(exitCode, errorOutput, initializing.failure())
        }
        exit.cancel()
        _signInMethods.value = SignInMethod.of(info.authMethods, command)
        return Connection(channel, connectionScope, client, info, errorReader, timeSource.markNow())
    }

    /**
     * Loads every conversation of an earlier process into [connection], and gives up on those it can't load.
     */
    private suspend fun reload(connection: Connection) {
        for (conversation in conversations) {
            if (conversation.currentSession()?.client === connection.client) continue
            if (!connection.info.capabilities.loadSession) {
                conversation.markLost()
                continue
            }
            try {
                conversation.attach(conversation.replaying { connection.client.loadSession(conversation.id, conversation.parameters, conversation.operations) })
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                conversation.markLost()
            }
        }
        conversations.removeAll { it.isLost.value }
    }

    private suspend fun onExit(connection: Connection, exitCode: Int) = lifecycle.withLock {
        if (this.connection !== connection) return@withLock
        this.connection = null
        withTimeoutOrNull(EXIT_WAIT) { connection.errorReader.join() }
        shutDown(connection.scope, connection.channel)
        if (isClosed) return@withLock
        crashesInARow = if (connection.startedAt.elapsedNow() < STABLE_UPTIME) crashesInARow + 1 else 1
        when {
            crashesInARow > MAXIMUM_RESTARTS -> _state.value = AgentState.Failed(exitCode, _errorOutput.value)
            conversations.isEmpty() -> _state.value = AgentState.Stopped
            else -> try {
                connected()
            } catch (_: AgentStartException) {
            }
        }
    }

    private suspend fun shutDown(connectionScope: CoroutineScope, channel: AgentChannel) {
        connectionScope.cancel()
        withContext(ioDispatcher + NonCancellable) { channel.stop() }
    }

    /**
     * Ends the agent's process. Its conversations can't be continued.
     */
    override fun close() {
        isClosed = true
        connection?.let {
            it.scope.cancel()
            it.channel.stop()
        }
        connection = null
        _state.value = AgentState.Stopped
    }

    companion object {
        private const val ERROR_OUTPUT_SIZE = 50
        private const val MAXIMUM_RESTARTS = 3
        private val STABLE_UPTIME = 30.seconds
        private val EXIT_WAIT = 2.seconds

        /**
         * What Kubriko Creator, at [version], tells agents about itself: protocol version 1, and terminal sign-in.
         */
        fun clientInfo(version: String) = ClientInfo(
            protocolVersion = PROTOCOL_VERSION_V1,
            capabilities = ClientCapabilities(_meta = buildJsonObject { put(TERMINAL_AUTH, JsonPrimitive(true)) }),
            implementation = Implementation(name = "kubriko-creator", version = version, title = "Kubriko Creator"),
            supportedProtocolVersions = setOf(PROTOCOL_VERSION_V1),
        )

        private const val TERMINAL_AUTH = "terminal-auth"
        private val authStatusUpdate = AcpMethod.AcpNotificationMethod(AuthStatus.METHOD, AuthStatusNotification.serializer())
    }
}

/**
 * The shape the SDK needs for an extension notification; [AuthStatus.parse] reads its parameters.
 */
@Serializable
private class AuthStatusNotification(
    @Suppress("PropertyName") override val _meta: JsonElement? = null,
) : AcpNotification

@OptIn(ExperimentalCoroutinesApi::class)
private fun Deferred<*>.failure() = if (isCompleted) getCompletionExceptionOrNull() else null
