/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

/**
 * The arguments of a Java `@argfile`, read as the `java` launcher reads them: separated by whitespace, grouped by single or
 * double quotes (inside which a backslash escapes, and one at the end of a line continues the argument on the next line,
 * without its leading whitespace), and with lines starting with `#` left out.
 */
internal object JavaArgumentFile {

    fun parse(text: String): List<String> {
        val arguments = mutableListOf<String>()
        val argument = StringBuilder()
        var isInArgument = false
        var quote: Char? = null
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                quote != null -> when (char) {
                    quote -> quote = null
                    '\\' -> index = appendEscaped(text, index + 1, argument)
                    else -> argument.append(char)
                }
                char == '"' || char == '\'' -> {
                    quote = char
                    isInArgument = true
                }
                char.isWhitespace() -> if (isInArgument) {
                    arguments += argument.toString()
                    argument.clear()
                    isInArgument = false
                }
                char == '#' && !isInArgument -> while (index + 1 < text.length && text[index + 1] != '\n') index++
                else -> {
                    argument.append(char)
                    isInArgument = true
                }
            }
            index++
        }
        if (isInArgument) arguments += argument.toString()
        return arguments
    }

    /**
     * Appends what the escape sequence whose character is at [index] stands for to [argument].
     *
     * @return The index of the escape sequence's last character.
     */
    private fun appendEscaped(text: String, index: Int, argument: StringBuilder): Int {
        if (index >= text.length) return index
        return when (val char = text[index]) {
            '\r', '\n' -> {
                var end = index
                if (char == '\r' && text.getOrNull(end + 1) == '\n') end++
                while (text.getOrNull(end + 1) == ' ' || text.getOrNull(end + 1) == '\t') end++
                end
            }
            else -> {
                argument.append(
                    when (char) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        'f' -> '\u000C'
                        else -> char
                    },
                )
                index
            }
        }
    }
}
