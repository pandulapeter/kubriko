/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

/**
 * One way to run an AI tool on this computer: its usual command, or a shell alias that runs it with settings of its
 * own (`alias claude-personal='CLAUDE_CONFIG_DIR=~/.claude-personal claude'`), each connected on its own.
 *
 * @param variant What tells the setup apart (`personal` for `claude-personal`), or null for the tool's usual command.
 */
internal data class AiClientSetup(
    val client: AiClient,
    val command: String,
    val variant: String? = null,
) {
    /**
     * Identifies the setup in the settings, which remember the connected ones.
     */
    val id get() = if (command == client.defaultCommand) client.id else "${client.id}$SEPARATOR$command"

    companion object {
        private const val SEPARATOR = "@"

        /**
         * The setups of every AI tool: the usual command (or the user's replacement for it), unless an alias of the same
         * name runs something else, and every alias that runs the tool's program.
         */
        fun discover(aliases: Map<String, String>, commandOf: (AiClient) -> String): List<AiClientSetup> = AiClient.entries.flatMap { client ->
            val command = commandOf(client)
            val isShadowed = aliases[command]?.let { ShellAliases.programOf(it) != client.defaultCommand } == true
            val variants = aliases
                .filter { (name, value) -> name != command && name != client.defaultCommand && ShellAliases.programOf(value) == client.defaultCommand }
                .keys
                .sorted()
                .map { name -> AiClientSetup(client, name, variantOf(name, client.defaultCommand)) }
            val usual = AiClientSetup(client, command, variantOf(command, client.defaultCommand).takeIf { command != client.defaultCommand })
            if (isShadowed && variants.isNotEmpty()) variants else listOf(usual) + variants
        }

        /**
         * The setup [id] names, for one connected before this run of Kubriko Creator found its aliases.
         */
        fun fromId(id: String): AiClientSetup? {
            val client = AiClient.entries.firstOrNull { it.id == id.substringBefore(SEPARATOR) } ?: return null
            return if (SEPARATOR in id) {
                val command = id.substringAfter(SEPARATOR)
                AiClientSetup(client, command, variantOf(command, client.defaultCommand))
            } else {
                AiClientSetup(client, client.defaultCommand)
            }
        }

        private fun variantOf(aliasName: String, programName: String) = aliasName.removePrefix(programName).trimStart('-', '_', '.').ifEmpty { aliasName }
    }
}
