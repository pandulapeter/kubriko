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
    private val module = modules.single().path

    private fun names(rows: List<FolderTreeRow>) = rows.map { row ->
        when (row) {
            is FolderTreeRow.Folder -> "${row.path}/"
            is FolderTreeRow.Item -> row.name
        }
    }

    @Test
    fun eachGroupsRowsFollowWhichFoldersAreOpen() {
        val rows = SidebarRows.of(modules, tree)
        assertEquals(listOf("enemies/", "Bat", "Ball"), names(checkNotNull(rows.of(module, FolderGroup.ACTOR_TYPES)).rows))
        assertEquals("enemies", rows.of(module, FolderGroup.ACTOR_TYPES)?.folderOfItem?.get("Bat"))
        tree.toggle(SidebarKeys.folder(SidebarKeys.folderGroup(module, FolderGroup.ACTOR_TYPES), "enemies"))
        assertEquals(listOf("enemies/", "Ball"), names(checkNotNull(SidebarRows.of(modules, tree).of(module, FolderGroup.ACTOR_TYPES)).rows))
    }
}
