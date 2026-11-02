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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The agent the assistant runs, chosen out of the registry's agents or a custom command, and remembered in [settings].
 *
 * @param registry Where the registry's agents come from, or null for Claude Agent alone.
 * @param isAiEnabled Whether AI features are on: the registry is fetched only once they are.
 * @param isAgentRunning Whether an agent runs, which a registry loaded meanwhile then doesn't switch.
 */
internal class AgentSelection(
    scope: CoroutineScope,
    private val settings: AssistantSettingsState,
    private val registry: AgentRegistryStore?,
    private val ioDispatcher: CoroutineDispatcher,
    private val isAiEnabled: StateFlow<Boolean>,
    private val isAgentRunning: () -> Boolean,
) {
    private val _agents = MutableStateFlow(listOf(RegistryAgent.claude))

    /**
     * The registry's agents this computer can run, Claude Agent first.
     */
    val agents = _agents.asStateFlow()
    private val _agent = MutableStateFlow(AgentChoice.of(settings.value.value, _agents.value))

    /**
     * The agent the assistant runs.
     */
    val agent = _agent.asStateFlow()

    /**
     * The command line of the custom agent, whether or not it is the one chosen.
     */
    val customAgentCommand get() = settings.value.value.customAgentCommand
    private val agentsLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch { loadAgents() }
    }

    /**
     * Runs [choice] from now on, and remembers it.
     *
     * @return Whether it is a different agent than the one before.
     */
    fun select(choice: AgentChoice): Boolean {
        if (choice == _agent.value) return false
        _agent.value = choice
        settings.update { it.copy(agentId = choice.id.takeUnless { choice.isClaude }, customAgentCommand = (choice as? AgentChoice.Custom)?.commandLine ?: it.customAgentCommand) }
        return true
    }

    /**
     * Waits for the cached registry, so that a remembered agent is known.
     */
    suspend fun awaitLoaded() = agentsLoaded.await()

    /**
     * The cached registry first, so that a remembered agent is known before the first conversation starts, then a fresh one.
     */
    private suspend fun loadAgents() {
        if (registry == null) {
            agentsLoaded.complete(Unit)
            return
        }
        updateAgents(withContext(ioDispatcher) { registry.cached() })
        agentsLoaded.complete(Unit)
        isAiEnabled.first { it }
        withContext(ioDispatcher) { registry.refresh() }?.let(::updateAgents)
    }

    private fun updateAgents(agents: List<RegistryAgent>) {
        _agents.value = agents
        if (!isAgentRunning()) _agent.value = AgentChoice.of(settings.value.value, _agents.value)
    }
}
