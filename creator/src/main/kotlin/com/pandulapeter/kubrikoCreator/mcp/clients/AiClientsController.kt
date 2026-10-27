/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Connects AI tools to Kubriko Creator's MCP server with their own command-line programs, so that the user doesn't
 * have to. Clients connected this way are connected again whenever the port or the token changes.
 */
internal class AiClientsController(
    private val mcp: McpController,
    private val runner: ProcessRunner,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val _statuses = MutableStateFlow(AiClient.entries.associateWith<AiClient, AiClientStatus> { AiClientStatus.Checking })
    val statuses = _statuses.asStateFlow()
    private val operations = Mutex()

    init {
        scope.launch {
            mcp.settings.map { it.port to it.token }.distinctUntilChanged().drop(1).collect {
                mcp.settings.value.connectedClients.forEach { client -> connect(client) }
            }
        }
    }

    /**
     * Checks which AI tools are installed, and which of them already know Kubriko Creator.
     */
    fun refresh() = AiClient.entries.forEach { client ->
        perform(client) { status(client) }
    }

    /**
     * Registers Kubriko Creator in [client], turning the server on first if it is off.
     */
    fun connect(client: AiClient) = perform(client) {
        if (!mcp.settings.value.isEnabled) {
            mcp.setEnabled(true)
            mcp.state.first { it is McpServerState.Running || it is McpServerState.Failed }
        }
        val command = mcp.settings.value.commandOf(client)
        val result = withContext(ioDispatcher) {
            if (!runner.exists(command)) return@withContext null
            runner.run(command, client.removeArguments)
            runner.run(command, client.addArguments(mcp.settings.value))
        }
        when {
            result == null -> AiClientStatus.NotInstalled
            result.exitCode != 0 -> AiClientStatus.Failed(result.output)
            status(client) == AiClientStatus.Connected -> AiClientStatus.Connected.also { mcp.setClientConnected(client, true) }
            else -> AiClientStatus.Failed(result.output)
        }
    }

    fun disconnect(client: AiClient) = perform(client) {
        val result = withContext(ioDispatcher) { runner.run(mcp.settings.value.commandOf(client), client.removeArguments) }
        mcp.setClientConnected(client, false)
        status(client).let { status -> if (status == AiClientStatus.Connected) AiClientStatus.Failed(result.output) else status }
    }

    /**
     * Uses [command] to run [client] from now on, and checks it again.
     */
    fun setCommand(client: AiClient, command: String) {
        mcp.setClientCommand(client, command)
        perform(client) { status(client) }
    }

    private suspend fun status(client: AiClient) = withContext(ioDispatcher) {
        val command = mcp.settings.value.commandOf(client)
        when {
            !runner.exists(command) -> AiClientStatus.NotInstalled
            runner.run(command, client.getArguments).let { client.isConnected(it.exitCode, it.output) } -> AiClientStatus.Connected
            else -> AiClientStatus.NotConnected
        }
    }

    private fun perform(client: AiClient, operation: suspend () -> AiClientStatus) {
        scope.launch {
            operations.withLock {
                _statuses.update { it + (client to AiClientStatus.Checking) }
                val status = operation()
                _statuses.update { it + (client to status) }
            }
        }
    }
}
