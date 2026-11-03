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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The AI connection: its settings, and the MCP server they start and stop. Settings changes restart the server.
 *
 * @param isAiEnabled Whether AI features are on: while they are off, the server is stopped whatever the settings say,
 *   and they are kept for when AI features are turned on again.
 */
public class McpController(
    private val server: McpToolServer,
    private val store: McpSettingsStore,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
) : AutoCloseable {
    private val _settings = MutableStateFlow(store.load())
    /**
     * The MCP server's settings, as saved.
     */
    public val settings: StateFlow<McpSettings> = _settings.asStateFlow()
    private val _state = MutableStateFlow<McpServerState>(McpServerState.Off)
    /**
     * Whether the server is off, starting, running, or failed.
     */
    public val state: StateFlow<McpServerState> = _state.asStateFlow()
    /**
     * The tool calls made since Kubriko Creator started, oldest first.
     */
    public val calls: StateFlow<List<McpToolCall>> = server.calls
    private var job: Job? = null
    private val saves = Mutex()
    private val restarts = Mutex()

    init {
        apply(_settings.value)
        scope.launch { isAiEnabled.drop(1).collect { apply(_settings.value) } }
    }

    /**
     * Turns the server on or off, and saves it.
     */
    public fun setEnabled(isEnabled: Boolean): Unit = update(_settings.value.copy(isEnabled = isEnabled))

    /**
     * Turns the server on, returning once it has started or failed to, or stayed off because AI features are off.
     */
    internal suspend fun enable(): McpServerState {
        if (!_settings.value.isEnabled) setEnabled(true)
        job?.join()
        return _state.value
    }

    /**
     * @return Whether [port] can be used.
     */
    public fun setPort(port: Int): Boolean {
        if (port !in McpSettings.portRange) return false
        if (port != _settings.value.port) update(_settings.value.copy(port = port))
        return true
    }

    /**
     * Remembers whether the AI tool setup with [setupId] is connected.
     */
    internal fun setClientConnected(setupId: String, isConnected: Boolean) = updateStored(
        _settings.value.copy(connectedClients = if (isConnected) _settings.value.connectedClients + setupId else _settings.value.connectedClients - setupId),
    )

    /**
     * Runs [command] for [client] from now on, or its usual program if it is blank.
     */
    internal fun setClientCommand(client: AiClient, command: String) = updateStored(
        _settings.value.copy(clientCommands = if (command.isBlank()) _settings.value.clientCommands - client.id else _settings.value.clientCommands + (client.id to command.trim())),
    )

    /**
     * Replaces the token, so that clients set up with the old one can no longer connect.
     */
    public fun regenerateToken(): Unit = update(_settings.value.copy(token = store.newToken()))

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
            // The server isn't safe to start and stop from two places at once. A lock rather than joining the change before
            // keeps every earlier change out, since the one before may have been cancelled while an older one still runs.
            restarts.withLock {
                withContext(ioDispatcher) { server.stop() }
                if (!settings.isEnabled || !isAiEnabled.value) {
                    _state.value = McpServerState.Off
                    return@launch
                }
                _state.value = McpServerState.Starting
                _state.value = try {
                    // A start cancelled half-way can leave the engine binding after it was stopped, so it runs to its end and the
                    // next change, which waits for this one, stops it.
                    McpServerState.Running(withContext(ioDispatcher + NonCancellable) { server.start(settings.port, settings.token) }.also { ensureActive() })
                } catch (_: IOException) {
                    McpServerState.Failed(settings.port)
                }
            }
        }
    }

    override fun close() {
        job?.cancel()
        server.stop()
    }
}
