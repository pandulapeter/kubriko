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

import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.withParents

/**
 * A row of a sidebar group shown as a tree.
 */
internal sealed interface FolderTreeRow {
    val depth: Int

    /**
     * @param path The folder's path (`enemies/flying`).
     * @param isEmpty Whether nothing is in it, not even another folder.
     */
    data class Folder(
        val path: String,
        override val depth: Int,
        val isExpanded: Boolean,
        val isEmpty: Boolean,
    ) : FolderTreeRow {
        val name get() = FolderPaths.nameOf(path)
    }

    data class Item(
        val name: String,
        override val depth: Int,
    ) : FolderTreeRow
}

/**
 * The rows of a group whose [items] (each a name and the path of the folder it is in, empty for none) are in [folders]:
 * at every level, folders first, then items, each sorted by name, and nothing inside a folder in [collapsed].
 */
internal fun folderTreeRows(items: List<Pair<String, String>>, folders: Set<String>, collapsed: Set<String>): List<FolderTreeRow> {
    val allFolders = (folders + items.mapNotNull { (_, folder) -> folder.ifEmpty { null } }).withParents()
    val rows = mutableListOf<FolderTreeRow>()
    fun addLevel(parent: String, depth: Int) {
        allFolders.filter { it.isNotEmpty() && FolderPaths.parentOf(it) == parent }.sortedBy { it.lowercase() }.forEach { folder ->
            val isExpanded = folder !in collapsed
            val isEmpty = items.none { (_, itemFolder) -> FolderPaths.isWithin(itemFolder, folder) } && allFolders.none { it != folder && FolderPaths.isWithin(it, folder) }
            rows += FolderTreeRow.Folder(folder, depth, isExpanded, isEmpty)
            if (isExpanded) addLevel(folder, depth + 1)
        }
        items.filter { (_, folder) -> folder == parent }.sortedBy { (name, _) -> name.lowercase() }.forEach { (name, _) -> rows += FolderTreeRow.Item(name, depth) }
    }
    addLevel(parent = "", depth = 0)
    return rows
}
