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
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ResourceGroup

/**
 * The rows of the sidebar's trees: each group's of each module shown with its contents (its sprites', sounds', and music's too),
 * each one's documents, and its Kotlin files', worked out outside the sidebar's list, so that building the list again (which a selection or an AI
 * step's flash does) works none of them out. Each tree keeps the rows of the one before when what it is made of is
 * equal, so opening one folder or selecting an item works out no other tree again.
 */
internal class SidebarRows private constructor(
    private val trees: Map<String, Tree<*>>,
) {
    /**
     * The rows of one tree, with what they were made of.
     */
    class Tree<T>(
        val inputs: List<Any?>,
        val rows: List<T>,
        val folderOfItem: Map<String, String> = emptyMap(),
    )

    /**
     * The rows of [group] in [module], made then and there if they weren't worked out beforehand.
     */
    fun groupOf(module: SidebarModule, group: FolderGroup, tree: SidebarTree): Tree<FolderTreeRow>? = treeAt(SidebarKeys.folderGroup(module.path, group))
        ?: module.folderGroups[group]?.let { groupTree(it, tree.collapsedFolders(SidebarKeys.folderGroup(module.path, group)), previous = null) }

    /**
     * The rows of the sprites, sounds, or music [group] of [module], made then and there if they weren't worked out beforehand.
     */
    fun resourceTreeOf(module: SidebarModule, group: ResourceGroup, tree: SidebarTree): Tree<FolderTreeRow>? = treeAt(SidebarKeys.resourceGroup(module.path, group))
        ?: module.resourceTrees[group]?.let { groupTree(it, tree.collapsedFolders(SidebarKeys.resourceGroup(module.path, group)), previous = null) }

    fun documentsOf(module: SidebarModule, tree: SidebarTree): List<FolderTreeRow> = treeAt<FolderTreeRow>(SidebarKeys.documents(module.path))?.rows
        ?: documentsTree(module, tree, previous = null).rows

    fun kotlinOf(module: SidebarModule, tree: SidebarTree): List<KotlinTreeRow> = treeAt<KotlinTreeRow>(SidebarKeys.resourceGroup(module.path, ResourceGroup.KOTLIN))?.rows
        ?: kotlinTree(module, tree, previous = null).rows

    @Suppress("UNCHECKED_CAST")
    private fun <T> treeAt(key: String) = trees[key] as Tree<T>?

    companion object {

        /**
         * The rows of the trees of [modules] that the sidebar shows open (every one unless [isGrouped], otherwise those
         * whose module band and every band above it are open), as [tree] opens their folders, reusing [previous]'s where
         * they are made of the same.
         */
        fun of(modules: List<SidebarModule>, isGrouped: Boolean, tree: SidebarTree, previous: SidebarRows?): SidebarRows {
            val trees = HashMap<String, Tree<*>>()
            modulesShowingContents(modules, isGrouped, tree).forEach { module ->
                module.folderGroups.forEach { (group, state) ->
                    val key = SidebarKeys.folderGroup(module.path, group)
                    trees[key] = groupTree(state, tree.collapsedFolders(key), previous?.trees?.get(key))
                }
                module.resourceTrees.forEach { (group, state) ->
                    val key = SidebarKeys.resourceGroup(module.path, group)
                    if (group in module.resourceGroups) trees[key] = groupTree(state, tree.collapsedFolders(key), previous?.trees?.get(key))
                }
                if (module.documents.isNotEmpty()) {
                    trees[SidebarKeys.documents(module.path)] = documentsTree(module, tree, previous?.trees?.get(SidebarKeys.documents(module.path)))
                }
                val kotlinKey = SidebarKeys.resourceGroup(module.path, ResourceGroup.KOTLIN)
                if (tree.isExpanded(kotlinKey) && !module.resources[ResourceGroup.KOTLIN].isNullOrEmpty()) {
                    trees[kotlinKey] = kotlinTree(module, tree, previous?.trees?.get(kotlinKey))
                }
            }
            return SidebarRows(trees)
        }

        /**
         * The modules whose contents the sidebar shows, as its list goes through them.
         */
        private fun modulesShowingContents(modules: List<SidebarModule>, isGrouped: Boolean, tree: SidebarTree): List<SidebarModule> {
            if (!isGrouped) return modules
            val open = HashSet<String>()
            return modules.filter { module ->
                (module.parent == null || module.parent in open) && tree.isExpanded(SidebarKeys.module(module.path)) && open.add(module.path)
            }
        }

        private fun <T> reused(previous: Tree<*>?, inputs: List<Any?>, make: () -> Tree<T>): Tree<T> {
            @Suppress("UNCHECKED_CAST")
            return if (previous != null && previous.inputs == inputs) previous as Tree<T> else make()
        }

        private fun groupTree(state: FolderTreeGroupState, collapsed: Set<String>, previous: Tree<*>?): Tree<FolderTreeRow> {
            val inputs = listOf(state.items, state.folders, state.root, collapsed)
            return reused(previous, inputs) { Tree(inputs, folderTreeRows(state.items, state.folders, collapsed).trimmedTo(state.root), state.items.toMap()) }
        }

        private fun documentsTree(module: SidebarModule, tree: SidebarTree, previous: Tree<*>?): Tree<FolderTreeRow> {
            val documentsKey = SidebarKeys.documents(module.path)
            val items = module.documentItems()
            val collapsed = items.mapNotNullTo(HashSet()) { (_, folder) -> folder.ifEmpty { null } }.withParents()
                .filterTo(HashSet()) { !tree.isExpanded(SidebarKeys.folder(documentsKey, it)) }
            val inputs = listOf(items, collapsed)
            return reused(previous, inputs) { Tree(inputs, folderTreeRows(items, emptySet(), collapsed)) }
        }

        private fun kotlinTree(module: SidebarModule, tree: SidebarTree, previous: Tree<*>?): Tree<KotlinTreeRow> {
            val groupKey = SidebarKeys.resourceGroup(module.path, ResourceGroup.KOTLIN)
            val resources = module.resources[ResourceGroup.KOTLIN].orEmpty()
            val toggled = tree.toggledKotlinRows(groupKey)
            val inputs = listOf(resources, module.kotlinRootPackage, toggled, tree.isFiltering)
            return reused(previous, inputs) { Tree(inputs, kotlinTreeRows(resources, module.kotlinRootPackage, toggled, isEverythingOpen = tree.isFiltering)) }
        }
    }
}
