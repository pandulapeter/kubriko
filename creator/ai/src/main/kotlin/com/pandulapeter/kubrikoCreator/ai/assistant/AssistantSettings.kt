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
 * @param claudeConfigurationAlias The shell alias whose Claude Code configuration Claude Agent uses, or null for the default one.
 * @param agentId The agent chosen ([AgentChoice.id]), or null for Claude Agent.
 * @param customAgentCommand The command line of the custom agent, kept while another agent is chosen.
 * @param agentVariables The names of the environment variables each agent ([AgentChoice.id]) was given for signing in,
 *   whose values are kept in the operating system's secret store.
 * @param agentsChangingFilesUnasked The agents ([AgentChoice.identity]) seen changing files or running commands without
 *   asking ([UnaskedChanges]).
 */
@Serializable
internal data class AssistantSettings(
    val claudeConfigurationAlias: String? = null,
    val agentId: String? = null,
    val customAgentCommand: String? = null,
    val agentVariables: Map<String, List<String>> = emptyMap(),
    val agentsChangingFilesUnasked: Set<String> = emptySet(),
)
