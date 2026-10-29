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

/**
 * What a command line program wrote, as plain text: its escape sequences (colors, cursor movement, and links) left out,
 * a line it wrote over with a carriage return showing only its last version, and the links it marked kept in [links].
 */
internal data class TerminalText(
    val text: String,
    val links: List<String>,
) {
    companion object {
        private const val ESCAPE = '\u001B'
        private const val BELL = '\u0007'
        private const val HYPERLINK = "8;"
        private val urlPattern = Regex("""https?://[^\s<>"']+""")

        fun of(raw: String): TerminalText {
            val text = StringBuilder()
            val links = LinkedHashSet<String>()
            var index = 0
            while (index < raw.length) {
                val character = raw[index]
                when {
                    character == ESCAPE && raw.getOrNull(index + 1) == ']' -> {
                        val end = oscEnd(raw, index + 2)
                        val body = raw.substring(index + 2, end.first)
                        if (body.startsWith(HYPERLINK)) body.substringAfter(';').substringAfter(';').takeIf { it.isNotEmpty() }?.let(links::add)
                        index = end.second
                    }
                    character == ESCAPE && raw.getOrNull(index + 1) == '[' -> {
                        var end = index + 2
                        while (end < raw.length && raw[end] !in '@'..'~') end++
                        index = end + 1
                    }
                    character == ESCAPE -> index += 2
                    character == '\r' && raw.getOrNull(index + 1) != '\n' -> {
                        text.setLength(text.lastIndexOf('\n') + 1)
                        index++
                    }
                    character == '\r' || character == BELL -> index++
                    else -> {
                        text.append(character)
                        index++
                    }
                }
            }
            val plain = text.toString()
            urlPattern.findAll(plain).forEach { links += it.value }
            return TerminalText(plain, links.toList())
        }

        /**
         * Where the operating system command starting at [start] ends: the index its terminator starts at, and the one
         * after it, ended by a bell or `ESC \`, or by the end of [raw] while it is still being written.
         */
        private fun oscEnd(raw: String, start: Int): Pair<Int, Int> {
            var index = start
            while (index < raw.length) {
                if (raw[index] == BELL) return index to index + 1
                if (raw[index] == ESCAPE && raw.getOrNull(index + 1) == '\\') return index to index + 2
                index++
            }
            return raw.length to raw.length
        }
    }
}
