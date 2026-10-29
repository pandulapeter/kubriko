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
 * A Claude Code configuration the assistant's Claude Agent can use: Claude Code's default one, or the one a shell alias
 * of the user's runs `claude` with (`alias claude-personal='CLAUDE_CONFIG_DIR=~/.claude-personal claude'`), with its own
 * login.
 *
 * @param alias The alias's name, or null for the default configuration.
 * @param variant What tells it apart (`personal` for `claude-personal`), or null for the default configuration.
 * @param environment The variables the alias sets, which the agent is started with.
 */
internal data class ClaudeConfiguration(
    val alias: String? = null,
    val variant: String? = null,
    val environment: Map<String, String> = emptyMap(),
) {
    companion object {
        val default = ClaudeConfiguration()

        /**
         * The default configuration, then one per alias in [aliases] that runs `claude` with variables of its own.
         */
        fun discover(aliases: Map<String, String>, home: String): List<ClaudeConfiguration> {
            val program = AiClient.CLAUDE_CODE.defaultCommand
            val variants = aliases
                .filter { (name, value) -> name != program && ShellAliases.programOf(value) == program }
                .mapNotNull { (name, value) ->
                    ShellAliases.environmentOf(value, home).takeIf { it.isNotEmpty() }?.let { environment ->
                        ofAlias(name).copy(environment = environment)
                    }
                }
                .sortedBy { it.alias }
            return listOf(default) + variants
        }

        /**
         * The configuration of the alias [name], before its variables are known.
         */
        fun ofAlias(name: String) = ClaudeConfiguration(
            alias = name,
            variant = name.removePrefix(AiClient.CLAUDE_CODE.defaultCommand).trimStart('-', '_', '.').ifEmpty { name },
        )
    }
}
