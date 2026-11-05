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
 * What the sidebar's New menu makes: an item of a group with folders, or a document at the project's top.
 */
internal sealed interface SidebarNewItem {

    data class GroupItem(val group: FolderGroup) : SidebarNewItem

    data object Document : SidebarNewItem
}

/**
 * What the New menu offers for [modules]: an item of every group one of them is creating in, in the groups' order, then
 * a document, which can always be made.
 */
internal fun sidebarNewItems(modules: List<SidebarModule>): List<SidebarNewItem> =
    FolderGroup.entries.filter { group -> modules.any { group in it.creatingGroups } }.map(SidebarNewItem::GroupItem) + SidebarNewItem.Document

/**
 * Where choosing a [SidebarNewItem] names it: [naming] shown in the row of [revealKey], once the module bands in
 * [openedKeys] (the creating module's and those holding it) are open.
 */
internal class SidebarNewTarget(
    val naming: SidebarNaming,
    val revealKey: String,
    val openedKeys: List<String>,
)

/**
 * Where this is named among [modules]: an item in the group of the module creating in it, a document at the top of the
 * sidebar; null for a group no module creates in.
 */
internal fun SidebarNewItem.target(modules: List<SidebarModule>): SidebarNewTarget? = when (this) {
    is SidebarNewItem.GroupItem -> modules.firstOrNull { group in it.creatingGroups }?.let { module ->
        val parents = modules.associate { it.path to it.parent }
        SidebarNewTarget(
            naming = SidebarNaming.NewItem(group),
            revealKey = newItemKey(SidebarKeys.folderGroup(module.path, group)),
            openedKeys = generateSequence(module.path) { parents[it] }.map(SidebarKeys::module).toList(),
        )
    }
    SidebarNewItem.Document -> SidebarNewTarget(SidebarNaming.NewProjectDocument, NEW_PROJECT_DOCUMENT_KEY, emptyList())
}

/**
 * The key of the row naming a new item of the group [groupKey].
 */
internal fun newItemKey(groupKey: String) = "$groupKey-new"

/**
 * The key of the row naming a new document at the top of the sidebar.
 */
internal const val NEW_PROJECT_DOCUMENT_KEY = "sidebar-new-document"
