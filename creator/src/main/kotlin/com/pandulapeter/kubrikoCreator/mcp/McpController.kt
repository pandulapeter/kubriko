/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The AI connection: its settings, and the MCP server they start and stop. Settings changes restart the server.
 */
internal class McpController(
    private val server: McpToolServer,
    private val store: McpSettingsStore,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : AutoCloseable {
    private val _settings = MutableStateFlow(store.load())
    val settings = _settings.asStateFlow()
    private val _state = MutableStateFlow<McpServerState>(McpServerState.Off)
    val state = _state.asStateFlow()
    val calls = server.calls
    private var job: Job? = null
    private val saves = Mutex()

    init {
        apply(_settings.value)
    }

    fun setEnabled(isEnabled: Boolean) = update(_settings.value.copy(isEnabled = isEnabled))

    /**
     * @return Whether [port] can be used.
     */
    fun setPort(port: Int): Boolean {
        if (port !in McpSettings.portRange) return false
        if (port != _settings.value.port) update(_settings.value.copy(port = port))
        return true
    }

    /**
     * Remembers whether the AI tool setup with [setupId] is connected.
     */
    fun setClientConnected(setupId: String, isConnected: Boolean) = updateStored(
        _settings.value.copy(connectedClients = if (isConnected) _settings.value.connectedClients + setupId else _settings.value.connectedClients - setupId),
    )

    /**
     * Runs [command] for [client] from now on, or its usual program if it is blank.
     */
    fun setClientCommand(client: AiClient, command: String) = updateStored(
        _settings.value.copy(clientCommands = if (command.isBlank()) _settings.value.clientCommands - client else _settings.value.clientCommands + (client to command.trim())),
    )

    /**
     * Replaces the token, so that clients set up with the old one can no longer connect.
     */
    fun regenerateToken() = update(_settings.value.copy(token = store.newToken()))

    private fun update(settings: McpSettings) {
        updateStored(settings)
        apply(settings)
    }

    /**
     * Keeps settings that don't change how the server runs.
     */
    private fun updateStored(settings: McpSettings) {
        _settings.value = settings
        // Saves can finish in any order, so each one writes the settings as they are by then.
        scope.launch(ioDispatcher) { saves.withLock { store.save(_settings.value) } }
    }

    private fun apply(settings: McpSettings) {
        job?.cancel()
        job = scope.launch {
            withContext(ioDispatcher) { server.stop() }
            if (!settings.isEnabled) {
                _state.value = McpServerState.Off
                return@launch
            }
            _state.value = McpServerState.Starting
            _state.value = try {
                McpServerState.Running(withContext(ioDispatcher) { server.start(settings.port, settings.token) })
            } catch (_: IOException) {
                McpServerState.Failed(settings.port)
            }
        }
    }

    override fun close() {
        job?.cancel()
        server.stop()
    }
}
