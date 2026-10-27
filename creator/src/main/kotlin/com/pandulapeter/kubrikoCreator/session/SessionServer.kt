/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Listens on a free localhost port for session processes, and emits every message they send.
 */
internal class SessionServer(
    private val scope: CoroutineScope,
) : SessionChannel {
    private val serverSocket = ServerSocket(0, 0, InetAddress.getLoopbackAddress())
    private val _messages = MutableSharedFlow<SessionMessage>(extraBufferCapacity = 256)
    override val messages = _messages.asSharedFlow()
    override val port: Int get() = serverSocket.localPort

    init {
        scope.launch(Dispatchers.IO) {
            while (isActive && !serverSocket.isClosed) {
                val socket = try {
                    serverSocket.accept()
                } catch (_: Exception) {
                    break
                }
                launch { socket.readMessages() }
            }
        }
    }

    private suspend fun Socket.readMessages() = use {
        val reader = getInputStream().bufferedReader(Charsets.UTF_8)
        while (true) {
            val line = try {
                reader.readLine()
            } catch (_: IOException) {
                null
            } ?: break
            decode(line)?.let { _messages.emit(it) }
        }
    }

    private fun decode(line: String) = try {
        SessionProtocol.decode(line)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    override fun close() = serverSocket.close()
}
