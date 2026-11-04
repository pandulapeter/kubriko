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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which of the sidebar's modules, groups, and folders the user opened or closed, by their [SidebarKeys]; the rest are as
 * the [SidebarOutline] starts them. Kept by key rather than by row, so a folder keeps its state while the rows around it
 * come and go, until it is gone itself ([forgetAllBut]), so one made later under the same name starts afresh.
 */
@Stable
internal class SidebarExpansion {
    private var choices by mutableStateOf(emptyMap<String, Boolean>())

    fun isExpanded(outline: SidebarOutline, key: String) = choices[key] ?: outline.isExpandedByDefault(key)

    fun toggle(outline: SidebarOutline, key: String) = choose(outline.toggled(key, isExpanded(outline, key)))

    fun setAll(outline: SidebarOutline, key: String, isExpanded: Boolean) = choose(outline.allSet(key, isExpanded))

    /**
     * Opens [key] alone, leaving what is in it as it was, for showing a name being typed inside it.
     */
    fun expand(key: String) = choose(mapOf(key to true))

    /**
     * The paths of the folders of the group [groupKey] that were closed, which start open ([folderTreeRows]'s `collapsed`).
     */
    fun collapsedFolders(groupKey: String): Set<String> = foldersOf(groupKey) { _, isExpanded -> !isExpanded }

    /**
     * The keys of the Kotlin tree rows of the group [groupKey] that aren't as they start ([kotlinTreeRows]'s `toggled`).
     */
    fun toggledKotlinRows(outline: SidebarOutline, groupKey: String): Set<String> = foldersOf(groupKey) { key, isExpanded ->
        isExpanded != outline.isExpandedByDefault(key)
    }

    private inline fun foldersOf(groupKey: String, isIncluded: (key: String, isExpanded: Boolean) -> Boolean): Set<String> {
        val prefix = SidebarKeys.folder(groupKey, "")
        return choices.entries.mapNotNullTo(HashSet()) { (key, isExpanded) -> key.removePrefix(prefix).takeIf { key.startsWith(prefix) && isIncluded(key, isExpanded) } }
    }

    /**
     * Forgets the choices about what is no longer in [outline].
     */
    fun forgetAllBut(outline: SidebarOutline) {
        if (choices.keys.any { it !in outline }) choices = choices.filterKeys { it in outline }
    }

    private fun choose(changes: Map<String, Boolean>) {
        choices = choices + changes
    }
}
