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
 * How a name matches what is typed to find it, as IDEs match: ignoring case and the query's spaces, and failing a
 * plain match, the query's letters in order with its first one starting a word (`brk` finds `Brick`, `bb` finds
 * `BigBall`).
 */
public object NameMatching {

    /**
     * How well [name] matches [query], with what of it matched, or null when it doesn't. A blank query matches every
     * name, highlighting nothing.
     */
    public fun match(name: String, query: String): NameMatch? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return NameMatch(NameMatch.Quality.EXACT, emptyList())
        val compact = trimmed.filterNot(Char::isWhitespace)
        return plainMatch(name, trimmed) ?: plainMatch(name, compact) ?: scatteredMatch(name, compact)
    }

    /**
     * Whether [name] matches [query] ([match]).
     */
    public fun matches(name: String, query: String): Boolean = match(name, query) != null

    private fun plainMatch(name: String, query: String): NameMatch? {
        val quality = when {
            name.equals(query, ignoreCase = true) -> NameMatch.Quality.EXACT
            name.startsWith(query, ignoreCase = true) -> NameMatch.Quality.PREFIX
            else -> null
        }
        if (quality != null) return NameMatch(quality, listOf(query.indices))
        var index = name.indexOf(query, ignoreCase = true)
        val first = index.takeIf { it >= 0 } ?: return null
        while (index >= 0) {
            if (isWordStart(name, index)) return NameMatch(NameMatch.Quality.WORD_START, listOf(index until index + query.length))
            index = name.indexOf(query, startIndex = index + 1, ignoreCase = true)
        }
        return NameMatch(NameMatch.Quality.CONTAINS, listOf(first until first + query.length))
    }

    /**
     * The query's letters in order, the first starting a word, each taken as early as it comes, which finds a match
     * whenever there is one, since starting later leaves fewer letters to take the rest from.
     */
    private fun scatteredMatch(name: String, query: String): NameMatch? {
        val start = name.indices.firstOrNull { isWordStart(name, it) && name[it].equals(query[0], ignoreCase = true) } ?: return null
        val ranges = ArrayList<IntRange>()
        var rangeStart = start
        var previous = start
        for (queryIndex in 1 until query.length) {
            val index = (previous + 1 until name.length).firstOrNull { name[it].equals(query[queryIndex], ignoreCase = true) } ?: return null
            if (index != previous + 1) {
                ranges += rangeStart..previous
                rangeStart = index
            }
            previous = index
        }
        ranges += rangeStart..previous
        return NameMatch(NameMatch.Quality.SCATTERED, ranges)
    }

    /**
     * Whether a word of [name] starts at [index]: its first letter, a capital after a small letter (`Ball` in
     * `BigBall`) or ending a run of capitals before a small letter (`Server` in `HTTPServer`), a letter or digit after
     * anything else (`ball` in `big_ball`), or a digit after a letter.
     */
    private fun isWordStart(name: String, index: Int): Boolean {
        if (index == 0) return true
        val previous = name[index - 1]
        val current = name[index]
        return when {
            !previous.isLetterOrDigit() -> current.isLetterOrDigit()
            current.isUpperCase() -> !previous.isUpperCase() || name.getOrNull(index + 1)?.isLowerCase() == true
            current.isDigit() -> previous.isLetter()
            else -> false
        }
    }
}
