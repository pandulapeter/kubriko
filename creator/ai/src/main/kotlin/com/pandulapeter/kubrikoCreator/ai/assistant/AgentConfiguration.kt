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
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellAliases

/**
 * An account an agent can run as: its tool's default configuration, or the one a shell alias of the user's runs the tool
 * with (`alias claude-personal='CLAUDE_CONFIG_DIR=~/.claude-personal claude'`, `alias codex-work='CODEX_HOME=~/.codex-work
 * codex'`), with its own login.
 *
 * @param client The tool whose configuration it is.
 * @param alias The alias's name, or null for the default configuration.
 * @param variant What tells it apart (`personal` for `claude-personal`), or null for the default configuration.
 * @param environment The variables the alias sets, which the agent is started with.
 */
public data class AgentConfiguration(
    val client: AiClient,
    val alias: String? = null,
    val variant: String? = null,
    val environment: Map<String, String> = emptyMap(),
) {
    public companion object {
        private val clientsByAgentId = mapOf(
            RegistryAgent.claude.id to AiClient.CLAUDE_CODE,
            RegistryAgent.CODEX_ID to AiClient.CODEX,
        )

        /**
         * The tool whose configurations [choice] runs with, which its configuration folder's variable picks
         * (`CLAUDE_CONFIG_DIR`, `CODEX_HOME`), or null for an agent whose tool Kubriko Creator doesn't know.
         */
        internal fun clientOf(choice: AgentChoice): AiClient? = (choice as? AgentChoice.Registry)?.let { clientsByAgentId[it.agent.id] }

        /**
         * [client]'s default configuration, then one per alias in [aliases] that runs its program with variables of its own.
         */
        internal fun discover(client: AiClient, aliases: Map<String, String>, home: String): List<AgentConfiguration> {
            val program = client.defaultCommand
            val variants = aliases
                .filter { (name, value) -> name != program && ShellAliases.programOf(value) == program }
                .mapNotNull { (name, value) ->
                    ShellAliases.environmentOf(value, home).takeIf { it.isNotEmpty() }?.let { environment ->
                        ofAlias(client, name).copy(environment = environment)
                    }
                }
                .sortedBy { it.alias }
            return listOf(AgentConfiguration(client)) + variants
        }

        /**
         * [client]'s configuration of the alias [name], before its variables are known.
         */
        internal fun ofAlias(client: AiClient, name: String) = AgentConfiguration(
            client = client,
            alias = name,
            variant = name.removePrefix(client.defaultCommand).trimStart('-', '_', '.').ifEmpty { name },
        )
    }
}
