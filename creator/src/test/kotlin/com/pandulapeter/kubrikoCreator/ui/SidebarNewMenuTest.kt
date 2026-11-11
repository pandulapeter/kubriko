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
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
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
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SidebarNewMenuTest {
    private val folderGroups = folderTreeGroups(
        types = listOf(ActorTypeDefinition(name = "Ball")).map(SidebarType::of),
        typeFolders = emptyMap(),
        selectedTypeName = null,
        sceneNames = listOf("Intro"),
        sceneFolders = emptySet(),
        openSceneName = null,
    )
    private val layout = ProjectLayout(typeModule = "game", sceneModule = "game", modules = mapOf("game" to ProjectModule(true), "tools" to ProjectModule(false)))
    private val resources = mapOf(ResourceGroup.KOTLIN to listOf(ProjectResource(ResourceGroup.KOTLIN, File("tools/src/main/kotlin/Main.kt"), "tools/src/main/kotlin/Main.kt", "", "tools")))
    private val createdItems = mutableListOf<Pair<FolderGroup, String>>()
    private val createdDocuments = mutableListOf<Pair<String, String>>()
    private val folderActions = FolderTreeActions(
        onSelected = { _, _ -> },
        onSelectionCleared = {},
        onCreateItem = { group, name -> null.also { createdItems += group to name } },
        onCreateFolder = { _, _ -> null },
        onRenameFolder = { _, _, _ -> null },
        onDeleteFolder = { _, _, _ -> },
        onRenameItem = { _, _, _ -> null },
        onDeleteItem = { _, _ -> },
    )
    private val documentActions = DocumentActions(onOpen = {}, onCreate = { folder, name -> null.also { createdDocuments += folder to name } }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {})

    @Test
    fun theMenuOffersWhatSomeModuleCreatesAndADocumentAlways() {
        val modules = sidebarModules(resources, folderGroups, layout)
        assertEquals(
            listOf(FolderGroup.PAGES, FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES, FolderGroup.SCENES).map(SidebarNewItem::GroupItem) +
                SidebarNewItem.Sprite + SidebarNewItem.SpriteFromFrames + ResourceKind.entries.map(SidebarNewItem::Resource) + SidebarNewItem.Document,
            sidebarNewItems(modules),
        )
        val toolsOnly = sidebarModules(resources, emptyMap(), ProjectLayout(modules = mapOf("tools" to ProjectModule(false))))
        assertEquals(
            listOf(SidebarNewItem.Sprite, SidebarNewItem.SpriteFromFrames) + ResourceKind.entries.map(SidebarNewItem::Resource) + SidebarNewItem.Document,
            sidebarNewItems(toolsOnly),
        )
        assertNull(SidebarNewItem.Sprite.target(modules))
        assertNull(SidebarNewItem.Resource(ResourceKind.SPRITE).target(modules))
        assertNull(SidebarNewItem.GroupItem(FolderGroup.SCENES).target(toolsOnly))
        val scene = SidebarNewItem.GroupItem(FolderGroup.SCENES).target(modules)!!
        assertEquals(SidebarNaming.NewItem(FolderGroup.SCENES), scene.naming)
        assertEquals(listOf(SidebarKeys.module("game")), scene.openedKeys)
        assertEquals(newItemKey(SidebarKeys.folderGroup("game", FolderGroup.SCENES)), scene.revealKey)
        assertEquals(SidebarNaming.NewProjectDocument, SidebarNewItem.Document.target(modules)?.naming)
    }

    @Test
    fun theHeadersHaveNoAddButtonOfTheirOwnAndTheMenuNamesANewThingWhereItGoes() = uiTest(width = 300, height = 600) {
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = folderGroups,
                folderActions = folderActions,
                dragState = SidebarDragState(),
                documentActions = documentActions,
                layout = layout,
            )
        }
        assertEquals(1, onAllNodesWithContentDescription("New").fetchSemanticsNodes().size)
        onNodeWithText("Ball").assertExists()
        onNodeWithText("game").performClick()
        onNodeWithText("Ball").assertDoesNotExist()
        onNodeWithContentDescription("New").performClick()
        saveSnapshot("sidebar-new-menu")
        onNodeWithText("New Scene").performClick()
        onNodeWithText("Ball").assertExists()
        onNode(hasSetTextAction() and isFocused()).performTextInput("Outro")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(FolderGroup.SCENES to "Outro"), createdItems)
        onNodeWithContentDescription("New").performClick()
        onNodeWithText("New document").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("docs/plan")
        saveSnapshot("sidebar-new-project-document")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("" to "docs/plan"), createdDocuments)
    }
}
