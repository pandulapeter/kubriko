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

import com.pandulapeter.kubrikoCreator.mcp.clients.ShellAliases
import java.io.File

/**
 * Reads the command line of a custom agent as a shell would: variables set in front (`GOOSE_MODE=chat goose acp`), then
 * the program and its arguments, each word unquoted. A word can't hold a space.
 */
internal object CustomAgentCommand {
    private val whitespace = Regex("\\s+")

    /**
     * The program [commandLine] runs, as typed.
     */
    fun programOf(commandLine: String) = wordsAfterVariables(commandLine).firstOrNull().orEmpty()

    /**
     * What starts the agent: a program named without a folder is looked up with [locate] (as the user's shell finds it),
     * and its folder goes first on the `PATH`.
     */
    fun of(commandLine: String, home: String, locate: (String) -> String?): AgentCommand? {
        val words = wordsAfterVariables(commandLine).ifEmpty { return null }
        val typed = expandHome(words.first(), home)
        val program = if ('/' in typed || '\\' in typed) typed else locate(typed) ?: typed
        return AgentCommand(
            program = program,
            arguments = words.drop(1).map { expandHome(it, home) },
            environment = (if (File(program).isAbsolute) RegistryAgent.withFolderOnPath(File(program)) else emptyMap()) +
                ShellAliases.environmentOf(commandLine, home),
        )
    }

    private fun wordsAfterVariables(commandLine: String) = commandLine.trim().split(whitespace)
        .filter { it.isNotEmpty() }
        .dropWhile { '=' in it || it == "env" }
        .map { ShellAliases.unquote(it) }

    private fun expandHome(word: String, home: String) = if (word == "~" || word.startsWith("~/")) home + word.removePrefix("~") else word
}
