/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

/**
 * Which pages mention each file of the project, read from their links and images ([PageLinks]).
 *
 * @param pagesByPath The names of the pages mentioning each file, by its path from the project's root, in order.
 */
public class MentionIndex(
    private val pagesByPath: Map<String, List<String>> = emptyMap(),
) {
    /**
     * The names of the pages that mention the file at [path], a path from the project's root, the first page first, then
     * by name; a page doesn't count as mentioning itself.
     */
    public fun pagesMentioning(path: String): List<String> = pagesByPath[path].orEmpty()

    public companion object {

        /**
         * The index of [pages], each page's text by its name.
         */
        public fun of(pages: Map<String, String>): MentionIndex {
            val index = HashMap<String, MutableList<String>>()
            pages.keys.sortedWith(compareBy<String> { it != PageNaming.FIRST_PAGE }.thenBy { it.lowercase() }).forEach { name ->
                val pagePath = PageNaming.pathOf(name)
                PageLinks.linksOf(checkNotNull(pages[name])).mapNotNullTo(LinkedHashSet()) { PageLinks.resolve(pagePath, it.destination) }
                    .filter { it != pagePath }
                    .forEach { path -> index.getOrPut(path) { ArrayList() } += name }
            }
            return MentionIndex(index)
        }
    }
}
