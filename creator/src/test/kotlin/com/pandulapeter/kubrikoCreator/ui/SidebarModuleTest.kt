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
import com.pandulapeter.kubrikoCreator.project.ProjectModule
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
        assertEquals(setOf(FolderGroup.PAGES, FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES, FolderGroup.SCENES), game.folderGroups.keys)
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

    @Test
    fun modulesAreNestedInsideTheNearestModuleTheirFolderIsInWithTheGamesBranchFirst() {
        fun kotlin(module: String) = resource(ResourceGroup.KOTLIN, "$module/src/main/kotlin/Main.kt", module, "")
        val nested = sidebarModules(
            resources = mapOf(ResourceGroup.KOTLIN to listOf("", "app/desktop", "tools/editor/model").map(::kotlin)),
            folderGroups = emptyMap(),
            layout = ProjectLayout(sceneModule = "app/shared", modules = modulesOf("" to false, "app/desktop" to false, "app/shared" to true, "tools/editor/model" to false, "tools/editor" to false)),
        )
        assertEquals(listOf("app", "app/shared", "app/desktop", "", "tools", "tools/editor", "tools/editor/model"), nested.map { it.path })
        assertEquals(listOf(null, "app", "app", null, null, "tools", "tools/editor"), nested.map { it.parent })
        assertEquals(listOf(0, 1, 1, 0, 0, 1, 2), nested.map { it.depth })
    }

    @Test
    fun aModuleWithNothingToShowHoldingNoneThatHasIsLeftOut() {
        val modules = sidebarModules(
            resources = emptyMap(),
            folderGroups = emptyMap(),
            layout = ProjectLayout(sceneModule = "game", modules = modulesOf("" to false, "game" to true, "tools/editor" to false, "tools/editor/model" to false)),
        )
        assertEquals(listOf("game"), modules.map { it.path })
        assertEquals(listOf(""), sidebarModules(emptyMap(), emptyMap(), ProjectLayout(modules = modulesOf("" to false))).map { it.path }, "A project shows its only module even when empty")
    }

    @Test
    fun aModuleThatUsesKubrikoListsEveryGroupEmptyOnesTooAndAnyOtherOnlyWhatItHas() {
        val withKubriko = sidebarModules(resources, folderGroups, ProjectLayout(typeModule = "game", sceneModule = "game", modules = modulesOf("game" to true, "tools" to true, "androidApp" to false)))
        val (game, android, tools) = withKubriko
        assertEquals(listOf(ResourceGroup.SPRITES, ResourceGroup.SOUNDS, ResourceGroup.MUSIC, ResourceGroup.KOTLIN), game.resourceGroups)
        assertEquals(game.resourceGroups, tools.resourceGroups)
        assertEquals(setOf(FolderGroup.SCENES), tools.folderGroups.keys)
        assertEquals(listOf(ResourceGroup.KOTLIN), android.resourceGroups)
        assertEquals(emptyMap(), android.folderGroups)
        val emptyKubrikoModule = sidebarModules(resources, folderGroups, ProjectLayout(typeModule = "game", sceneModule = "game", modules = modulesOf("game" to true, "levels" to true))).first { it.path == "levels" }
        assertEquals(listOf<Pair<String, String>>(), emptyKubrikoModule.folderGroups.getValue(FolderGroup.SCENES).items)
        assertEquals(emptySet(), emptyKubrikoModule.creatingGroups)
    }

    @Test
    fun filteringKeepsTheModulesHoldingOneThatMatchesSoItIsShownWhereItIs() {
        val nested = sidebarModules(
            resources = mapOf(ResourceGroup.KOTLIN to listOf(resource(ResourceGroup.KOTLIN, "app/desktop/src/main/kotlin/Main.kt", "app/desktop", ""))),
            folderGroups = emptyMap(),
            layout = ProjectLayout(sceneModule = "app/desktop", modules = modulesOf("app" to true, "app/desktop" to false, "tools" to false)),
        )
        val filtered = filteredModules(nested, "main")
        assertEquals(listOf("app", "app/desktop"), filtered.map { it.path })
        assertEquals(emptyList(), filtered.first().resourceGroups, "The module holding the match shows only its band")
        assertEquals(emptyList(), filteredModules(nested, "zzz"))
    }

    @Test
    fun aBuildOfItsOwnIsShownAtTheTopRatherThanInsideTheFoldersAboveIt() {
        val modules = sidebarModules(
            resources = mapOf(ResourceGroup.KOTLIN to listOf(resource(ResourceGroup.KOTLIN, "gradle/build-logic/convention/src/main/kotlin/Plugin.kt", "gradle/build-logic/convention", ""))),
            folderGroups = emptyMap(),
            layout = ProjectLayout(
                sceneModule = "game",
                modules = mapOf(
                    "game" to ProjectModule(usesKubriko = true),
                    "gradle/build-logic" to ProjectModule(usesKubriko = false, isBuildRoot = true),
                    "gradle/build-logic/convention" to ProjectModule(usesKubriko = false),
                ),
            ),
        )
        assertEquals(listOf("game" to null, "gradle/build-logic" to null, "gradle/build-logic/convention" to "gradle/build-logic"), modules.map { it.path to it.parent })
    }

    @Test
    fun theOnlyModuleLeftOnceTheEmptyOnesAreOutListsKotlinFilesAsAProjectOfOneAlwaysDoes() {
        val only = sidebarModules(
            resources = mapOf(ResourceGroup.SPRITES to listOf(resource(ResourceGroup.SPRITES, "game/src/commonMain/composeResources/drawable/hero.png", "game"))),
            folderGroups = emptyMap(),
            layout = ProjectLayout(sceneModule = "game", modules = modulesOf("" to false, "game" to false)),
        ).single()
        assertEquals(listOf(ResourceGroup.SPRITES, ResourceGroup.KOTLIN), only.resourceGroups)
    }

    @Test
    fun onlyTheGamesBranchStartsOpenInAProjectOfSeveralModules() {
        val modules = sidebarModules(
            resources = mapOf(ResourceGroup.KOTLIN to listOf("app/shared", "app/desktop", "tools").map { resource(ResourceGroup.KOTLIN, "$it/src/main/kotlin/Main.kt", it, "") }),
            folderGroups = emptyMap(),
            layout = ProjectLayout(sceneModule = "app/shared", modules = modulesOf("app/shared" to true, "app/desktop" to false, "tools" to false)),
        )
        assertEquals(mapOf("app" to true, "app/shared" to true, "app/desktop" to false, "tools" to false), modules.associate { it.path to it.holdsGame })
        val outline = SidebarOutline(modules.flatMap { it.outlineNodes(isGrouped = true) })
        assertEquals(listOf(true, true, false, false), modules.map { outline.isExpandedByDefault(SidebarKeys.module(it.path)) })
    }

    @Test
    fun aModuleThatDoesntUseKubrikoGetsNoScenesJustBecauseTheTemplatesWouldPutThemThere() {
        val notOptedIn = sidebarModules(
            resources = mapOf(ResourceGroup.KOTLIN to listOf(resource(ResourceGroup.KOTLIN, "gameplay/src/main/kotlin/Main.kt", "gameplay", ""))),
            folderGroups = folderTreeGroups(null, emptyMap(), null, emptyList(), emptySet(), null),
            layout = ProjectLayout(sceneModule = "", modules = modulesOf("" to false, "gameplay" to true)),
        )
        assertEquals(listOf("gameplay"), notOptedIn.map { it.path })
        assertEquals(emptySet(), notOptedIn.single().creatingGroups, "Nothing loads Scenes from where the templates keep them in this project")
    }

    @Test
    fun aProjectThatIsntOptedInOpensOnTheModuleChosenInTheToolbar() {
        val modules = sidebarModules(
            resources = mapOf(ResourceGroup.KOTLIN to listOf("app/desktop", "editor/model", "gameplay").map { resource(ResourceGroup.KOTLIN, "$it/src/main/kotlin/Main.kt", it, "") }),
            folderGroups = folderTreeGroups(null, emptyMap(), null, emptyList(), emptySet(), null),
            layout = ProjectLayout(modules = modulesOf("" to false, "app/desktop" to false, "editor/model" to true, "gameplay" to true)),
            gameModule = moduleFolderOf(":app:desktop"),
        )
        assertEquals(listOf("app", "app/desktop"), modules.filter { it.holdsGame }.map { it.path })
        assertEquals("app", modules.first().path)
        val optedIn = sidebarModules(resources, folderGroups, ProjectLayout(typeModule = "game", sceneModule = "game"), gameModule = "tools")
        assertEquals(listOf("game"), optedIn.filter { it.holdsGame }.map { it.path }, "Opted in, the game is where its code is generated")
    }

    private fun modulesOf(vararg modules: Pair<String, Boolean>) = modules.associate { (path, usesKubriko) -> path to ProjectModule(usesKubriko) }

    @Test
    fun theScreenplayIsInTheGamesModuleOnlyAndInAProjectThatIsntOptedInOnlyOnceItHasPages() {
        val (_, android, tools) = modules
        assertEquals(listOf("README" to "", "levels/cave" to "levels"), sidebarModules(resources, folderTreeGroups(listOf(ActorTypeDefinition(name = "Ball")), emptyMap(), null, emptyList(), emptySet(), null, pageGroupState(listOf("README", "levels/cave"), setOf("levels"), null)), ProjectLayout(typeModule = "game", sceneModule = "game")).first().folderGroups.getValue(FolderGroup.PAGES).items)
        listOf(android, tools).forEach { assertEquals(false, FolderGroup.PAGES in it.folderGroups) }
        val layout = ProjectLayout(sceneModule = "", modules = modulesOf("" to false, "gameplay" to true))
        val kotlin = mapOf(ResourceGroup.KOTLIN to listOf(resource(ResourceGroup.KOTLIN, "gameplay/src/main/kotlin/Main.kt", "gameplay", "")))
        val withPages = sidebarModules(kotlin, folderTreeGroups(null, emptyMap(), null, emptyList(), emptySet(), null, pageGroupState(listOf("README"), emptySet(), null)), layout, gameModule = "gameplay")
        assertEquals(listOf("README" to ""), withPages.first { it.path == "gameplay" }.folderGroups.getValue(FolderGroup.PAGES).items)
        assertEquals(setOf(FolderGroup.PAGES), withPages.first { it.path == "gameplay" }.creatingGroups)
    }
}
