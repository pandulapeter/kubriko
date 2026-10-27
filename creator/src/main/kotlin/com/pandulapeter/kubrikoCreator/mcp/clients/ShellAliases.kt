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

    fun parse(output: String): Map<String, String> = output.lines().mapNotNull { line ->
        val definition = line.trim().removePrefix("alias ").trim()
        val separator = definition.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        definition.substring(0, separator) to unquote(definition.substring(separator + 1))
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
     * [text] as a POSIX shell reads it: quotes removed, escapes resolved.
     */
    internal fun unquote(text: String) = buildString {
        var index = 0
        while (index < text.length) {
            when (val character = text[index]) {
                '\'' -> {
                    val end = text.indexOf('\'', index + 1).let { if (it < 0) text.length else it }
                    append(text, index + 1, end)
                    index = end + 1
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
    private val prefixes = setOf("env", "exec", "command")
}
