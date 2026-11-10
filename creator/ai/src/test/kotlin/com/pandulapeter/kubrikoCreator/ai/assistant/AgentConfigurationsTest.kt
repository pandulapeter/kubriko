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

import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import com.pandulapeter.kubrikoCreator.testing.FakeProcessRunner
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentConfigurationsTest {
    private val mainExecutor = Executors.newSingleThreadExecutor()
    private val mainThread = mainExecutor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val runner = FakeProcessRunner()
    private val codex = AgentChoice.Registry(RegistryAgent(RegistryAgent.CODEX_ID, "Codex", "2.4.0", RegistryAgent.claude.distribution))

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainExecutor.shutdownNow()
    }

    private suspend fun configurations(rememberedAliases: Map<String, String> = emptyMap()) = withContext(mainThread) {
        AgentConfigurations(scope, Dispatchers.IO, runner, "/Users/me", MutableStateFlow(codex), rememberedAliases)
    }

    @Test
    fun aRememberedAliasThatIsGoneCanBeClearedByChoosingTheDefault() = blockingTest {
        val configurations = configurations(mapOf(RegistryAgent.CODEX_ID to "codex-gone"))
        withContext(mainThread) { configurations.find() }
        assertEquals(emptyMap(), configurations.environmentOf(codex))
        configurations.configuration.first { it == AgentConfiguration(AiClient.CODEX) }
        assertTrue(withContext(mainThread) { configurations.select(AgentConfiguration(AiClient.CODEX)) })
        assertEquals(emptyMap(), configurations.chosenAliases)
        assertFalse(withContext(mainThread) { configurations.select(AgentConfiguration(AiClient.CODEX)) })
    }

    @Test
    fun eachAccountKeepsWhatItSignsInWithApart() = blockingTest {
        runner.aliases["codex-work"] = "CODEX_HOME=~/.codex-work codex"
        val configurations = configurations()
        assertEquals(RegistryAgent.CODEX_ID, configurations.accountOf(codex))
        withContext(mainThread) { configurations.find() }
        val work = configurations.configurations.first { it.size == 2 }.last()
        withContext(mainThread) { configurations.select(work) }
        assertEquals("${RegistryAgent.CODEX_ID}@codex-work", configurations.accountOf(codex))
        assertEquals(RegistryAgent.claude.id, configurations.accountOf(AgentChoice.Registry(RegistryAgent.claude)))
    }

    @Test
    fun anAgentStartsWithTheDefaultConfigurationWhenTheAliasesCantBeListed() = blockingTest {
        runner.areAliasesFailing = true
        val configurations = configurations(mapOf(RegistryAgent.CODEX_ID to "codex-work"))
        assertEquals(emptyMap(), configurations.environmentOf(codex))
    }
}
