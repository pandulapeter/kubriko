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

import com.pandulapeter.kubrikoCreator.actors.withParents

/**
 * Whether [name] (a type, Scene, or file name; for a path, its last part counts as well as the whole) matches [query]: it
 * contains the trimmed query, ignoring case. An empty query matches everything.
 */
internal fun matchesSidebarFilter(name: String, query: String) = name.contains(query.trim(), ignoreCase = true)

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
