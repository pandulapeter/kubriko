/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

/**
 * A line found by a search of texts, shortened to the words around what it found, with what matched in it, as the
 * searches of the project's texts and of conversations list their finds.
 *
 * @param ranges The characters of [text] that matched, to highlight.
 */
public data class TextSnippet(
    val text: String,
    val ranges: List<IntRange>,
) {
    public companion object {

        /**
         * The snippet of [line] around where it contains [query], ignoring case (every place marked), or null when it
         * doesn't: the line trimmed, and cut to about [length] characters around the first match when longer, with an
         * ellipsis where it was cut.
         */
        public fun of(line: String, query: String, length: Int = DEFAULT_LENGTH): TextSnippet? {
            if (query.isEmpty()) return null
            val starts = generateSequence(line.indexOf(query, ignoreCase = true).takeIf { it >= 0 }) { previous ->
                line.indexOf(query, startIndex = previous + query.length, ignoreCase = true).takeIf { it >= 0 }
            }.toList()
            if (starts.isEmpty()) return null
            val trimmedStart = line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            val trimmedEnd = line.indexOfLast { !it.isWhitespace() } + 1
            var from = trimmedStart
            var to = trimmedEnd
            if (to - from > length) {
                from = (starts.first() - length / 3).coerceIn(trimmedStart, trimmedEnd)
                to = (from + length).coerceAtMost(trimmedEnd)
            }
            val prefix = if (from > trimmedStart) ELLIPSIS else ""
            val suffix = if (to < trimmedEnd) ELLIPSIS else ""
            val ranges = starts.mapNotNull { start ->
                val first = maxOf(start, from)
                val last = minOf(start + query.length, to) - 1
                if (first > last) null else first - from + prefix.length..last - from + prefix.length
            }
            return TextSnippet(prefix + line.substring(from, to) + suffix, ranges)
        }

        private const val DEFAULT_LENGTH = 120
        private const val ELLIPSIS = "…"
    }
}
