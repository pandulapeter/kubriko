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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProjectSidebarTest {
    private val folderGroups = folderTreeGroups(
        types = listOf(ActorTypeDefinition(name = "Ball"), ActorTypeDefinition(name = "Bat", folder = "enemies")),
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
        onDeleteFolder = { _, _ -> },
        onRenameItem = { group, name, newName -> null.also { renamedItems += Triple(group, name, newName) } },
        onDeleteItem = { group, name -> deletedItems += group to name },
    )
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
                dragState = dragState,
            )
        }
        dragOnto("Ball", "enemies")
        dragOnto("Bat", "Actor types")
        dragOnto("Bat", "Ball")
        assertEquals(listOf("Ball" to "enemies", "Bat" to "", "Bat" to ""), moves)
    }

    private fun ComposeUiTest.dragOnto(item: String, target: String) {
        val from = onNodeWithText(item).fetchSemanticsNode().boundsInRoot
        val to = onNodeWithText(target).fetchSemanticsNode().boundsInRoot.center
        onNodeWithText(item).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, 30f))
            moveTo(to - from.topLeft)
            release()
        }
        waitForIdle()
    }

    @Test
    fun everyRowsMoreOptionsButtonLinesUpWithTheHeadersAddButtonAndStaysThereWhileAMenuIsOpen() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                dragState = SidebarDragState(),
            )
        }
        val add = onNodeWithContentDescription("New Actor type").fetchSemanticsNode().boundsInRoot
        val edges = onAllNodesWithContentDescription("More options").fetchSemanticsNodes().map { it.boundsInRoot.right }
        assertTrue(edges.size >= 3, "Only ${edges.size} rows offer more options")
        assertEquals(setOf(add.right, add.left), edges.toSet(), "A row's more options button lines up with a header's +, which a header's own is just before")
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
    fun kotlinFilesAreGroupedByPlatformAndByPackageBelowTheRootOneAndItsFoldersOpenAndCloseWithoutAnyActions() = uiTest(width = 300, height = 600) {
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
                dragState = SidebarDragState(),
            )
        }
        onNodeWithText("Kotlin files").performClick()
        listOf("Desktop", "Android", "ui.feature1", "Menu.kt", "Main.kt", "MainActivity.kt", "Game.kt").forEach { onNodeWithText(it).assertExists() }
        onAllNodesWithContentDescription("More options").assertCountEquals(0)
        saveSnapshot("sidebar-kotlin-files")
        onNodeWithText("Android").performClick()
        onNodeWithText("MainActivity.kt").assertDoesNotExist()
        onNodeWithText("ui.feature1").performClick()
        onNodeWithText("Menu.kt").assertDoesNotExist()
        onNodeWithText("Main.kt").assertExists()
        onNode(hasSetTextAction()).performTextInput("main")
        listOf("MainActivity.kt", "Main.kt").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Game.kt").assertDoesNotExist()
    }
}
