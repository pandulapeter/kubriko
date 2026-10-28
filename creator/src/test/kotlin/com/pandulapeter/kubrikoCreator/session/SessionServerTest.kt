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
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun messagesArriveWhileTheSessionKeepsItsConnectionOpen() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        SessionServer(scope).use { server ->
            val hello = SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY)
            val received = async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) { server.messages.first() }
            }
            Socket(InetAddress.getLoopbackAddress(), server.port).use { socket ->
                socket.getOutputStream().bufferedWriter().apply {
                    write("${SessionProtocol.encode(hello)}\n")
                    flush()
                }
                assertEquals(hello, received.await())
            }
        }
        scope.cancel()
    }

    @Test
    fun commandsGoToTheSessionThatConnectedLast() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        SessionServer(scope).use { server ->
            assertFalse(server.send(SessionCommand.BringToFront(1)))
            val hello = SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY)
            val received = async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) { server.messages.first() }
            }
            Socket(InetAddress.getLoopbackAddress(), server.port).use { socket ->
                socket.getOutputStream().bufferedWriter().apply {
                    write("${SessionProtocol.encode(hello)}\n")
                    flush()
                }
                received.await()
                val command = SessionCommand.PressKeys(2, listOf("LEFT"), 100)
                assertTrue(server.send(command))
                assertEquals(command, SessionProtocol.decodeCommand(socket.getInputStream().bufferedReader().readLine()))
            }
        }
        scope.cancel()
    }

    @Test
    fun theServerListensOnLocalhostUntilClosed() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val server = SessionServer(scope)
        assertTrue(server.port > 0)
        Socket(InetAddress.getLoopbackAddress(), server.port).close()
        server.close()
        // Connecting to the freed port proves nothing: another process, or the client itself, may be bound to it by then.
        withTimeout(10_000) { scope.coroutineContext.job.children.toList().joinAll() }
        scope.cancel()
    }
}
