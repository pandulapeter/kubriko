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

import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An AI tool Kubriko Creator can connect to its MCP server in one click. Every supported tool is a command-line program
 * that manages its MCP servers with `mcp add`, `mcp get`, and `mcp remove`, so a new one only says how it spells them.
 *
 * @param displayName The tool's own name, shown as it is.
 * @param defaultCommand The program's name. The user can replace it with any command line that runs the tool.
 */
@Serializable
internal enum class AiClient(
    val displayName: String,
    val defaultCommand: String,
    val installUrl: String,
) {
    @SerialName("claudeCode")
    CLAUDE_CODE("Claude Code", "claude", "https://code.claude.com/docs/en/setup") {
        override fun addArguments(settings: McpSettings) = listOf(
            "mcp", "add", "--scope", "user", "--transport", "http", SERVER_NAME, settings.url, "--header", "Authorization: Bearer ${settings.token}",
        )

        override val removeArguments = listOf("mcp", "remove", "--scope", "user", SERVER_NAME)
    },

    /**
     * Codex can only read a bearer token from an environment variable, so it gets the token in the URL instead.
     */
    @SerialName("codex")
    CODEX("Codex", "codex", "https://developers.openai.com/codex/cli") {
        override fun addArguments(settings: McpSettings) = listOf("mcp", "add", SERVER_NAME, "--url", settings.urlWithToken)

        override val removeArguments = listOf("mcp", "remove", SERVER_NAME)
    },

    @SerialName("geminiCli")
    GEMINI_CLI("Gemini CLI", "gemini", "https://github.com/google-gemini/gemini-cli") {
        override fun addArguments(settings: McpSettings) = listOf(
            "mcp", "add", "--scope", "user", "--transport", "http", "--header", "Authorization: Bearer ${settings.token}", SERVER_NAME, settings.url,
        )

        override val removeArguments = listOf("mcp", "remove", "--scope", "user", SERVER_NAME)

        override val getArguments = listOf("mcp", "list")
    };

    /**
     * Registers Kubriko Creator's server for every project of the user, not just the folder the command runs in.
     */
    abstract fun addArguments(settings: McpSettings): List<String>

    abstract val removeArguments: List<String>

    open val getArguments = listOf("mcp", "get", SERVER_NAME)

    /**
     * Whether the output of [getArguments] says Kubriko Creator's server is registered: every supported tool starts a
     * line with the server's name (after a status mark, at most). The exit code alone isn't enough, since a wrapper
     * script or alias can print something else (its own arguments, even) and still succeed.
     */
    fun isConnected(exitCode: Int, output: String) = exitCode == 0 && output.lines().any { it.trimStart { character -> !character.isLetterOrDigit() }.startsWith(SERVER_NAME) }
}

private const val SERVER_NAME = McpToolServer.SERVER_NAME
