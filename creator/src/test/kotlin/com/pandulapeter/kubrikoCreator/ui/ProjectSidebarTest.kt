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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
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
        onDeleteItem = { group, name -> deletedItems += group to name },
    )
    private val deletedItems = mutableListOf<Pair<FolderGroup, String>>()

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
    }

    @Test
    fun kindsOfAssetsNothingUsesYetAreOnlyListedWhenTheProjectHasSomeAndScenesAreLeftToTheirTree() {
        assertEquals(listOf(ResourceGroup.KOTLIN), shownResourceGroups(emptyMap()))
        val sound = ProjectResource(ResourceGroup.SOUNDS, File("hit.wav"), "hit.wav")
        assertEquals(listOf(ResourceGroup.SOUNDS, ResourceGroup.KOTLIN), shownResourceGroups(mapOf(ResourceGroup.SOUNDS to listOf(sound))))
    }
}
