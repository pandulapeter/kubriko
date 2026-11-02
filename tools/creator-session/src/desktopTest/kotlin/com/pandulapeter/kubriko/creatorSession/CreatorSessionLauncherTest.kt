/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession

import com.pandulapeter.kubriko.creatorSession.implementation.SessionConnection
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreatorSessionLauncherTest {

    @Test
    fun anEditSessionSaysHelloThenFails() = ServerSocket(0, 0, InetAddress.getLoopbackAddress()).use { server ->
        val message = "This Kubriko version's session no longer opens the Scene editor; Kubriko Creator edits Scenes in its own window."
        val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            SessionConnection.open(server.localPort).use { connection ->
                server.accept().use { socket ->
                    socket.soTimeout = 10_000
                    assertEquals(message, run(listOf("edit"), connection, logScope))
                    logScope.cancel()
                    connection.close()
                    val messages = socket.getInputStream().bufferedReader().readLines().map(SessionProtocol::decode)
                    val helloIndex = messages.indexOf(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.EDIT))
                    assertEquals(0, helloIndex)
                    assertTrue(messages.indexOf(SessionMessage.Failure(message)) > helloIndex)
                }
            }
        } finally {
            logScope.cancel()
        }
    }
}
