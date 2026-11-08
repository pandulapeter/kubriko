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

import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageText

/**
 * The screenplay's page index a conversation starts with, after the digest: each page's name, title, and summary, so the
 * agent knows the game's design in its own words and which page to read. A screenplay too large for [LIMIT] characters
 * lists the pages at its top and its folders with how many pages each holds instead.
 */
internal object PageIndexText {
    const val LIMIT = 4_000
    private const val SUMMARY_LIMIT = 200

    /**
     * The index of [pages], each page's text by its name, or null when there are none.
     */
    fun of(pages: Map<String, String>): String? {
        if (pages.isEmpty()) return null
        val names = pages.keys.sortedWith(compareBy<String> { it != PageNaming.FIRST_PAGE }.thenBy { it.lowercase() })
        val lines = names.map { name -> lineOf(name, checkNotNull(pages[name])) }
        val full = lines.joinToString("\n")
        val body = if (full.length <= LIMIT) full else folded(names, pages)
        return "$HEADER\n$body"
    }

    private fun lineOf(name: String, text: String): String {
        val title = PageText.titleOf(text)
        val summary = PageText.summaryOf(text)?.let { if (it.length > SUMMARY_LIMIT) it.take(SUMMARY_LIMIT).trimEnd() + "…" else it }
        return "- $name" + title?.let { ": $it" }.orEmpty() + summary?.let { " — $it" }.orEmpty()
    }

    private fun folded(names: List<String>, pages: Map<String, String>): String {
        val top = names.filter { '/' !in it }.map { lineOf(it, checkNotNull(pages[it])) }
        val folders = names.filter { '/' in it }.groupingBy { it.substringBefore('/') }.eachCount().toSortedMap()
            .map { (folder, count) -> "- $folder/ ($count ${if (count == 1) "page" else "pages"})" }
        val text = (top + folders).joinToString("\n")
        return if (text.length <= LIMIT) text else (names.filter { '/' !in it }.map { "- $it" } + folders).joinToString("\n").take(LIMIT)
    }

    private const val HEADER = "The screenplay (the game's design in its own words, in screenplay/; read_pages reads pages):"
}
