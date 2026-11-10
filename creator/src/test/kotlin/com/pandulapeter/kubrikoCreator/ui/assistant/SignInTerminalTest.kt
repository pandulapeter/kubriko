/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.agentclientprotocol.model.AuthMethodId
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChannel
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentCommand
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInSession
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class SignInTerminalTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val offer = SystemTerminalOffer(isOpening = false, hasFailed = false, onContinue = {})
    private val method = SignInMethod(AuthMethodId("login"), "Agent login", terminal = AgentCommand("agent", listOf("login")))

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    /**
     * A sign-in session whose command printed [output], and ended with [exitCode] unless it is null.
     */
    private fun session(output: String, exitCode: Int?, isInSystemTerminal: Boolean = false): SignInSession {
        val exit = CompletableDeferred<Int>().apply { exitCode?.let(::complete) }
        val channel = object : AgentChannel {
            override val fromAgent: InputStream = output.byteInputStream()
            override val toAgent: OutputStream = OutputStream.nullOutputStream()
            override val errors: InputStream = InputStream.nullInputStream()

            override suspend fun awaitExit() = exit.await()

            override fun stop() = Unit
        }
        val session = SignInSession(method, channel, scope, Dispatchers.IO, isInSystemTerminal)
        runBlocking {
            withTimeout(5.seconds) {
                session.output.first { it.text == output }
                if (exitCode != null) session.exitCode.first { it != null }
            }
        }
        return session
    }

    @Test
    fun aCommandNeedingATerminalDeviceOffersTheSystemsTerminalFirst() = uiTest {
        val continued = mutableListOf<String>()
        val session = session("ERROR Raw mode is not supported on the current process.stdin.\n", exitCode = 1)
        showing { SignInTerminal(session = session, onClose = {}, systemTerminal = SystemTerminalOffer(isOpening = false, hasFailed = false, onContinue = { continued += it.name })) }
        onNodeWithText("This sign-in needs a terminal of its own.").assertExists()
        saveSnapshot("sign-in-needs-terminal")
        onNodeWithText("Continue in a terminal").performClick()
        assertEquals(listOf("Agent login"), continued)
    }

    @Test
    fun aCommandRunningInThePanelCanStillContinueInTheSystemsTerminal() = uiTest {
        val session = session("Paste code here if prompted > ", exitCode = null)
        showing { SignInTerminal(session = session, onClose = {}, systemTerminal = offer) }
        onNodeWithText("Continue in a terminal").assertExists()
        onNodeWithText("This sign-in needs a terminal of its own.").assertDoesNotExist()
        saveSnapshot("sign-in-running")
    }

    @Test
    fun withoutATerminalOfItsOwnNothingIsOffered() = uiTest {
        val session = session("ERROR Raw mode is not supported on the current process.stdin.\n", exitCode = 1)
        showing { SignInTerminal(session = session, onClose = {}, systemTerminal = null) }
        onNodeWithText("Continue in a terminal").assertDoesNotExist()
        onNodeWithText("This sign-in needs a terminal of its own.").assertDoesNotExist()
    }

    @Test
    fun aCommandInTheSystemsTerminalIsWaitedFor() = uiTest {
        val session = session("", exitCode = null, isInSystemTerminal = true)
        showing { SignInTerminal(session = session, onClose = {}, systemTerminal = offer) }
        onNodeWithText("Signing in in a terminal window: Agent login").assertExists()
        onNodeWithText("Finish signing in in the terminal window", substring = true).assertExists()
        onNodeWithText("Continue in a terminal").assertDoesNotExist()
        saveSnapshot("sign-in-in-system-terminal")
    }

    @Test
    fun aCommandThatFailedInTheSystemsTerminalCanBeTriedThereAgain() = uiTest {
        val session = session("", exitCode = 1, isInSystemTerminal = true)
        showing { SignInTerminal(session = session, onClose = {}, systemTerminal = offer) }
        onNodeWithText("Finish signing in in the terminal window", substring = true).assertDoesNotExist()
        onNodeWithText("Continue in a terminal").assertExists()
    }

    @Test
    fun aTerminalThatCouldntBeOpenedIsSaid() = uiTest {
        val session = session("Paste code here if prompted > ", exitCode = null)
        showing { SignInTerminal(session = session, onClose = {}, systemTerminal = offer.copy(hasFailed = true)) }
        onNodeWithText("The terminal couldn't be opened.").assertExists()
        saveSnapshot("sign-in-system-terminal-failed")
    }
}
