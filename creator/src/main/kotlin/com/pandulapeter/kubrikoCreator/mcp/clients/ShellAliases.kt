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
 * Reads the aliases a shell lists when asked for `alias` (zsh writes `name='value'`, bash `alias name='value'`).
 */
internal object ShellAliases {

    /**
     * The aliases in the output of a shell's `alias` command: `name='value'` (zsh), `alias name='value'` (bash), or
     * `alias name 'value'` (fish, whose quoting differs).
     */
    fun parse(output: String): Map<String, String> = output.lines().mapNotNull { line ->
        val trimmed = line.trim()
        val definition = trimmed.removePrefix("alias ").trim()
        val space = definition.indexOfFirst { it.isWhitespace() }
        val equalsSign = definition.indexOf('=')
        val isFish = trimmed.startsWith("alias ") && space > 0 && (equalsSign < 0 || space < equalsSign)
        when {
            isFish -> definition.substring(0, space) to unquote(definition.substring(space + 1).trim(), isFish = true)
            equalsSign > 0 -> definition.substring(0, equalsSign) to unquote(definition.substring(equalsSign + 1))
            else -> null
        }
    }.toMap()

    /**
     * The name of the program [command] runs: its first word that neither sets an environment variable nor is `env`,
     * `exec`, or `command`, without its folder.
     */
    fun programOf(command: String) = command.trim().split(whitespace)
        .firstOrNull { '=' !in it && it !in prefixes }
        ?.substringAfterLast('/')
        .orEmpty()

    /**
     * The environment variables [command] sets for its program (`CLAUDE_CONFIG_DIR=~/.claude-personal claude`), in front
     * of it or after `env`, with a leading `~` or `$HOME` replaced by [home].
     */
    fun environmentOf(command: String, home: String) = command.trim().split(whitespace)
        .takeWhile { '=' in it || it in prefixes }
        .filter { '=' in it }
        .associate { assignment ->
            val value = unquote(assignment.substringAfter('='))
            assignment.substringBefore('=') to when {
                value == "~" || value.startsWith("~/") -> home + value.removePrefix("~")
                value.startsWith(HOME_VARIABLE) -> home + value.removePrefix(HOME_VARIABLE)
                value.startsWith(BRACED_HOME_VARIABLE) -> home + value.removePrefix(BRACED_HOME_VARIABLE)
                else -> value
            }
        }

    /**
     * [text] as a POSIX shell (or, with [isFish], fish) reads it: quotes removed, escapes resolved. Fish differs only in
     * single quotes, where `\'` and `\\` are escapes.
     */
    internal fun unquote(text: String, isFish: Boolean = false) = buildString {
        var index = 0
        while (index < text.length) {
            when (val character = text[index]) {
                '\'' -> {
                    index++
                    while (index < text.length && text[index] != '\'') {
                        if (isFish && text[index] == '\\' && index + 1 < text.length && text[index + 1] in fishSingleQuoteEscapes) index++
                        append(text[index])
                        index++
                    }
                    index++
                }
                '"' -> {
                    index++
                    while (index < text.length && text[index] != '"') {
                        if (text[index] == '\\' && index + 1 < text.length) index++
                        append(text[index])
                        index++
                    }
                    index++
                }
                '\\' -> {
                    if (index + 1 < text.length) append(text[index + 1])
                    index += 2
                }
                else -> {
                    append(character)
                    index++
                }
            }
        }
    }

    private val whitespace = Regex("\\s+")
    private val fishSingleQuoteEscapes = setOf('\'', '\\')
    private val prefixes = setOf("env", "exec", "command")
    private const val HOME_VARIABLE = "\$HOME"
    private const val BRACED_HOME_VARIABLE = "\${HOME}"
}
