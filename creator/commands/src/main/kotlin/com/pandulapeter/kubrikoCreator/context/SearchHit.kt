/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

/**
 * Where a search found its query, what it found there, and what of [text] matched.
 *
 * @param text The name, or the line it is in, shortened around the query when long.
 * @param ranges The characters of [text] that match, to highlight.
 * @param line The line the text is on (from 1), for code and Markdown.
 */
public data class SearchHit(
    val place: UsagePlace,
    val kind: Kind,
    val text: String,
    val ranges: List<IntRange>,
    val line: Int? = null,
) {
    /**
     * What a hit was found in, with the words the AI reads.
     */
    public enum class Kind(
        public val english: String,
    ) {
        NAME("name"),
        DESCRIPTION("description"),
        BLOCK_TEXT("Block text"),
        KOTLIN("Kotlin"),
        SCREEN_TEXT("screen text"),
        MARKDOWN("Markdown"),
    }

    override fun toString(): String = "$place" + (line?.let { ":$it" } ?: "") + " (${kind.english}): $text"
}
