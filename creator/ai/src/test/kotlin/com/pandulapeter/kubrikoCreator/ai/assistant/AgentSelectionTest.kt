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
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentSelectionTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val settingsDirectory = createTempDirectory().toFile()
    private val settingsFile = File(settingsDirectory, "assistant.json")

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainThread.close()
        settingsDirectory.deleteRecursively()
    }

    private fun registry(fetched: String? = CODEX) = AgentRegistryStore(File(settingsDirectory, "agent-registry.json"), "darwin-aarch64") {
        fetched?.let { """{"agents": [$it]}""" }
    }

    private fun selection(
        registry: AgentRegistryStore?,
        isAiEnabled: Boolean = true,
        isAgentRunning: () -> Boolean = { false },
    ) = AgentSelection(scope, AssistantSettingsState(scope, AssistantSettingsStore(settingsFile), Dispatchers.IO), registry, Dispatchers.IO, MutableStateFlow(isAiEnabled), isAgentRunning)

    @Test
    fun aRememberedRegistryAgentIsChosenOnceTheCacheLoads() = blockingTest {
        registry().refresh()
        AssistantSettingsStore(settingsFile).save(AssistantSettings(agentId = "codex-acp"))
        val selection = selection(registry(fetched = null), isAiEnabled = false)
        selection.awaitLoaded()
        assertEquals("Codex", selection.agent.value.name)
        assertEquals(listOf("Claude Agent", "Codex"), selection.agents.value.map { it.name })
    }

    @Test
    fun aFetchedRegistryDoesntSwitchTheAgentWhileOneRuns() = blockingTest {
        AssistantSettingsStore(settingsFile).save(AssistantSettings(agentId = "codex-acp"))
        val selection = selection(registry(), isAgentRunning = { true })
        selection.agents.first { agents -> agents.any { it.id == "codex-acp" } }
        assertTrue(selection.agent.value.isClaude)
    }

    @Test
    fun choosingAnAgentRemembersIt() = blockingTest {
        val selection = selection(registry = null)
        selection.awaitLoaded()
        val choice = AgentChoice.Custom("goose acp")
        assertTrue(withContext(mainThread) { selection.select(choice) })
        assertFalse(withContext(mainThread) { selection.select(choice) })
        assertEquals(choice, selection.agent.value)
        while (AssistantSettingsStore(settingsFile).load().agentId != AgentChoice.Custom.ID) delay(10)
        assertEquals("goose acp", AssistantSettingsStore(settingsFile).load().customAgentCommand)
    }

    private companion object {
        const val CODEX = """{"id": "codex-acp", "name": "Codex", "version": "2.4.0", "distribution": {"npx": {"package": "@zed-industries/codex-acp@2.4.0"}}}"""
    }
}
