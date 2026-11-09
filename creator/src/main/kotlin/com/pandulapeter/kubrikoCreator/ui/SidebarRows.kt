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

import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup

/**
 * The rows of the sidebar's folder trees: each group's of each module, and each module's documents. Worked out together
 * from the modules (without what is selected) and which folders are open, so that building the sidebar's list again,
 * which a selection or an AI step's flash does, works none of them out again.
 */
internal class SidebarRows private constructor(
    private val groups: Map<GroupKey, GroupRows>,
    private val documents: Map<String, List<FolderTreeRow>>,
) {
    /**
     * The rows of [group] in the module at [modulePath], with the folder of each item.
     */
    class GroupRows(
        val rows: List<FolderTreeRow>,
        val folderOfItem: Map<String, String>,
    )

    private data class GroupKey(val modulePath: String, val group: FolderGroup)

    fun of(modulePath: String, group: FolderGroup): GroupRows? = groups[GroupKey(modulePath, group)]

    fun documentsOf(modulePath: String): List<FolderTreeRow> = documents[modulePath].orEmpty()

    companion object {

        /**
         * The rows of [modules], as [tree] opens their folders.
         */
        fun of(modules: List<SidebarModule>, tree: SidebarTree) = SidebarRows(
            groups = buildMap {
                modules.forEach { module ->
                    module.folderGroups.forEach { (group, state) ->
                        val collapsed = tree.collapsedFolders(SidebarKeys.folderGroup(module.path, group))
                        put(GroupKey(module.path, group), GroupRows(folderTreeRows(state.items, state.folders, collapsed), state.items.toMap()))
                    }
                }
            },
            documents = modules.filter { it.documents.isNotEmpty() }.associate { module ->
                val documentsKey = SidebarKeys.documents(module.path)
                val items = module.documentItems()
                val collapsed = folderTreeRows(items, emptySet(), collapsed = emptySet())
                    .filterIsInstance<FolderTreeRow.Folder>()
                    .mapNotNullTo(HashSet()) { folder -> folder.path.takeIf { !tree.isExpanded(SidebarKeys.folder(documentsKey, it)) } }
                module.path to folderTreeRows(items, emptySet(), collapsed)
            },
        )
    }
}
