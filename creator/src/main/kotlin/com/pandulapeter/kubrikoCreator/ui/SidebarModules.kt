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
 * The modules the sidebar shows, as a tree in the order it lists them: each inside the nearest one its folder is in
 * (`app/desktop` and `app/android` inside `app`, which is shown even without a build file of its own, as Gradle makes
 * it a project too, but never inside the folders above a build of its own, such as `gradle/build-logic`), and among
 * those at one level, the one holding the game's module first, then the root project, then the rest by name. The game's
 * module is the one Kubriko Creator generates code into, or in a project that isn't opted in, the [gameModule] chosen
 * in the toolbar (by its folder), or else the one it puts new Scenes in.
 *
 * Every module [layout] knows is one, and so is every module with files. Types go to the module of their code, Scenes to
 * every module that has a file of their name (one not scanned yet to [layout]'s Scene module, where it was made), and the
 * Scene folders [folderGroups] holds to that module, since they are those of its Scenes folder. New Scenes are made only
 * there, and only when it is the game's: Kubriko Creator's code loads them from it, or it uses Kubriko. In a project
 * that isn't opted in, the place the templates keep Scenes in may be a module nothing loads them from. A module that uses
 * Kubriko lists every group of what a game is made of, empty ones too, so it shows where things go; any other lists only
 * what it has (Kotlin files always in a project of one module), and one with nothing to show, holding none that has
 * something (a root project only gathering the others), isn't listed.
 */
internal fun sidebarModules(
    resources: Map<ResourceGroup, List<ProjectResource>>,
    folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    layout: ProjectLayout,
    gameModule: String? = null,
): List<SidebarModule> {
    val typeModule = layout.typeModule ?: layout.sceneModule
    val home = if (folderGroups.keys.any { it.kind != null }) typeModule else gameModule ?: layout.sceneModule
    val buildRoots = layout.modules.filterValues { it.isBuildRoot }.keys
    val modules = resources.values.flatten().map { it.module } + layout.modules.keys + layout.sceneModule + home
    val paths = modules.flatMapTo(HashSet()) { path -> listOf(path) + foldersAbove(path, buildRoots) }
    val parents = paths.associateWith { path -> if (path in buildRoots) null else foldersAbove(path, buildRoots).firstOrNull { it in paths } }
    val children = paths.groupBy { parents[it] }
    val order = compareBy<String>({ !(home == it || home.startsWith("$it/")) }, { it.isNotEmpty() }, { it.lowercase() }, { it })
    val ordered = ArrayList<String>()
    fun addLevel(parent: String?): Unit = children[parent].orEmpty().sortedWith(order).forEach { path ->
        ordered += path
        addLevel(path)
    }
    addLevel(null)
    val sceneModules = resources[ResourceGroup.SCENES].orEmpty().groupBy({ SceneStore.nameOf(it.file) }, { it.module })
    val built = ordered.map { path ->
        val usesKubriko = layout.modules[path]?.usesKubriko == true
        val isSceneHome = path == layout.sceneModule && (usesKubriko || layout.typeModule != null || layout.modules.isEmpty())
        val groups = folderGroups.mapNotNull { (group, state) ->
            when {
                group.kind != null -> state.takeIf { path == typeModule }
                else -> state.copy(
                    items = state.items.filter { (name, _) -> path in (sceneModules[name] ?: listOf(layout.sceneModule)) },
                    folders = if (path == layout.sceneModule) state.folders else emptySet(),
                ).takeIf { isSceneHome || usesKubriko || it.items.isNotEmpty() || it.folders.isNotEmpty() }
            }?.let { group to it }
        }.toMap()
        val moduleResources = resources.mapValues { (_, groupResources) -> groupResources.filter { it.module == path } }.filterValues { it.isNotEmpty() }
        SidebarModule(
            path = path,
            parent = parents[path],
            depth = generateSequence(parents[path]) { parents[it] }.count(),
            folderGroups = groups,
            resources = moduleResources,
            resourceGroups = if (usesKubriko) ResourceGroup.entries - ResourceGroup.SCENES else shownResourceGroups(moduleResources).filter { it in moduleResources },
            holdsGame = path == home || home.startsWith("$path/"),
            creatingGroups = groups.keys.filterTo(HashSet()) { group -> if (group.kind != null) path == typeModule else isSceneHome },
        )
    }
    val shown = HashSet<String>()
    built.asReversed().forEach { module ->
        if (module.folderGroups.isNotEmpty() || module.resourceGroups.isNotEmpty() || module.path in shown) {
            generateSequence(module.path) { parents[it] }.forEach(shown::add)
        }
    }
    val listed = built.filter { it.path in shown }.ifEmpty { built.take(1) }
    return listed.singleOrNull()?.let { only -> listOf(only.copy(resourceGroups = (shownResourceGroups(only.resources) + only.resourceGroups).distinct().sortedBy { it.ordinal })) } ?: listed
}

/**
 * [modules] narrowed to what matches [query] ([SidebarModule.filtered]), with the modules holding one that does kept
 * too, empty, so it is shown where it is.
 */
internal fun filteredModules(modules: List<SidebarModule>, query: String): List<SidebarModule> {
    val filtered = modules.associate { it.path to it.filtered(query) }
    val parents = modules.associate { it.path to it.parent }
    val kept = filtered.filterValues { it != null }.keys.flatMapTo(HashSet()) { path -> generateSequence(path) { parents[it] } }
    return modules.filter { it.path in kept }.map { module ->
        filtered[module.path] ?: module.copy(folderGroups = emptyMap(), resources = emptyMap(), resourceGroups = emptyList())
    }
}

/**
 * The folders above the module in [path] that show as modules holding it, nearest first: each of them (Gradle makes
 * `:app` a project for `:app:desktop`), up to the first that is a build of its own ([buildRoots]), whose part it is,
 * and none above a module that is such a build itself.
 */
private fun foldersAbove(path: String, buildRoots: Set<String>): List<String> {
    if (path in buildRoots) return emptyList()
    val folders = generateSequence(path) { it.substringBeforeLast('/', "") }.drop(1).takeWhile { it.isNotEmpty() }.toList()
    val buildRoot = folders.indexOfFirst { it in buildRoots }
    return if (buildRoot < 0) folders else folders.take(buildRoot + 1)
}
