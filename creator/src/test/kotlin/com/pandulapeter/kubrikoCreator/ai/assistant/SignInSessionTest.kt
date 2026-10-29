/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.agentclientprotocol.model.AuthMethodId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SignInSessionTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val channel = FakeTerminalChannel()
    private val method = SignInMethod(AuthMethodId("login"), "Login", terminal = AgentCommand("login"))

    @AfterTest
    fun tearDown() {
        channel.stop()
        scope.cancel()
        mainThread.close()
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { withTimeout(10.seconds) { block() } }

    private suspend fun session() = withContext(mainThread) { SignInSession(method, channel, scope, Dispatchers.IO) }

    @Test
    fun theOutputShowsAsItIsWrittenWithItsLinks() = test {
        val session = session()
        channel.write("Visit: \u001B]8;;https://example.com/a\u0007https://exa")
        channel.write("mple.com/a\u001B]8;;\u0007\nPaste code here if prompted > ")
        val output = session.output.first { it.text.endsWith("> ") }
        assertEquals("Visit: https://example.com/a\nPaste code here if prompted > ", output.text)
        assertEquals(listOf("https://example.com/a"), output.links)
    }

    @Test
    fun aTypedCodeReachesTheCommandAndItsExitCodeIsKept() = test {
        val session = session()
        withContext(mainThread) { session.send("the-code") }
        assertEquals("the-code", channel.readTypedLine())
        channel.exit(0)
        assertEquals(0, session.exitCode.first { it != null })
    }

    @Test
    fun stoppingEndsTheCommand() = test {
        val session = session()
        session.stop()
        assertTrue(channel.isStopped)
    }
}
