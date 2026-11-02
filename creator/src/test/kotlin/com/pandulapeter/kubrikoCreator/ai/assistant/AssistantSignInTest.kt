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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AssistantSignInTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val terminals = FakeTerminalLauncher()
    private val secrets = FakeSecretStore()
    private val settingsDirectory = createTempDirectory().toFile()
    private val settings = AssistantSettingsState(scope, AssistantSettingsStore(File(settingsDirectory, "assistant.json")), Dispatchers.IO)
    private val host = AssistantHost<Assistant>(scope, Dispatchers.IO, MutableStateFlow(true)) { error("These tests start no agent.") }
    private val restarts = AtomicInteger()
    private val signIn = AssistantSignIn(
        scope = scope,
        ioDispatcher = Dispatchers.IO,
        signInLauncher = terminals,
        secrets = secrets,
        settings = settings,
        host = host,
        resendRefused = {},
        restart = { restarts.incrementAndGet() },
    )
    private val terminalMethod = SignInMethod(AuthMethodId("terminal-login"), "Terminal login", terminal = AgentCommand(FakeAgent.TERMINAL_LOGIN, listOf("login")))
    private val variablesMethod = SignInMethod(AuthMethodId("variables"), "Variables", variables = listOf(SignInMethod.Variable("FAKE_API_KEY")))

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainThread.close()
        settingsDirectory.deleteRecursively()
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { withTimeout(10.seconds) { block() } }

    private suspend fun <T> onMain(block: suspend () -> T) = withContext(mainThread) { block() }

    @Test
    fun aSecondSignInStopsTheFirst() = test {
        onMain {
            signIn.signIn(terminalMethod)
            signIn.signIn(terminalMethod)
        }
        while (terminals.channels.size < 2 || terminals.channels.none { it.isStopped }) delay(10)
        assertEquals(1, terminals.channels.count { it.isStopped })
        delay(100)
        assertNull(signIn.session.value!!.exitCode.value)
    }

    @Test
    fun variablesAreKeptThenTheAgentRestarts() = test {
        onMain { signIn.signIn(variablesMethod) }
        assertEquals(variablesMethod, signIn.variablesForm.value)
        onMain { signIn.setVariables("codex-acp", variablesMethod, mapOf("FAKE_API_KEY" to " secret ")) }
        while (restarts.get() == 0) delay(10)
        assertEquals("secret", secrets.secrets["agent-variable:codex-acp:FAKE_API_KEY"])
        assertNull(signIn.variablesForm.value)
        assertEquals(listOf("FAKE_API_KEY"), settings.value.value.agentVariables["codex-acp"])
    }

    @Test
    fun aFailedVariablesSaveKeepsTheFormOpen() = test {
        secrets.isWritable = false
        onMain { signIn.signIn(variablesMethod) }
        onMain { signIn.setVariables("codex-acp", variablesMethod, mapOf("FAKE_API_KEY" to "secret")) }
        signIn.variablesFailed.first { it }
        assertEquals(variablesMethod, signIn.variablesForm.value)
        assertEquals(0, restarts.get())
        assertTrue(secrets.secrets.isEmpty())
    }
}
