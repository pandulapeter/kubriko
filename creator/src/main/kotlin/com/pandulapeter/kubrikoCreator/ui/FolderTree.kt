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
    val allFolders = (folders + items.mapNotNull { (_, folder) -> folder.ifEmpty { null } }).withParents().filterTo(HashSet()) { it.isNotEmpty() }
    val foldersByParent = allFolders.groupBy(FolderPaths::parentOf).mapValues { (_, children) -> children.sortedBy { it.lowercase() } }
    val itemsByFolder = items.groupBy({ (_, folder) -> folder }, { (name, _) -> name }).mapValues { (_, names) -> names.sortedBy { it.lowercase() } }
    // A folder holds something when an item is in it, or it has a folder of its own.
    val nonEmptyFolders = HashSet<String>()
    itemsByFolder.keys.forEach { folder -> var current = folder; while (current.isNotEmpty() && nonEmptyFolders.add(current)) current = FolderPaths.parentOf(current) }
    allFolders.forEach { folder -> FolderPaths.parentOf(folder).takeIf { it.isNotEmpty() }?.let(nonEmptyFolders::add) }
    val rows = mutableListOf<FolderTreeRow>()
    fun addLevel(parent: String, depth: Int) {
        foldersByParent[parent].orEmpty().forEach { folder ->
            val isExpanded = folder !in collapsed
            rows += FolderTreeRow.Folder(folder, depth, isExpanded, isEmpty = folder !in nonEmptyFolders)
            if (isExpanded) addLevel(folder, depth + 1)
        }
        itemsByFolder[parent].orEmpty().forEach { name -> rows += FolderTreeRow.Item(name, depth) }
    }
    addLevel(parent = "", depth = 0)
    return rows
}
