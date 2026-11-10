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

import com.pandulapeter.kubrikoCreator.mcp.clients.ProcessRunner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The accounts agents run as ([AgentConfiguration]): the ones the user's shell aliases set up for each agent's tool, and the
 * one chosen for each agent.
 *
 * @param scope Where the aliases are found: the main thread, which the panel reads the configurations on.
 * @param agent The agent chosen, whose configurations are offered.
 * @param rememberedAliases The alias of the configuration chosen in an earlier run for each agent ([AgentChoice.id]),
 *   resolved once the aliases are found.
 */
internal class AgentConfigurations(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val runner: ProcessRunner,
    private val home: String,
    private val agent: StateFlow<AgentChoice>,
    rememberedAliases: Map<String, String>,
) {
    private var aliases: Map<String, String>? = null
    private var hasFoundAliases = false
    private val discovery = CompletableDeferred<Unit>()

    /**
     * The alias of the configuration chosen for each agent ([AgentChoice.id]), for the settings.
     */
    var chosenAliases = rememberedAliases
        private set
    private val _configurations = MutableStateFlow(configurationsOf(agent.value))

    /**
     * The chosen agent's configurations found on this computer, its default one first, or none for an agent whose tool
     * Kubriko Creator doesn't know.
     */
    val configurations = _configurations.asStateFlow()
    private val _configuration = MutableStateFlow(chosenOf(agent.value))

    /**
     * The configuration the chosen agent uses, or null for an agent whose tool Kubriko Creator doesn't know.
     */
    val configuration = _configuration.asStateFlow()

    init {
        scope.launch { agent.collect { refresh() } }
    }

    /**
     * Finds the configurations of the user's shell aliases, once per run. A remembered configuration whose alias is gone,
     * or whose shell can't list its aliases, falls back to the default one.
     */
    fun find() {
        if (hasFoundAliases) return
        hasFoundAliases = true
        scope.launch {
            try {
                aliases = try {
                    withContext(ioDispatcher) { runner.aliases() }
                } catch (_: IOException) {
                    emptyMap()
                }
                refresh()
            } finally {
                discovery.complete(Unit)
            }
        }
    }

    /**
     * What tells [choice]'s chosen account apart from its others, which what it signs in with is kept by: its ID, followed
     * by the alias it runs as, if any.
     */
    fun accountOf(choice: AgentChoice) = chosenOf(choice)?.alias?.let { "${choice.id}$ACCOUNT_SEPARATOR$it" } ?: choice.id

    /**
     * Makes the chosen agent use [configuration] from now on.
     *
     * @return Whether it is another one than before.
     */
    fun select(configuration: AgentConfiguration): Boolean {
        val choice = agent.value
        if (AgentConfiguration.clientOf(choice) != configuration.client || configuration.alias == chosenAliases[choice.id]) return false
        chosenAliases = configuration.alias?.let { chosenAliases + (choice.id to it) } ?: (chosenAliases - choice.id)
        refresh()
        return true
    }

    /**
     * The variables of the configuration [choice] uses, once the aliases are found if it uses one of theirs.
     */
    suspend fun environmentOf(choice: AgentChoice): Map<String, String> {
        if (chosenAliases[choice.id] == null) return emptyMap()
        find()
        discovery.await()
        return chosenOf(choice)?.environment.orEmpty()
    }

    private fun refresh() {
        val configurations = configurationsOf(agent.value)
        _configurations.value = configurations
        _configuration.value = chosenOf(agent.value, configurations)
    }

    private fun configurationsOf(choice: AgentChoice): List<AgentConfiguration> {
        val client = AgentConfiguration.clientOf(choice) ?: return emptyList()
        return AgentConfiguration.discover(client, aliases.orEmpty(), home)
    }

    private fun chosenOf(choice: AgentChoice, configurations: List<AgentConfiguration> = configurationsOf(choice)): AgentConfiguration? {
        val client = AgentConfiguration.clientOf(choice) ?: return null
        val alias = chosenAliases[choice.id] ?: return AgentConfiguration(client)
        if (aliases == null) return AgentConfiguration.ofAlias(client, alias)
        return configurations.firstOrNull { it.alias == alias } ?: AgentConfiguration(client)
    }

    private companion object {
        const val ACCOUNT_SEPARATOR = "@"
    }
}
