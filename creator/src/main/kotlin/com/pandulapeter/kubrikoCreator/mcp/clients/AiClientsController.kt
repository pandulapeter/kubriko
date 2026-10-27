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
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
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
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException

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
    private val configFileOf: (AiConfigFileClient) -> File = { it.configFile() },
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
                mcp.settings.value.connectedClients.forEach { id ->
                    AiConfigFileClient.entries.firstOrNull { it.id == id }?.let(::connectConfigFile)
                        ?: (_setups.value.firstOrNull { it.id == id } ?: AiClientSetup.fromId(id))?.let(::connect)
                }
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
            _setups.value.forEach { setup -> perform(setup.id) { status(setup) } }
            AiConfigFileClient.entries.forEach { client -> perform(client.id) { status(client) } }
        }
    }

    /**
     * Registers Kubriko Creator in [setup], turning the server on first if it is off.
     */
    fun connect(setup: AiClientSetup) = perform(setup.id) {
        enableServer()
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

    fun disconnect(setup: AiClientSetup) = perform(setup.id) {
        val result = withContext(ioDispatcher) { runner.run(setup.command, setup.client.removeArguments) }
        mcp.setClientConnected(setup.id, false)
        status(setup).let { status -> if (status == AiClientStatus.Connected) AiClientStatus.Failed(result.output) else status }
    }

    /**
     * Adds Kubriko Creator's server to [client]'s configuration file, turning the server on first if it is off.
     */
    fun connectConfigFile(client: AiConfigFileClient) = perform(client.id) {
        when (val status = status(client)) {
            AiClientStatus.NotInstalled, is AiClientStatus.NeedsProgram -> status
            else -> {
                enableServer()
                val settings = mcp.settings.value
                editConfigFile(client) { file ->
                    val programPath = client.requiredProgram?.let { runner.locate(it.command) }
                    McpConfigFile.addServer(file, McpToolServer.SERVER_NAME, client.entry(settings, programPath))
                }.also { if (it == AiClientStatus.Connected) mcp.setClientConnected(client.id, true) }
            }
        }
    }

    fun disconnectConfigFile(client: AiConfigFileClient) = perform(client.id) {
        editConfigFile(client) { file -> McpConfigFile.removeServer(file, McpToolServer.SERVER_NAME) }
            .also { mcp.setClientConnected(client.id, false) }
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

    private suspend fun status(client: AiConfigFileClient) = withContext(ioDispatcher) {
        val file = configFileOf(client)
        val program = client.requiredProgram
        when {
            file.parentFile?.isDirectory != true -> AiClientStatus.NotInstalled
            program != null && !runner.exists(program.command) -> AiClientStatus.NeedsProgram(program)
            else -> try {
                if (McpConfigFile.server(file, McpToolServer.SERVER_NAME) != null) AiClientStatus.Connected else AiClientStatus.NotConnected
            } catch (exception: SerializationException) {
                AiClientStatus.Failed(exception.message.orEmpty())
            }
        }
    }

    /**
     * Does [edit] to [client]'s configuration file, then reports its status, or what went wrong. A file that isn't valid
     * JSON is never written.
     */
    private suspend fun editConfigFile(client: AiConfigFileClient, edit: (File) -> Unit) = try {
        withContext(ioDispatcher) { edit(configFileOf(client)) }
        status(client)
    } catch (exception: SerializationException) {
        AiClientStatus.Failed(exception.message.orEmpty())
    } catch (exception: IOException) {
        AiClientStatus.Failed(exception.message.orEmpty())
    }

    private suspend fun enableServer() {
        if (!mcp.settings.value.isEnabled) {
            mcp.setEnabled(true)
            mcp.state.first { it is McpServerState.Running || it is McpServerState.Failed }
        }
    }

    private fun perform(id: String, operation: suspend () -> AiClientStatus) {
        scope.launch {
            operations.withLock {
                _statuses.update { it + (id to AiClientStatus.Checking) }
                val status = operation()
                _statuses.update { it + (id to status) }
            }
        }
    }
}
