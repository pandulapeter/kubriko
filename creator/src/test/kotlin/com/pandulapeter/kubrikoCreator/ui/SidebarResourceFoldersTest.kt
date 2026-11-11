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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SidebarResourceFoldersTest {
    private fun sound(path: String) = "src/commonMain/composeResources/$path".let { ProjectResource(ResourceGroup.SOUNDS, File(it), it) }

    @Test
    fun soundsAreShownInTheirFoldersWhichAreMadeRenamedAndDeletedThere() = uiTest(width = 300, height = 600) {
        val created = mutableListOf<Pair<FolderGroup, String>>()
        val deleted = mutableListOf<Triple<FolderGroup, String, Boolean>>()
        val actions = FolderTreeActions(
            onSelected = { _, _ -> },
            onSelectionCleared = {},
            onCreateItem = { _, _ -> null },
            onCreateFolder = { group, path -> null.also { created += group to path } },
            onRenameFolder = { _, _, _ -> null },
            onDeleteFolder = { group, path, isEmpty -> deleted += Triple(group, path, isEmpty) },
            onRenameItem = { _, _, _ -> null },
            onDeleteItem = { _, _ -> },
        )
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = mapOf(ResourceGroup.SOUNDS to listOf(sound("files/sounds/jump.wav"), sound("files/sounds/enemies/roar.wav"))),
                folderGroups = folderTreeGroups(types = null, typeFolders = emptyMap(), selectedTypeName = null, sceneNames = emptyList(), sceneFolders = emptySet(), openSceneName = null),
                folderActions = actions,
                dragState = SidebarDragState(),
                documentActions = DocumentActions(onOpen = {}, onCreate = { _, _ -> null }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {}),
                layout = ProjectLayout(audioFolders = setOf("sounds", "sounds/enemies", "sounds/empty")),
            )
        }
        saveSnapshot("sidebar-audio-folders")
        onNodeWithText("sounds").assertDoesNotExist()
        onNodeWithText("roar.wav").assertExists()
        onNodeWithText("Sounds").performMouseInput { rightClick() }
        onNodeWithText("New folder").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("effects")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        onNodeWithText("enemies").performMouseInput { rightClick() }
        onNodeWithText("New folder").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("bosses")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(FolderGroup.SOUNDS to "sounds/effects", FolderGroup.SOUNDS to "sounds/enemies/bosses"), created)
        onNodeWithText("empty").performMouseInput { rightClick() }
        onNodeWithText("Delete folder").performClick()
        assertEquals(listOf(Triple(FolderGroup.SOUNDS, "sounds/empty", true)), deleted)
    }

    @Test
    fun spritesAreShownInTheirOwnFolders() = uiTest(width = 300, height = 600) {
        val created = mutableListOf<Pair<FolderGroup, String>>()
        val sprite = { path: String -> "src/commonMain/composeResources/$path".let { ProjectResource(ResourceGroup.SPRITES, File(it), it) } }
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = mapOf(ResourceGroup.SPRITES to listOf(sprite("drawable/bat.png"), sprite("drawable/ball.png"))),
                folderGroups = folderTreeGroups(types = null, typeFolders = emptyMap(), selectedTypeName = null, sceneNames = emptyList(), sceneFolders = emptySet(), openSceneName = null),
                folderActions = FolderTreeActions({ _, _ -> }, {}, { _, _ -> null }, { group, path -> null.also { created += group to path } }, { _, _, _ -> null }, { _, _, _ -> }, { _, _, _ -> null }, { _, _ -> }),
                dragState = SidebarDragState(),
                documentActions = DocumentActions(onOpen = {}, onCreate = { _, _ -> null }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {}),
                spriteFolders = SpriteFolderStore.State(setOf("enemies", "empty"), mapOf("bat" to "enemies")),
            )
        }
        saveSnapshot("sidebar-sprite-folders")
        onNodeWithText("enemies").assertExists()
        onNodeWithText("empty").assertExists()
        onNodeWithText("Sprites").performMouseInput { rightClick() }
        onNodeWithText("New folder").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("items")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(FolderGroup.SPRITES to "items"), created)
    }
}
