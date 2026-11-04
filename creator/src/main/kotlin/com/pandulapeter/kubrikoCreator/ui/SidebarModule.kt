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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout

/**
 * What the sidebar shows of one Gradle module of the project.
 *
 * @param path The module's folder, as [ProjectResource.module] names it.
 * @param folderGroups Its groups with folders: the types in the module their code is generated into, and its Scenes.
 * @param resources Its files, by group.
 * @param resourceGroups The groups of its files that aren't types or Scenes it lists ([shownResourceGroups]; in a project
 *   of several modules, Kotlin files only in a module that has some).
 * @param creatingGroups The groups new items and folders can be made in here: those whose files Kubriko Creator puts in
 *   this module. In another module's, what exists can be opened, renamed, and deleted, but nothing is added.
 * @param kotlinRootPackage The package every one of its Kotlin files is in ([rootPackageOf]).
 */
internal data class SidebarModule(
    val path: String,
    val folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    val resources: Map<ResourceGroup, List<ProjectResource>>,
    val resourceGroups: List<ResourceGroup>,
    val creatingGroups: Set<FolderGroup>,
    val kotlinRootPackage: String = rootPackageOf(resources[ResourceGroup.KOTLIN].orEmpty()),
) {
    /**
     * This module narrowed to what matches [query] ([matchesSidebarFilter]), or null when nothing in it does.
     */
    fun filtered(query: String): SidebarModule? {
        val shownGroups = folderGroups.mapValues { (_, state) -> state.filtered(query) }.filterValues { it.items.isNotEmpty() }
        val shownResources = resources.mapValues { (_, groupResources) -> groupResources.filter { matchesSidebarFilter(it.name, query) } }.filterValues { it.isNotEmpty() }
        val shownResourceGroups = resourceGroups.filter { it in shownResources }
        return if (shownGroups.isEmpty() && shownResourceGroups.isEmpty()) null else copy(folderGroups = shownGroups, resources = shownResources, resourceGroups = shownResourceGroups)
    }

    fun withoutSelection() = copy(folderGroups = folderGroups.mapValues { (_, state) -> state.copy(selectedName = null) })

    /**
     * The [SidebarOutline] nodes of this module, under its own when [isGrouped] (the project has several modules), which
     * don't depend on what is selected.
     */
    fun outlineNodes(isGrouped: Boolean): List<SidebarOutline.Node> = buildList {
        val moduleKey = if (isGrouped) SidebarKeys.module(path).also { add(SidebarOutline.Node(it, parent = null, isFolder = false)) } else null
        folderGroups.forEach { (group, state) ->
            val groupKey = SidebarKeys.folderGroup(path, group)
            add(SidebarOutline.Node(groupKey, moduleKey, isFolder = false, isCollapsible = false))
            val folders = folderTreeRows(state.items, state.folders, collapsed = emptySet()).filterIsInstance<FolderTreeRow.Folder>()
            addFolders(groupKey, folders.map { OutlineFolder(it.path, it.depth, isExpandedByDefault = true) })
        }
        resourceGroups.forEach { group ->
            val groupKey = SidebarKeys.resourceGroup(path, group)
            add(SidebarOutline.Node(groupKey, moduleKey, isFolder = false, isExpandedByDefault = group != ResourceGroup.KOTLIN))
            if (group == ResourceGroup.KOTLIN) {
                val rows = kotlinTreeRows(resources[group].orEmpty(), kotlinRootPackage, toggled = emptySet(), isEverythingOpen = true).filter { it !is KotlinTreeRow.File }
                addFolders(groupKey, rows.map { OutlineFolder(it.key, it.depth, isExpandedByDefault = it !is KotlinTreeRow.Container) })
            }
        }
    }

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

/**
 * The modules the sidebar shows, the one Kubriko Creator generates code into (or, without block-built types, puts new
 * Scenes in) first, then the rest by path. Every module with files shown is one, and so are those [layout] names. Types
 * go to the module of their code, Scenes to every module that has a file of their name (one not scanned yet to
 * [layout]'s Scene module, where it was made), and the Scene folders [folderGroups] holds to that module, since they are
 * those of its Scenes folder.
 */
internal fun sidebarModules(
    resources: Map<ResourceGroup, List<ProjectResource>>,
    folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    layout: ProjectLayout,
): List<SidebarModule> {
    val typeModule = layout.typeModule ?: layout.sceneModule
    val sceneModules = resources[ResourceGroup.SCENES].orEmpty().groupBy({ SceneStore.nameOf(it.file) }, { it.module })
    val hasTypes = folderGroups.keys.any { it.kind != null }
    val home = if (hasTypes) typeModule else layout.sceneModule
    val paths = (resources.values.flatten().map { it.module } + layout.sceneModule + home).toSortedSet()
    return (listOf(home) + (paths - home)).map { path ->
        val groups = folderGroups.mapNotNull { (group, state) ->
            when {
                group.kind != null -> state.takeIf { path == typeModule }
                else -> state.copy(
                    items = state.items.filter { (name, _) -> path in (sceneModules[name] ?: listOf(layout.sceneModule)) },
                    folders = if (path == layout.sceneModule) state.folders else emptySet(),
                ).takeIf { path == layout.sceneModule || it.items.isNotEmpty() }
            }?.let { group to it }
        }.toMap()
        val moduleResources = resources.mapValues { (_, groupResources) -> groupResources.filter { it.module == path } }.filterValues { it.isNotEmpty() }
        SidebarModule(
            path = path,
            folderGroups = groups,
            resources = moduleResources,
            resourceGroups = shownResourceGroups(moduleResources).filter { it != ResourceGroup.KOTLIN || paths.size == 1 || it in moduleResources },
            creatingGroups = groups.keys.filterTo(HashSet()) { group -> if (group.kind != null) path == typeModule else path == layout.sceneModule },
        )
    }
}
