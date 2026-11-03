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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Listens on a free localhost port for session processes, takes the first connection that says Hello after each
 * [expectSession] as the session, emits the messages it sends, and sends commands to it. Other connections are closed
 * without being read further, as is one that doesn't say Hello within [helloTimeoutInMilliseconds].
 *
 * A local process that connects and says Hello faster than the launched game becomes the session of that launch; a
 * token in the session protocol's next version closes it.
 */
public class SessionServer(
    private val scope: CoroutineScope,
    private val helloTimeoutInMilliseconds: Int = HELLO_TIMEOUT_IN_MILLISECONDS,
) : SessionChannel {
    private val serverSocket = ServerSocket(0, 0, InetAddress.getLoopbackAddress())
    private val _messages = MutableSharedFlow<SessionMessage>(extraBufferCapacity = 256)
    override val messages: SharedFlow<SessionMessage> = _messages.asSharedFlow()
    override val port: Int get() = serverSocket.localPort
    private val lock = Any()
    private var isExpecting = false
    private var session: BufferedWriter? = null
    private var sessionSocket: Socket? = null
    private val pendingSockets = ArrayDeque<Socket>()

    init {
        scope.launch(Dispatchers.IO) {
            while (isActive && !serverSocket.isClosed) {
                val socket = try {
                    serverSocket.accept()
                } catch (_: Exception) {
                    break
                }
                val evicted = synchronized(lock) {
                    pendingSockets.addLast(socket)
                    if (pendingSockets.size > MAXIMUM_PENDING_CONNECTIONS) pendingSockets.removeFirst() else null
                }
                evicted?.closeQuietly()
                launch { socket.readMessages() }
            }
        }
    }

    override fun expectSession() {
        val previous = synchronized(lock) {
            isExpecting = true
            session = null
            sessionSocket.also { sessionSocket = null }
        }
        previous?.closeQuietly()
    }

    override fun send(command: SessionCommand): Boolean {
        val writer = synchronized(lock) { session } ?: return false
        return try {
            synchronized(writer) {
                writer.write(SessionProtocol.encodeCommand(command))
                writer.newLine()
                writer.flush()
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    private suspend fun Socket.readMessages() = use {
        try {
            val reader = getInputStream().bufferedReader(Charsets.UTF_8)
            val hello = awaitHello(reader) ?: return@use
            soTimeout = 0
            val writer = getOutputStream().bufferedWriter(Charsets.UTF_8)
            val isSession = synchronized(lock) {
                pendingSockets.remove(this)
                isExpecting.also { isSession ->
                    if (isSession) {
                        isExpecting = false
                        session = writer
                        sessionSocket = this
                    }
                }
            }
            if (!isSession) return@use
            _messages.emit(hello)
            while (true) {
                val line = reader.readLine() ?: break
                decode(line)?.let { _messages.emit(it) }
            }
        } catch (_: IOException) {
            Unit
        } finally {
            synchronized(lock) {
                pendingSockets.remove(this)
                if (sessionSocket === this) {
                    sessionSocket = null
                    session = null
                }
            }
        }
    }

    /**
     * Reads lines until one is a Hello, dropping the ones before it, or returns null when the connection ends or the
     * deadline passes first.
     */
    private fun Socket.awaitHello(reader: BufferedReader): SessionMessage.Hello? {
        val deadline = System.nanoTime() + helloTimeoutInMilliseconds * 1_000_000L
        while (true) {
            val remainingInMilliseconds = (deadline - System.nanoTime()) / 1_000_000L
            if (remainingInMilliseconds <= 0) return null
            soTimeout = remainingInMilliseconds.toInt()
            val line = reader.readLine() ?: return null
            (decode(line) as? SessionMessage.Hello)?.let { return it }
        }
    }

    private fun decode(line: String) = try {
        SessionProtocol.decode(line)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    override fun close() {
        serverSocket.close()
        val sockets = synchronized(lock) { listOfNotNull(sessionSocket) + pendingSockets }
        sockets.forEach { it.closeQuietly() }
    }

    private fun Socket.closeQuietly() = try {
        close()
    } catch (_: IOException) {
        Unit
    }

    private companion object {
        const val HELLO_TIMEOUT_IN_MILLISECONDS = 10_000
        const val MAXIMUM_PENDING_CONNECTIONS = 4
    }
}
