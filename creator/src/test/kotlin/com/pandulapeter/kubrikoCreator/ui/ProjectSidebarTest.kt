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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectModule
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProjectSidebarTest {
    private val folderGroups = folderTreeGroups(
        types = listOf(ActorTypeDefinition(name = "Ball"), ActorTypeDefinition(name = "Bat", folder = "enemies")).map(SidebarType::of),
        typeFolders = emptyMap(),
        selectedTypeName = null,
        sceneNames = listOf("Intro", "Level", "Finale"),
        sceneFolders = emptySet(),
        openSceneName = null,
    )
    private val resources = mapOf(ResourceGroup.SPRITES to listOf(ProjectResource(ResourceGroup.SPRITES, File("hero.png"), "drawable/hero.png")))
    private val folderActions = FolderTreeActions(
        onSelected = { _, _ -> },
        onSelectionCleared = {},
        onCreateItem = { _, _ -> null },
        onCreateFolder = { _, _ -> null },
        onRenameFolder = { _, _, _ -> null },
        onDeleteFolder = { _, _, _ -> },
        onRenameItem = { group, name, newName -> null.also { renamedItems += Triple(group, name, newName) } },
        onDeleteItem = { group, name -> deletedItems += group to name },
    )
    private val noDocumentActions = DocumentActions(onOpen = {}, onCreate = { _, _ -> null }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {})
    private val deletedItems = mutableListOf<Pair<FolderGroup, String>>()
    private val renamedItems = mutableListOf<Triple<FolderGroup, String, String>>()

    @Test
    fun theFilterNarrowsEverythingToMatchingNamesOpeningTheirFoldersAndEscapeBringsBackWhatWasCollapsed() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
            )
        }
        onNodeWithText("enemies").performClick()
        onNodeWithText("Bat").assertDoesNotExist()
        onNode(hasSetTextAction()).performTextInput("bat")
        onNodeWithText("Bat").assertExists()
        listOf("Ball", "Intro", "Level", "Finale", "hero.png").forEach { onNodeWithText(it).assertDoesNotExist() }
        saveSnapshot("sidebar-filtered")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        listOf("Ball", "Intro", "Level", "Finale", "hero.png").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Bat").assertDoesNotExist()
        onNode(hasSetTextAction()).performTextInput("zzz")
        onNodeWithText("Nothing matches.").assertExists()
        saveSnapshot("sidebar-filter-none")
    }

    @Test
    fun draggingAnItemOntoAFolderMovesItInAndOntoAnythingOutsideEveryFolderMovesItOut() = uiTest(width = 300, height = 600) {
        val moves = mutableListOf<Pair<String, String>>()
        val dragState = SidebarDragState().apply { onMoveToFolder = { _, name, folder -> moves += name to folder } }
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = dragState,
            )
        }
        dragOnto("Ball", "enemies")
        dragOnto("Bat", "Actor types")
        dragOnto("Bat", "Ball")
        assertEquals(listOf("Ball" to "enemies", "Bat" to "", "Bat" to ""), moves)
    }

    @Test
    fun everyRowsMoreOptionsButtonLinesUpWithTheHeadersAndStaysThereWhileAMenuIsOpen() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
            )
        }
        val edges = onAllNodesWithContentDescription("More options").fetchSemanticsNodes().map { it.boundsInRoot.right }
        assertTrue(edges.size >= 3, "Only ${edges.size} rows offer more options")
        assertEquals(1, edges.toSet().size, "A row's more options button lines up with a header's: $edges")
        val before = onAllNodesWithContentDescription("More options").fetchSemanticsNodes().map { it.boundsInRoot }
        onNodeWithText("Level").performMouseInput { rightClick() }
        onNodeWithText("Delete Scene").assertExists()
        val whileOpen = onAllNodesWithContentDescription("More options").fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(before, whileOpen, "Opening a menu moved the buttons")
    }

    @Test
    fun aRightClickOffersWhatCanBeDoneToARowAndAHeader() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
            )
        }
        onNodeWithText("Level").performMouseInput { rightClick() }
        saveSnapshot("sidebar-context-menu")
        onNodeWithText("Delete Scene").performClick()
        assertEquals(listOf(FolderGroup.SCENES to "Level"), deletedItems)
        onNodeWithText("Delete Scene").assertDoesNotExist()
        onNodeWithText("Scenes").performMouseInput { rightClick() }
        onNodeWithText("New Scene").performClick()
        onNodeWithText("New Scene name", useUnmergedTree = true).assertExists()
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Intro").performMouseInput { rightClick() }
        onNodeWithText("Rename").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextReplacement("Opening")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(Triple(FolderGroup.SCENES, "Intro", "Opening")), renamedItems)
        onNodeWithText("enemies").performMouseInput { rightClick() }
        onNodeWithText("New folder").performClick()
        onNodeWithText("Folder name", useUnmergedTree = true).assertExists()
        saveSnapshot("sidebar-new-subfolder")
    }

    @Test
    fun kindsOfAssetsNothingUsesYetAreOnlyListedWhenTheProjectHasSomeAndScenesAreLeftToTheirTree() {
        assertEquals(listOf(ResourceGroup.KOTLIN), shownResourceGroups(emptyMap()))
        val sound = ProjectResource(ResourceGroup.SOUNDS, File("hit.wav"), "hit.wav")
        assertEquals(listOf(ResourceGroup.SOUNDS, ResourceGroup.KOTLIN), shownResourceGroups(mapOf(ResourceGroup.SOUNDS to listOf(sound))))
    }

    @Test
    fun kotlinFilesAreGroupedByPlatformsStartingClosedAndByPackagesBelowTheRootOneAndOpenAndCloseWithoutAnyActions() = uiTest(width = 300, height = 600) {
        fun kotlin(path: String, kotlinPackage: String) = ProjectResource(ResourceGroup.KOTLIN, File(path), path, kotlinPackage)
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = mapOf(
                    ResourceGroup.KOTLIN to listOf(
                        kotlin("src/commonMain/kotlin/com/game/Game.kt", "com.game"),
                        kotlin("src/commonMain/kotlin/com/game/ui/feature1/Menu.kt", "com.game.ui.feature1"),
                        kotlin("src/desktopMain/kotlin/com/game/Main.kt", "com.game"),
                        kotlin("src/androidMain/kotlin/com/game/MainActivity.kt", "com.game"),
                    ),
                ),
                folderGroups = emptyMap(),
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
            )
        }
        onNodeWithText("Kotlin files").performClick()
        listOf("Desktop", "Android", "ui.feature1", "Game.kt").forEach { onNodeWithText(it).assertExists() }
        listOf("Main.kt", "MainActivity.kt", "Menu.kt").forEach { onNodeWithText(it).assertDoesNotExist() }
        onAllNodesWithContentDescription("More options").assertCountEquals(0)
        onNodeWithText("Android").performClick()
        onNodeWithText("MainActivity.kt").assertExists()
        onNodeWithText("ui.feature1").performClick()
        onNodeWithText("Menu.kt").assertExists()
        saveSnapshot("sidebar-kotlin-files")
        onNodeWithText("ui.feature1").performClick()
        onNodeWithText("Menu.kt").assertDoesNotExist()
        onNodeWithText("Main.kt").assertDoesNotExist()
        onNode(hasSetTextAction()).performTextInput("main")
        listOf("MainActivity.kt", "Main.kt").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Game.kt").assertDoesNotExist()
    }

    @Test
    fun aFolderOpensWithTheFoldersInItClosedAndTheButtonsOfARowOpenOrCloseEverythingInIt() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = emptyMap(),
                folderGroups = folderTreeGroups(
                    types = listOf(ActorTypeDefinition(name = "Bat", folder = "enemies/flying"), ActorTypeDefinition(name = "Slime", folder = "enemies")).map(SidebarType::of),
                    typeFolders = emptyMap(),
                    selectedTypeName = null,
                    sceneNames = emptyList(),
                    sceneFolders = emptySet(),
                    openSceneName = null,
                ),
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
            )
        }
        listOf("enemies", "flying", "Bat", "Slime").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Actor types").performMouseInput { moveTo(center) }
        saveSnapshot("sidebar-expand-collapse-buttons")
        onAllNodesWithContentDescription("Collapse all").onFirst().performClick()
        onNodeWithText("enemies").assertExists()
        listOf("flying", "Bat", "Slime").forEach { onNodeWithText(it).assertDoesNotExist() }
        onNodeWithText("enemies").performClick()
        listOf("flying", "Slime").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Bat").assertDoesNotExist()
        onAllNodesWithContentDescription("Expand all").onFirst().performClick()
        onNodeWithText("Bat").assertExists()
        onAllNodesWithContentDescription("Expand all").assertCountEquals(1)
        onNodeWithText("enemies").performMouseInput { moveTo(center) }
        onAllNodesWithContentDescription("Expand all").assertCountEquals(2)
        saveSnapshot("sidebar-folder-hover-buttons")
    }

    @Test
    fun aProjectOfSeveralModulesShowsEachUnderItsOwnBandAndAddsOnlyWhereCreatorWrites() = uiTest(width = 300, height = 700) {
        fun resource(group: ResourceGroup, path: String, module: String) = ProjectResource(group, File(path), path, if (group == ResourceGroup.KOTLIN) "com.game" else null, module)
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = listOf(
                    resource(ResourceGroup.SCENES, "src/commonMain/composeResources/files/scenes/Intro.json", ""),
                    resource(ResourceGroup.SCENES, "tools/src/commonMain/composeResources/files/scenes/Sandbox.json", "tools"),
                    resource(ResourceGroup.SPRITES, "tools/src/commonMain/composeResources/drawable/grid.png", "tools"),
                    resource(ResourceGroup.KOTLIN, "tools/src/desktopMain/kotlin/com/game/Tool.kt", "tools"),
                ).groupBy { it.group },
                folderGroups = folderTreeGroups(
                    types = listOf(ActorTypeDefinition(name = "Ball")).map(SidebarType::of),
                    typeFolders = emptyMap(),
                    selectedTypeName = null,
                    sceneNames = listOf("Intro", "Sandbox"),
                    sceneFolders = emptySet(),
                    openSceneName = null,
                ),
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
                layout = ProjectLayout(typeModule = "", sceneModule = "", modules = modulesOf("" to true, "tools" to false)),
            )
        }
        listOf("Root module", "tools", "Ball", "Intro").forEach { onNodeWithText(it).assertExists() }
        listOf("Sandbox", "grid.png").forEach { onNodeWithText(it).assertDoesNotExist() }
        onNodeWithText("tools").performClick()
        listOf("Sandbox", "grid.png").forEach { onNodeWithText(it).assertExists() }
        onAllNodesWithText(":tools", substring = true).assertCountEquals(0)
        onAllNodesWithText("Scenes").assertCountEquals(2)
        onAllNodesWithText("Kotlin files").assertCountEquals(2)
        onAllNodesWithText("Sounds").assertCountEquals(1)
        saveSnapshot("sidebar-modules")
        onNodeWithText("Sandbox").performMouseInput { rightClick() }
        onNodeWithText("Delete Scene").performClick()
        assertEquals(listOf(FolderGroup.SCENES to "Sandbox"), deletedItems)
        onNodeWithText("Root module").performClick()
        listOf("Ball", "Intro").forEach { onNodeWithText(it).assertDoesNotExist() }
        onNode(hasSetTextAction()).performTextInput("grid")
        listOf("tools", "grid.png").forEach { onNodeWithText(it).assertExists() }
        listOf("Root module", "Sandbox").forEach { onNodeWithText(it).assertDoesNotExist() }
    }

    @Test
    fun modulesInsideAnotherOneAreShownInsideItsBandAndCloseWithIt() = uiTest(width = 300, height = 700) {
        fun kotlin(path: String, module: String) = ProjectResource(ResourceGroup.KOTLIN, File(path), path, "com.game", module)
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = mapOf(
                    ResourceGroup.KOTLIN to listOf(
                        kotlin("app/desktop/src/main/kotlin/com/game/Main.kt", "app/desktop"),
                        kotlin("app/android/src/main/kotlin/com/game/MainActivity.kt", "app/android"),
                        kotlin("shared/src/commonMain/kotlin/com/game/Game.kt", "shared"),
                    ),
                ),
                folderGroups = emptyMap(),
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
                layout = ProjectLayout(sceneModule = "shared", modules = modulesOf("" to false, "app/desktop" to false, "app/android" to false, "shared" to true)),
            )
        }
        listOf("shared", "app").forEach { onNodeWithText(it).assertExists() }
        listOf("Root module", "desktop", "android").forEach { onNodeWithText(it).assertDoesNotExist() }
        onNodeWithText("app").performClick()
        listOf("desktop", "android").forEach { onNodeWithText(it).assertExists() }
        listOf("app:desktop", "app/desktop").forEach { onNodeWithText(it).assertDoesNotExist() }
        val app = onNodeWithText("app", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val desktop = onNodeWithText("desktop", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(desktop.left > app.left, "A module inside another one is indented below it")
        assertTrue(desktop.top > app.top)
        saveSnapshot("sidebar-nested-modules")
        onNodeWithText("app").performClick()
        listOf("desktop", "android").forEach { onNodeWithText(it).assertDoesNotExist() }
        onNodeWithText("shared").assertExists()
    }

    @Test
    fun theModulesFoundOnceTheProjectIsReadAreShownFromTheTopRatherThanScrolledAway() = uiTest(width = 300, height = 600) {
        val game = ProjectResource(ResourceGroup.KOTLIN, File("src/commonMain/kotlin/Game.kt"), "src/commonMain/kotlin/Game.kt", "com.game", "")
        val tool = ProjectResource(ResourceGroup.KOTLIN, File("tools/src/main/kotlin/Tool.kt"), "tools/src/main/kotlin/Tool.kt", "com.game", "tools")
        var layout by mutableStateOf(ProjectLayout())
        var resources by mutableStateOf(emptyMap<ResourceGroup, List<ProjectResource>>())
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
                layout = layout,
            )
        }
        onNodeWithText("Root module").assertDoesNotExist()
        layout = ProjectLayout(modules = modulesOf("" to true, "tools" to false))
        resources = mapOf(ResourceGroup.KOTLIN to listOf(game, tool))
        waitForIdle()
        onNodeWithText("Root module").assertIsDisplayed()
        onNodeWithText("Actor types").assertIsDisplayed()
    }

    @Test
    fun aGroupWithNothingInItIsOnlyItsFadedHeader() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = emptyMap(),
                folderGroups = emptyMap(),
                folderActions = folderActions,
                documentActions = noDocumentActions,
                dragState = SidebarDragState(),
                layout = ProjectLayout(modules = modulesOf("" to true)),
            )
        }
        listOf("Sprites", "Sounds", "Music", "Kotlin files").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Sprites").performClick()
        onAllNodesWithContentDescription("Hide").assertCountEquals(0)
        saveSnapshot("sidebar-empty-groups")
    }

    private fun modulesOf(vararg modules: Pair<String, Boolean>) = modules.associate { (path, usesKubriko) -> path to ProjectModule(usesKubriko) }
}
