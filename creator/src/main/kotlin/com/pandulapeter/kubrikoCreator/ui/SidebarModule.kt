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
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup

/**
 * What the sidebar shows of one Gradle module of the project.
 *
 * @param path The module's folder, as [ProjectResource.module] names it.
 * @param parent The path of the module it is shown inside (`app` for `app/desktop`), or null for one at the top.
 * @param depth How many modules it is inside.
 * @param holdsGame Whether it is the module Kubriko Creator generates code into (or puts new Scenes in), or holds it,
 *   which is open at first while the others start closed, so a project of many modules opens on the game.
 * @param folderGroups Its groups with folders: the types in the module their code is generated into, and its Scenes.
 * @param resources Its files, by group.
 * @param resourceTrees Its sounds and music as trees of their folders ([resourceTreesOf]).
 * @param resourceGroups The groups of its files that aren't types or Scenes it lists ([shownResourceGroups]; in a project
 *   of several modules, Kotlin files only in a module that has some).
 * @param creatingGroups The groups new items and folders can be made in here: those whose files Kubriko Creator puts in
 *   this module. In another module's, what exists can be opened, renamed, and deleted, but nothing is added.
 * @param kotlinRootPackage The package every one of its Kotlin files is in ([rootPackageOf]).
 * @param documents The paths of the project's documents in its folder and not in a band of their own, listed after its
 *   groups, each plain folder of them a folder row.
 */
internal data class SidebarModule(
    val path: String,
    val parent: String? = null,
    val depth: Int = 0,
    val holdsGame: Boolean = false,
    val folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    val resources: Map<ResourceGroup, List<ProjectResource>>,
    val resourceTrees: Map<ResourceGroup, FolderTreeGroupState> = emptyMap(),
    val resourceGroups: List<ResourceGroup>,
    val creatingGroups: Set<FolderGroup>,
    val kotlinRootPackage: String = rootPackageOf(resources[ResourceGroup.KOTLIN].orEmpty()),
    val documents: List<String> = emptyList(),
) {
    /**
     * This module narrowed to what matches [query] ([matchesSidebarFilter]), or null when nothing in it does.
     */
    fun filtered(query: String): SidebarModule? {
        val shownGroups = folderGroups.mapValues { (_, state) -> state.filtered(query) }.filterValues { it.items.isNotEmpty() }
        val shownResources = resources.mapValues { (_, groupResources) -> groupResources.filter { matchesSidebarFilter(it.name, query) } }.filterValues { it.isNotEmpty() }
        val shownResourceGroups = resourceGroups.filter { it in shownResources }
        val shownDocuments = documents.filter { matchesSidebarFilter(it.substringAfterLast('/'), query) }
        return if (shownGroups.isEmpty() && shownResourceGroups.isEmpty() && shownDocuments.isEmpty()) {
            null
        } else {
            copy(
                folderGroups = shownGroups,
                resources = shownResources,
                resourceTrees = resourceTrees.mapValues { (_, state) -> state.filtered(query, FolderPaths::nameOf) },
                resourceGroups = shownResourceGroups,
                documents = shownDocuments,
            )
        }
    }

    fun withoutSelection() = copy(folderGroups = folderGroups.mapValues { (_, state) -> state.copy(selectedName = null) })

    /**
     * The [SidebarOutline] nodes of this module, under its own when [isGrouped] (the project has several modules), which
     * don't depend on what is selected.
     */
    fun outlineNodes(isGrouped: Boolean): List<SidebarOutline.Node> = buildList {
        val moduleKey = if (isGrouped) SidebarKeys.module(path).also { add(SidebarOutline.Node(it, parent = parent?.let(SidebarKeys::module), isFolder = false, isExpandedByDefault = holdsGame)) } else null
        folderGroups.forEach { (group, state) ->
            val groupKey = SidebarKeys.folderGroup(path, group)
            add(SidebarOutline.Node(groupKey, moduleKey, isFolder = false, isCollapsible = false))
            val folders = folderTreeRows(state.items, state.folders, collapsed = emptySet()).filterIsInstance<FolderTreeRow.Folder>()
            addFolders(groupKey, folders.map { OutlineFolder(it.path, it.depth, isExpandedByDefault = true) })
        }
        resourceGroups.forEach { group ->
            val groupKey = SidebarKeys.resourceGroup(path, group)
            add(SidebarOutline.Node(groupKey, moduleKey, isFolder = false, isExpandedByDefault = group != ResourceGroup.KOTLIN))
            resourceTrees[group]?.let { state ->
                val folders = folderTreeRows(state.items, state.folders, collapsed = emptySet()).trimmedTo(state.root).filterIsInstance<FolderTreeRow.Folder>()
                addFolders(groupKey, folders.map { OutlineFolder(it.path, it.depth, isExpandedByDefault = true) })
            }
            if (group == ResourceGroup.KOTLIN) {
                val rows = kotlinTreeRows(resources[group].orEmpty(), kotlinRootPackage, toggled = emptySet(), isEverythingOpen = true).filter { it !is KotlinTreeRow.File }
                addFolders(groupKey, rows.map { OutlineFolder(it.key, it.depth, isExpandedByDefault = it !is KotlinTreeRow.Container) })
            }
        }
        if (documents.isNotEmpty()) {
            val documentsKey = SidebarKeys.documents(path)
            add(SidebarOutline.Node(documentsKey, moduleKey, isFolder = false, isCollapsible = false))
            val folders = folderTreeRows(documentItems(), emptySet(), collapsed = emptySet()).filterIsInstance<FolderTreeRow.Folder>()
            addFolders(documentsKey, folders.map { OutlineFolder(it.path, it.depth, isExpandedByDefault = false) })
        }
    }

    /**
     * Its [documents], each with the folder it is in below this module's folder (empty for none), as [folderTreeRows]
     * takes them.
     */
    fun documentItems(): List<Pair<String, String>> = documents.map { document -> document to documentFolderOf(document) }

    /**
     * The folder the document at [document] is in below this module's folder, or an empty string for none.
     */
    fun documentFolderOf(document: String): String = document.removePrefix(if (path.isEmpty()) "" else "$path/").substringBeforeLast('/', "")

    /**
     * The folder at [folder] below this module's folder, from the project's root.
     */
    fun documentFolderPath(folder: String): String = listOf(path, folder).filter { it.isNotEmpty() }.joinToString("/")

    /**
     * Adds the [folders] of the group [groupKey], given in the order a tree lists them, each under the nearest one before
     * it that is a level higher.
     */
    private fun MutableList<SidebarOutline.Node>.addFolders(groupKey: String, folders: List<OutlineFolder>) {
        val parents = ArrayList<String>()
        folders.forEach { folder ->
            val key = SidebarKeys.folder(groupKey, folder.path)
            while (parents.size > folder.depth) parents.removeAt(parents.lastIndex)
            add(SidebarOutline.Node(key, parents.lastOrNull() ?: groupKey, isFolder = true, isExpandedByDefault = folder.isExpandedByDefault))
            parents += key
        }
    }

    /**
     * A folder row of one of the module's trees, by the path its key is built from.
     */
    private class OutlineFolder(val path: String, val depth: Int, val isExpandedByDefault: Boolean)
}
