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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
     * Replaces the token, so that clients set up with the old one can no longer connect.
     */
    fun regenerateToken() = update(_settings.value.copy(token = store.newToken()))

    private fun update(settings: McpSettings) {
        _settings.value = settings
        scope.launch(ioDispatcher) { store.save(settings) }
        apply(settings)
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
