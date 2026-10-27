/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionConnectionTest {

    @Test
    fun messagesArriveAsOneEncodedMessagePerLine() = ServerSocket(0, 0, InetAddress.getLoopbackAddress()).use { server ->
        val messages = listOf(
            SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY),
            SessionMessage.Failure(message = "Multi\nline"),
        )
        SessionConnection.open(server.localPort).use { connection ->
            server.accept().use { socket ->
                messages.forEach(connection::send)
                connection.close()
                val lines = socket.getInputStream().bufferedReader().readLines()
                assertEquals(messages, lines.map(SessionProtocol::decode))
            }
        }
    }

    @Test
    fun withoutAPortMessagesAreDropped() = SessionConnection.open(null).use { connection ->
        connection.send(SessionMessage.Failure(message = "Nobody is listening."))
    }

    @Test
    fun aLostConnectionDropsMessagesInsteadOfThrowing() {
        val server = ServerSocket(0, 0, InetAddress.getLoopbackAddress())
        SessionConnection.open(server.localPort).use { connection ->
            server.accept().close()
            server.close()
            repeat(100) { connection.send(SessionMessage.Failure(message = "Still running.")) }
        }
    }
}
