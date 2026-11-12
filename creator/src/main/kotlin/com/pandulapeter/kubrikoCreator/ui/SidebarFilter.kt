/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.pandulapeter.kubrikoCreator.actors.withParents
import com.pandulapeter.kubrikoCreator.foundation.NameMatching
import com.pandulapeter.kubrikoCreator.ui.components.highlightedMatch
import com.pandulapeter.kubrikoCreator.ui.components.matchHighlightStyle

/**
 * Whether [name] (a type, Scene, or file name) matches [query]: its last part, after a path's folders, matches as names
 * do ([NameMatching], so `brk` finds `Brick`), or the whole contains the trimmed query, ignoring case. An empty query
 * matches everything.
 */
internal fun matchesSidebarFilter(name: String, query: String) =
    NameMatching.matches(name.substringAfterLast('/'), query) || name.contains(query.trim(), ignoreCase = true)

/**
 * What the sidebar's filter field holds, which the rows highlight in their names.
 */
internal val LocalSidebarFilter = compositionLocalOf { "" }

/**
 * This state narrowed to the items matching [query] by what [shownNameOf] makes of their names, with only the folders
 * that hold one (and their parents).
 */
internal fun FolderTreeGroupState.filtered(query: String, shownNameOf: (String) -> String = { it }): FolderTreeGroupState {
    val matchingItems = items.filter { (name, _) -> matchesSidebarFilter(shownNameOf(name), query) }
    return copy(
        items = matchingItems,
        folders = matchingItems.map { it.second }.filter(String::isNotEmpty).toSet().withParents(),
    )
}

/**
 * [text] with what the sidebar's filter ([LocalSidebarFilter]) matches in it highlighted, worked out again only when
 * either changes.
 */
@Composable
internal fun rememberFilterHighlighted(text: String): AnnotatedString {
    val query = LocalSidebarFilter.current
    val style = matchHighlightStyle()
    return remember(text, query, style) { if (query.isBlank()) AnnotatedString(text) else highlightedMatch(text, query, style) }
}
