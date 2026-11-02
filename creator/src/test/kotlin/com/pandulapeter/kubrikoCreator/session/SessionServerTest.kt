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
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionServerTest {

    private val hello = SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY)

    @Test
    fun onlyTheExpectedSessionsLinesAreEmittedAndGarbageBeforeItsHelloIsSkipped() = withServer(helloTimeoutInMilliseconds = 1_000) { server ->
        val editHello = SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.EDIT)
        val log = SessionMessage.Log(id = "1", message = "Started", details = null, source = "Game", timestamp = 0L, importance = "high")
        val failure = SessionMessage.Failure(message = "Second session")
        server.expectSession()
        val received = receive(server, 2)
        server.connect().use { session ->
            session.writeLines("not json", "{\"type\":\"unknown\"}", SessionProtocol.encode(editHello), SessionProtocol.encode(log))
            server.connect().use { other ->
                other.writeLines(SessionProtocol.encode(failure))
                assertEquals(listOf(editHello, log), received.await())
                assertTrue(other.isClosedByServer())
            }
        }
    }

    @Test
    fun aConnectionBeforeAnyLaunchIsClosedWithoutEmitting() = withServer { server ->
        val received = async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(500) { server.messages.first() }
        }
        server.connect().use { socket ->
            socket.writeLines(SessionProtocol.encode(hello))
            assertTrue(socket.isClosedByServer())
        }
        assertNull(received.await())
        assertFalse(server.send(SessionCommand.BringToFront(1)))
    }

    @Test
    fun messagesArriveWhileTheSessionKeepsItsConnectionOpen() = withServer { server ->
        server.expectSession()
        val received = receive(server, 1)
        server.connect().use { socket ->
            socket.writeLines(SessionProtocol.encode(hello))
            assertEquals(listOf(hello), received.await())
        }
    }

    @Test
    fun commandsGoOnlyToTheSession() = withServer { server ->
        assertFalse(server.send(SessionCommand.BringToFront(1)))
        server.expectSession()
        val received = receive(server, 1)
        server.connect().use { session ->
            session.writeLines(SessionProtocol.encode(hello))
            received.await()
            server.connect().use { other ->
                other.writeLines(SessionProtocol.encode(hello))
                assertTrue(other.isClosedByServer())
            }
            val command = SessionCommand.PressKeys(2, listOf("LEFT"), 100)
            assertTrue(server.send(command))
            assertEquals(command, session.readCommand())
        }
    }

    @Test
    fun aConnectionThatNeverSaysHelloIsClosed() = withServer(helloTimeoutInMilliseconds = 200) { server ->
        server.connect().use { idle ->
            assertTrue(idle.isClosedByServer())
        }
        server.expectSession()
        val received = receive(server, 1)
        server.connect().use { session ->
            session.writeLines(SessionProtocol.encode(hello))
            assertEquals(listOf(hello), received.await())
        }
    }

    @Test
    fun anIdleConnectionMakesRoomForANewOne() = withServer { server ->
        server.expectSession()
        val received = receive(server, 1)
        val idle = List(5) { server.connect() }
        try {
            server.connect().use { session ->
                session.writeLines(SessionProtocol.encode(hello))
                assertEquals(listOf(hello), received.await())
                assertTrue(idle[0].isClosedByServer())
                assertTrue(idle[1].isClosedByServer())
                val command = SessionCommand.BringToFront(3)
                assertTrue(server.send(command))
                assertEquals(command, session.readCommand())
            }
        } finally {
            idle.forEach { it.close() }
        }
    }

    @Test
    fun theNextLaunchReplacesTheSessionOfTheLastOne() = withServer { server ->
        server.expectSession()
        val firstHello = receive(server, 1)
        server.connect().use { first ->
            first.writeLines(SessionProtocol.encode(hello))
            assertEquals(listOf(hello), firstHello.await())
            server.expectSession()
            assertTrue(first.isClosedByServer())
            val secondHello = receive(server, 1)
            server.connect().use { second ->
                second.writeLines(SessionProtocol.encode(hello))
                assertEquals(listOf(hello), secondHello.await())
                val command = SessionCommand.BringToFront(4)
                assertTrue(server.send(command))
                assertEquals(command, second.readCommand())
            }
        }
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

    private fun withServer(
        helloTimeoutInMilliseconds: Int = 10_000,
        block: suspend CoroutineScope.(SessionServer) -> Unit,
    ) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            SessionServer(scope, helloTimeoutInMilliseconds).use { server -> withTimeout(10_000) { block(server) } }
        } finally {
            scope.cancel()
        }
    }

    private fun CoroutineScope.receive(server: SessionServer, count: Int) = async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        withTimeout(10_000) { server.messages.take(count).toList() }
    }

    private fun SessionServer.connect() = Socket(InetAddress.getLoopbackAddress(), port).apply { soTimeout = 10_000 }

    private fun Socket.writeLines(vararg lines: String) = getOutputStream().bufferedWriter().apply {
        lines.forEach { write("$it\n") }
        flush()
    }

    private fun Socket.readCommand() = SessionProtocol.decodeCommand(getInputStream().bufferedReader().readLine())

    private fun Socket.isClosedByServer() = try {
        getInputStream().read() == -1
    } catch (_: SocketException) {
        true
    }
}
