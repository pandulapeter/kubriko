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
import com.pandulapeter.kubrikoCreator.testing.IdleTrackingDispatcher
import com.pandulapeter.kubrikoCreator.testing.awaitIdle
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AssistantSignInTest {
    private val mainExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainThread = IdleTrackingDispatcher(mainExecutor)
    private val io = IdleTrackingDispatcher(Dispatchers.IO)
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val terminals = FakeTerminalLauncher()
    private val systemTerminals = FakeTerminalLauncher()
    private val resends = AtomicInteger()
    private val secrets = FakeSecretStore()
    private val settingsDirectory = createTempDirectory().toFile()
    private val settings = AssistantSettingsState(scope, AssistantSettingsStore(File(settingsDirectory, "assistant.json")), io)
    private val host = AssistantHost<Assistant>(scope, io, MutableStateFlow(true)) { error("These tests start no agent.") }
    private val restarts = AtomicInteger()
    private val signIn = AssistantSignIn(
        scope = scope,
        ioDispatcher = io,
        signInLauncher = terminals,
        systemTerminal = systemTerminals,
        secrets = secrets,
        settings = settings,
        host = host,
        resendRefused = { resends.incrementAndGet() },
        restart = { restarts.incrementAndGet() },
    )
    private val terminalMethod = SignInMethod(AuthMethodId("terminal-login"), "Terminal login", terminal = AgentCommand(FakeAgent.TERMINAL_LOGIN, listOf("login")))
    private val variablesMethod = SignInMethod(AuthMethodId("variables"), "Variables", variables = listOf(SignInMethod.Variable("FAKE_API_KEY")))

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainExecutor.close()
        settingsDirectory.deleteRecursively()
    }

    private suspend fun <T> onMain(block: suspend () -> T) = withContext(mainThread) { block() }

    @Test
    fun aSecondSignInStopsTheFirst() = blockingTest {
        onMain {
            signIn.signIn(terminalMethod)
            signIn.signIn(terminalMethod)
        }
        while (terminals.channels.size < 2 || terminals.channels.none { it.isStopped }) delay(10)
        assertEquals(1, terminals.channels.count { it.isStopped })
        awaitIdle(mainThread, io)
        assertNull(signIn.session.value!!.exitCode.value)
    }

    @Test
    fun aSignInContinuedInTheSystemsTerminalEndsThePanelsAndSendsTheRefusedPromptsAgainOnceItWorks() = blockingTest {
        onMain { signIn.signIn(terminalMethod) }
        while (terminals.channels.isEmpty()) delay(10)
        onMain { signIn.signIn(terminalMethod, isInSystemTerminal = true) }
        while (systemTerminals.channels.isEmpty()) delay(10)
        assertTrue(terminals.channels.single().isStopped)
        assertEquals(listOf(terminalMethod.terminal), systemTerminals.commands)
        val session = signIn.session.first { it?.isInSystemTerminal == true }!!
        systemTerminals.channels.single().exit(0)
        session.exitCode.first { it != null }
        signIn.session.first { it == null }
        assertEquals(1, resends.get())
    }

    @Test
    fun aSystemsTerminalThatCantBeOpenedKeepsThePanelsSignInAndSaysSo() = blockingTest {
        onMain { signIn.signIn(terminalMethod) }
        while (terminals.channels.isEmpty()) delay(10)
        val panelSession = signIn.session.first { it != null }
        systemTerminals.isFailing = true
        onMain { signIn.signIn(terminalMethod, isInSystemTerminal = true) }
        signIn.systemTerminalFailed.first { it }
        assertSame(panelSession, signIn.session.value)
        assertFalse(terminals.channels.single().isStopped)
        assertFalse(signIn.isOpeningSystemTerminal.value)
        onMain { signIn.closeSignIn() }
        assertFalse(signIn.systemTerminalFailed.value)
    }

    @Test
    fun theSystemsTerminalIsOpenedOnceWhileItOpens() = blockingTest {
        onMain {
            signIn.signIn(terminalMethod, isInSystemTerminal = true)
            assertTrue(signIn.isOpeningSystemTerminal.value)
            signIn.signIn(terminalMethod, isInSystemTerminal = true)
        }
        signIn.session.first { it != null }
        awaitIdle(mainThread, io)
        assertEquals(1, systemTerminals.commands.size)
    }

    @Test
    fun withoutATerminalOfItsOwnTheSystemsTerminalIsntOffered() = blockingTest {
        val withoutTerminal = AssistantSignIn(scope, io, terminals, systemTerminal = null, secrets, settings, host, resendRefused = {}, restart = {})
        assertFalse(withoutTerminal.canUseSystemTerminal)
        assertTrue(signIn.canUseSystemTerminal)
        onMain { withoutTerminal.signIn(terminalMethod, isInSystemTerminal = true) }
        awaitIdle(mainThread, io)
        assertNull(withoutTerminal.session.value)
        assertTrue(terminals.commands.isEmpty())
    }

    @Test
    fun variablesAreKeptThenTheAgentRestarts() = blockingTest {
        onMain { signIn.signIn(variablesMethod) }
        assertEquals(variablesMethod, signIn.variablesForm.value)
        onMain { signIn.setVariables("codex-acp", variablesMethod, mapOf("FAKE_API_KEY" to " secret ")) }
        while (restarts.get() == 0) delay(10)
        assertEquals("secret", secrets.secrets["agent-variable:codex-acp:FAKE_API_KEY"])
        assertNull(signIn.variablesForm.value)
        assertEquals(listOf("FAKE_API_KEY"), settings.value.value.agentVariables["codex-acp"])
    }

    @Test
    fun aFailedVariablesSaveKeepsTheFormOpen() = blockingTest {
        secrets.isWritable = false
        onMain { signIn.signIn(variablesMethod) }
        onMain { signIn.setVariables("codex-acp", variablesMethod, mapOf("FAKE_API_KEY" to "secret")) }
        signIn.variablesFailed.first { it }
        assertEquals(variablesMethod, signIn.variablesForm.value)
        assertEquals(0, restarts.get())
        assertTrue(secrets.secrets.isEmpty())
    }
}
