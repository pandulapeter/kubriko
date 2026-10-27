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
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionServerTest {

    @Test
    fun messagesOfEveryConnectedSessionAreEmittedAndGarbageIsSkipped() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        SessionServer(scope).use { server ->
            val hello = SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.EDIT)
            val failure = SessionMessage.Failure(message = "Second session")
            val received = async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) { server.messages.take(2).toList() }
            }
            Socket(InetAddress.getLoopbackAddress(), server.port).use { socket ->
                socket.getOutputStream().bufferedWriter().apply {
                    write("not json\n{\"type\":\"unknown\"}\n${SessionProtocol.encode(hello)}\n")
                    flush()
                }
            }
            Socket(InetAddress.getLoopbackAddress(), server.port).use { socket ->
                socket.getOutputStream().bufferedWriter().apply {
                    write("${SessionProtocol.encode(failure)}\n")
                    flush()
                }
            }
            assertEquals(setOf(hello, failure), received.await().toSet())
        }
        scope.cancel()
    }

    @Test
    fun theServerListensOnLocalhostUntilClosed() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val server = SessionServer(scope)
        assertTrue(server.port > 0)
        Socket(InetAddress.getLoopbackAddress(), server.port).close()
        server.close()
        assertTrue(runCatching { Socket(InetAddress.getLoopbackAddress(), server.port).close() }.isFailure)
        scope.cancel()
    }
}
