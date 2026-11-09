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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class SidebarRowsTest {
    private val modules = sidebarModules(
        resources = emptyMap(),
        folderGroups = folderTreeGroups(
            types = listOf(ActorTypeDefinition(name = "Ball"), ActorTypeDefinition(name = "Bat", folder = "enemies")).map(SidebarType::of),
            typeFolders = emptyMap(),
            selectedTypeName = null,
            sceneNames = emptyList(),
            sceneFolders = emptySet(),
            openSceneName = null,
        ),
        layout = ProjectLayout(),
    )
    private val expansion = SidebarExpansion()
    private val outline = SidebarOutline(modules.flatMap { it.outlineNodes(isGrouped = false) })
    private val tree = SidebarTree(outline, expansion, isFiltering = false)
    private val module = modules.single()
    private val enemiesKey = SidebarKeys.folder(SidebarKeys.folderGroup(module.path, FolderGroup.ACTOR_TYPES), "enemies")

    private fun names(rows: List<FolderTreeRow>) = rows.map { row ->
        when (row) {
            is FolderTreeRow.Folder -> "${row.path}/"
            is FolderTreeRow.Item -> row.name
        }
    }

    private fun SidebarRows.actorTypes() = checkNotNull(groupOf(module, FolderGroup.ACTOR_TYPES, tree))

    @Test
    fun eachGroupsRowsFollowWhichFoldersAreOpen() {
        val rows = SidebarRows.of(modules, isGrouped = false, tree, previous = null)
        assertEquals(listOf("enemies/", "Bat", "Ball"), names(rows.actorTypes().rows))
        assertEquals("enemies", rows.actorTypes().folderOfItem["Bat"])
        tree.toggle(enemiesKey)
        assertEquals(listOf("enemies/", "Ball"), names(SidebarRows.of(modules, isGrouped = false, tree, previous = rows).actorTypes().rows))
    }

    @Test
    fun aTreeMadeOfTheSameKeepsItsRowsAndOnlyAChangedOneIsWorkedOutAgain() {
        val first = SidebarRows.of(modules, isGrouped = false, tree, previous = null)
        val selected = modules.map { module -> module.copy(folderGroups = module.folderGroups.mapValues { (_, state) -> state.copy(selectedName = "Ball", items = state.items.toList()) }) }
        val afterSelection = SidebarRows.of(selected, isGrouped = false, tree, previous = first)
        assertSame(first.actorTypes(), afterSelection.actorTypes(), "selecting an item works out no rows")
        val scenes = checkNotNull(first.groupOf(module, FolderGroup.SCENES, tree))
        tree.toggle(enemiesKey)
        val afterToggle = SidebarRows.of(modules, isGrouped = false, tree, previous = afterSelection)
        assertNotSame(first.actorTypes(), afterToggle.actorTypes())
        assertSame(scenes, afterToggle.groupOf(module, FolderGroup.SCENES, tree), "opening a folder works out only its own tree")
    }
}
