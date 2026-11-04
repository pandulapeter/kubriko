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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SidebarModuleTest {
    private fun resource(group: ResourceGroup, path: String, module: String, kotlinPackage: String? = null) = ProjectResource(group, File(path), path, kotlinPackage, module)

    private val resources = listOf(
        resource(ResourceGroup.SCENES, "game/src/commonMain/composeResources/files/scenes/levels/1-1.json", "game"),
        resource(ResourceGroup.SCENES, "tools/src/commonMain/composeResources/files/scenes/sandbox.json", "tools"),
        resource(ResourceGroup.SPRITES, "game/src/commonMain/composeResources/drawable/hero.png", "game"),
        resource(ResourceGroup.SPRITES, "tools/src/commonMain/composeResources/drawable/grid.png", "tools"),
        resource(ResourceGroup.KOTLIN, "game/src/commonMain/kotlin/com/game/Game.kt", "game", "com.game"),
        resource(ResourceGroup.KOTLIN, "androidApp/src/main/kotlin/com/game/MainActivity.kt", "androidApp", "com.game"),
    ).groupBy { it.group }
    private val folderGroups = folderTreeGroups(
        types = listOf(ActorTypeDefinition(name = "Ball")),
        typeFolders = emptyMap(),
        selectedTypeName = null,
        sceneNames = listOf("levels/1-1", "sandbox", "fresh"),
        sceneFolders = setOf("levels", "worlds"),
        openSceneName = null,
    )
    private val modules = sidebarModules(resources, folderGroups, ProjectLayout(typeModule = "game", sceneModule = "game"))

    @Test
    fun theModuleCreatorWritesToComesFirstThenTheRestByPath() {
        assertEquals(listOf("game", "androidApp", "tools"), modules.map { it.path })
    }

    @Test
    fun typesAndNewScenesAreInCreatorsModuleAndEveryOtherSceneInItsOwn() {
        val (game, android, tools) = modules
        assertEquals(setOf(FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES, FolderGroup.SCENES), game.folderGroups.keys)
        assertEquals(game.folderGroups.keys, game.creatingGroups)
        assertEquals(listOf("levels/1-1" to "levels", "fresh" to ""), game.folderGroups.getValue(FolderGroup.SCENES).items)
        assertEquals(setOf("levels", "worlds"), game.folderGroups.getValue(FolderGroup.SCENES).folders)
        assertEquals(emptyMap(), android.folderGroups, "A module without Scenes has no Scenes group")
        assertEquals(listOf("sandbox" to ""), tools.folderGroups.getValue(FolderGroup.SCENES).items)
        assertEquals(emptySet(), tools.folderGroups.getValue(FolderGroup.SCENES).folders)
        assertEquals(emptySet(), tools.creatingGroups)
    }

    @Test
    fun eachModuleHasItsOwnFilesAndRootPackage() {
        val (game, android, tools) = modules
        assertEquals(listOf("hero.png"), game.resources.getValue(ResourceGroup.SPRITES).map { it.name })
        assertEquals(listOf("grid.png"), tools.resources.getValue(ResourceGroup.SPRITES).map { it.name })
        assertEquals(setOf(ResourceGroup.KOTLIN), android.resources.keys)
        assertEquals("com.game", android.kotlinRootPackage)
        assertEquals(listOf(ResourceGroup.SPRITES), tools.resourceGroups, "A module without Kotlin files lists none")
        assertEquals(listOf(ResourceGroup.KOTLIN), sidebarModules(emptyMap(), emptyMap(), ProjectLayout()).single().resourceGroups, "A project of one lists them always")
    }

    @Test
    fun aProjectOfOneModuleIsOneModuleEvenWithoutALayout() {
        val single = sidebarModules(mapValuesTo(""), folderGroups, ProjectLayout())
        assertEquals(listOf(""), single.map { it.path })
        assertEquals(listOf("levels/1-1", "sandbox", "fresh"), single.single().folderGroups.getValue(FolderGroup.SCENES).items.map { it.first })
    }

    @Test
    fun filteringKeepsOnlyWhatMatchesAndDropsAModuleWithNothing() {
        val (game, android, tools) = modules
        assertEquals(listOf("hero.png"), game.filtered("hero")?.resources?.values?.flatten()?.map { it.name })
        assertEquals(emptySet(), game.filtered("hero")?.folderGroups?.keys)
        assertNull(android.filtered("hero"))
        assertEquals(listOf("sandbox" to ""), tools.filtered("sand")?.folderGroups?.getValue(FolderGroup.SCENES)?.items)
        assertNull(game.filtered("json"), "Scene files are shown by their Scene names only")
    }

    @Test
    fun aSceneNameTwoModulesHaveIsShownInBoth() {
        val sameName = resources + (ResourceGroup.SCENES to resources.getValue(ResourceGroup.SCENES) + resource(ResourceGroup.SCENES, "tools/src/commonMain/composeResources/files/scenes/levels/1-1.json", "tools"))
        val tools = sidebarModules(sameName, folderGroups, ProjectLayout(typeModule = "game", sceneModule = "game")).last()
        assertEquals(listOf("levels/1-1", "sandbox"), tools.folderGroups.getValue(FolderGroup.SCENES).items.map { it.first })
    }

    @Test
    fun theOutlineHangsEveryModulesGroupsAndFoldersUnderItWhenThereAreSeveral() {
        val game = modules.first()
        val nodes = game.outlineNodes(isGrouped = true).associateBy { it.key }
        val scenes = SidebarKeys.folderGroup("game", FolderGroup.SCENES)
        val kotlin = SidebarKeys.resourceGroup("game", ResourceGroup.KOTLIN)
        assertEquals(SidebarKeys.module("game"), nodes.getValue(scenes).parent)
        assertEquals(scenes, nodes.getValue(SidebarKeys.folder(scenes, "levels")).parent)
        assertEquals(false, nodes.getValue(kotlin).isExpandedByDefault)
        assertNull(game.outlineNodes(isGrouped = false).first { it.key == scenes }.parent)
    }

    private fun mapValuesTo(module: String) = resources.mapValues { (_, list) -> list.map { it.copy(module = module) } }
}
