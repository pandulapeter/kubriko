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
 * have to: every [AiClientSetup] on this computer, including shell aliases that run a tool with settings of their own.
 * Setups connected this way are connected again whenever the port or the token changes.
 */
internal class AiClientsController(
    private val mcp: McpController,
    private val runner: ProcessRunner,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val _setups = MutableStateFlow(AiClient.entries.map { AiClientSetup(it, mcp.settings.value.commandOf(it)) })
    val setups = _setups.asStateFlow()
    private val _statuses = MutableStateFlow(emptyMap<String, AiClientStatus>())

    /**
     * The status of each setup, by [AiClientSetup.id]; a setup without one is being checked.
     */
    val statuses = _statuses.asStateFlow()
    private val operations = Mutex()

    init {
        scope.launch {
            mcp.settings.map { it.port to it.token }.distinctUntilChanged().drop(1).collect {
                mcp.settings.value.connectedClients
                    .mapNotNull { id -> _setups.value.firstOrNull { it.id == id } ?: AiClientSetup.fromId(id) }
                    .forEach(::connect)
            }
        }
    }

    /**
     * Finds the setups of every AI tool, and checks which are installed and which already know Kubriko Creator.
     */
    fun refresh() {
        scope.launch {
            val aliases = withContext(ioDispatcher) { runner.aliases() }
            _setups.value = AiClientSetup.discover(aliases, mcp.settings.value::commandOf)
            _statuses.value = emptyMap()
            _setups.value.forEach { setup -> perform(setup) { status(setup) } }
        }
    }

    /**
     * Registers Kubriko Creator in [setup], turning the server on first if it is off.
     */
    fun connect(setup: AiClientSetup) = perform(setup) {
        if (!mcp.settings.value.isEnabled) {
            mcp.setEnabled(true)
            mcp.state.first { it is McpServerState.Running || it is McpServerState.Failed }
        }
        val result = withContext(ioDispatcher) {
            if (!runner.exists(setup.command)) return@withContext null
            runner.run(setup.command, setup.client.removeArguments)
            runner.run(setup.command, setup.client.addArguments(mcp.settings.value))
        }
        when {
            result == null -> AiClientStatus.NotInstalled
            result.exitCode != 0 -> AiClientStatus.Failed(result.output)
            status(setup) == AiClientStatus.Connected -> AiClientStatus.Connected.also { mcp.setClientConnected(setup.id, true) }
            else -> AiClientStatus.Failed(result.output)
        }
    }

    fun disconnect(setup: AiClientSetup) = perform(setup) {
        val result = withContext(ioDispatcher) { runner.run(setup.command, setup.client.removeArguments) }
        mcp.setClientConnected(setup.id, false)
        status(setup).let { status -> if (status == AiClientStatus.Connected) AiClientStatus.Failed(result.output) else status }
    }

    /**
     * Uses [command] to run [client] from now on, and looks for its setups again.
     */
    fun setCommand(client: AiClient, command: String) {
        mcp.setClientCommand(client, command)
        refresh()
    }

    private suspend fun status(setup: AiClientSetup) = withContext(ioDispatcher) {
        when {
            !runner.exists(setup.command) -> AiClientStatus.NotInstalled
            runner.run(setup.command, setup.client.getArguments).let { setup.client.isConnected(it.exitCode, it.output) } -> AiClientStatus.Connected
            else -> AiClientStatus.NotConnected
        }
    }

    private fun perform(setup: AiClientSetup, operation: suspend () -> AiClientStatus) {
        scope.launch {
            operations.withLock {
                _statuses.update { it + (setup.id to AiClientStatus.Checking) }
                val status = operation()
                _statuses.update { it + (setup.id to status) }
            }
        }
    }
}
