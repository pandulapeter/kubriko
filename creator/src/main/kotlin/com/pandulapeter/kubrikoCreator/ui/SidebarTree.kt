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

/**
 * How the sidebar's modules, groups, and folders are open now, and what opens and closes them: [expansion] over
 * [outline], except while [isFiltering], when everything is open and stays so.
 */
internal class SidebarTree(
    private val outline: SidebarOutline,
    private val expansion: SidebarExpansion,
    val isFiltering: Boolean,
) {
    fun isExpanded(key: String) = isFiltering || expansion.isExpanded(outline, key)

    fun toggle(key: String) {
        if (!isFiltering) expansion.toggle(outline, key)
    }

    fun expand(key: String) {
        if (!isFiltering) expansion.expand(key)
    }

    /**
     * What [key]'s row offers for every folder in it, or null when it holds none, or while filtering.
     */
    fun subfolderActions(key: String) = if (isFiltering || !outline.hasSubfolders(key)) {
        null
    } else {
        SubfolderActions(
            onExpandAll = { expansion.setAll(outline, key, isExpanded = true) },
            onCollapseAll = { expansion.setAll(outline, key, isExpanded = false) },
        )
    }

    fun followFolder(groupKey: String, path: String, newPath: String) = expansion.followFolder(groupKey, path, newPath)

    fun collapsedFolders(groupKey: String) = if (isFiltering) emptySet() else expansion.collapsedFolders(groupKey)

    fun toggledKotlinRows(groupKey: String) = if (isFiltering) emptySet() else expansion.toggledKotlinRows(outline, groupKey)
}
