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

import kotlinx.serialization.Serializable

/**
 * What the assistant panel remembers between runs of Kubriko Creator.
 *
 * @param configurationAliases The shell alias whose configuration each agent ([AgentChoice.id]) runs as
 *   ([AgentConfiguration]), for those not using the default one.
 * @param claudeConfigurationAlias The shell alias Claude Agent ran as when it was the only agent with configurations, read
 *   into [configurationAliases] by [withConfigurationAliasesMigrated].
 * @param agentId The agent chosen ([AgentChoice.id]), or null for Claude Agent.
 * @param customAgentCommand The command line of the custom agent, kept while another agent is chosen.
 * @param agentVariables The names of the environment variables each agent's account ([AgentConfigurations.accountOf]) was
 *   given for signing in, whose values are kept in the operating system's secret store.
 * @param agentsChangingFilesUnasked The agents ([AgentChoice.identity]) seen changing files with their own tools without
 *   asking ([UnaskedChanges]).
 */
@Serializable
internal data class AssistantSettings(
    val configurationAliases: Map<String, String> = emptyMap(),
    val claudeConfigurationAlias: String? = null,
    val agentId: String? = null,
    val customAgentCommand: String? = null,
    val agentVariables: Map<String, List<String>> = emptyMap(),
    val agentsChangingFilesUnasked: Set<String> = emptySet(),
) {
    /**
     * These settings with the agent [identity] marked as changing files without asking, keeping only the custom agent marked
     * last, since the custom agent's earlier commands are forgotten too.
     */
    fun withChangingFilesUnasked(identity: String) = copy(
        agentsChangingFilesUnasked = agentsChangingFilesUnasked.filterNot { AgentChoice.Custom.isIdentity(it) && AgentChoice.Custom.isIdentity(identity) }.toSet() + identity,
    )

    /**
     * These settings with [claudeConfigurationAlias] moved into [configurationAliases].
     */
    fun withConfigurationAliasesMigrated() = claudeConfigurationAlias?.let { alias ->
        copy(configurationAliases = mapOf(RegistryAgent.claude.id to alias) + configurationAliases, claudeConfigurationAlias = null)
    } ?: this
}
