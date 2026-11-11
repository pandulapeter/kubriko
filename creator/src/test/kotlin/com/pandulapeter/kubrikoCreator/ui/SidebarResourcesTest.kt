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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.project.ProjectModule
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SidebarResourcesTest {
    private fun resource(group: ResourceGroup, path: String) = ProjectResource(group, File(path), path)

    private val resources = mapOf(
        ResourceGroup.SPRITES to listOf(resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable/ball.png"), resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable-hdpi/ball.png")),
        ResourceGroup.SOUNDS to listOf(resource(ResourceGroup.SOUNDS, "src/commonMain/composeResources/files/sounds/hit.wav")),
    )

    private val noFolders = folderTreeGroups(types = null, typeFolders = emptyMap(), selectedTypeName = null, sceneNames = emptyList(), sceneFolders = emptySet(), openSceneName = null)

    @Test
    fun aFilesPathFromComposeResourcesIsKnownOnlyForTheOnesCreatorKeeps() {
        assertEquals("drawable/ball.png", resourcePathOf(resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable/ball.png")))
        assertEquals("files/sounds/hit.wav", resourcePathOf(resource(ResourceGroup.SOUNDS, "game/src/commonMain/composeResources/files/sounds/hit.wav")))
        assertNull(resourcePathOf(resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable-hdpi/ball.png")))
        assertNull(resourcePathOf(resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable/icon.svg")))
    }

    @Test
    fun onlySpritesSoundsAndMusicAreTakenFromDroppedOrPastedFiles() {
        val folder = testDirectory()
        val files = listOf("ball.png", "hit.WAV", "notes.txt", "theme.ogg").map { File(folder, it).apply { writeText("x") } } + File(folder, "missing.png")
        assertEquals(listOf("ball.png", "hit.WAV", "theme.ogg"), resourceFilesAmong(files).map { it.name })
        val clipboard = Clipboard("files")
        clipboard.setContents(FileList(files), null)
        assertEquals(listOf("ball.png", "hit.WAV", "theme.ogg"), pastedResources(clipboard).map { it.name })
        clipboard.setContents(StringSelection("just text"), null)
        assertEquals(emptyList(), pastedResources(clipboard))
    }

    @Test
    fun aPastedImageIsAddedAsASpriteNamedForBeingPasted() {
        val clipboard = Clipboard("image")
        clipboard.setContents(ImageSelection(BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB)), null)
        val pasted = pastedResources(clipboard).single()
        assertEquals(PASTED_IMAGE_NAME, pasted.name)
        assertEquals("png", pasted.file.extension)
    }

    @Test
    fun spritesSoundsAndMusicAreAddedRenamedAndDeletedFromTheSidebar() = uiTest(width = 300, height = 600) {
        val chosen = mutableListOf<ResourceKind>()
        val renamed = mutableListOf<Pair<String, String>>()
        val deleted = mutableListOf<String>()
        var pastes = 0
        val actions = ResourceActions(
            onChoose = { chosen += it },
            onPaste = { pastes++ },
            onDelete = { deleted += it },
        )
        val folderActions = FolderTreeActions(
            onSelected = { _, _ -> },
            onSelectionCleared = {},
            onCreateItem = { _, _ -> null },
            onCreateFolder = { _, _ -> null },
            onRenameFolder = { _, _, _ -> null },
            onDeleteFolder = { _, _, _ -> },
            onRenameItem = { _, path, name -> if (name == "taken") Rejection(CommandProblem.RESOURCE_TAKEN, path) else null.also { renamed += path to name } },
            onDeleteItem = { _, path -> deleted += path },
        )
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = noFolders,
                folderActions = folderActions,
                dragState = SidebarDragState(),
                documentActions = DocumentActions(onOpen = {}, onCreate = { _, _ -> null }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {}),
                resourceActions = actions,
            )
        }
        saveSnapshot("sidebar-resources")
        onNodeWithText("hit.wav").performMouseInput { rightClick() }
        onNodeWithText("Rename").performClick()
        onNode(hasSetTextAction() and isFocused()).assert(hasText("hit"))
        onNode(hasSetTextAction() and isFocused()).performTextReplacement("taken")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        saveSnapshot("sidebar-resource-rename-refused")
        onNode(hasSetTextAction() and isFocused()).performTextReplacement("bounce")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("files/sounds/hit.wav" to "bounce"), renamed)
        onAllNodesWithText("ball.png")[0].performMouseInput { rightClick() }
        onNodeWithText("Delete file").performClick()
        assertEquals(listOf("drawable/ball.png"), deleted)
        onAllNodesWithText("ball.png")[1].performMouseInput { rightClick() }
        onNodeWithText("Delete file").assertDoesNotExist()
        onNodeWithText("Sounds").performMouseInput { rightClick() }
        onNodeWithText("Add sound…").performClick()
        onNodeWithText("Sprites").performMouseInput { rightClick() }
        onNodeWithText("Paste").performClick()
        onNodeWithContentDescription("New").performClick()
        onNodeWithText("Add music…").performClick()
        assertEquals(listOf(ResourceKind.SOUND, ResourceKind.MUSIC), chosen)
        assertEquals(1, pastes)
    }

    @Test
    fun copiedTextIsNeverPastedAsAnImage() {
        val clipboard = Clipboard("text")
        clipboard.setContents(TextWithImage(BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB)), null)
        assertEquals(emptyList(), pastedResources(clipboard))
    }

    @Test
    fun anotherModulesFilesOfferNothing() = uiTest(width = 300, height = 600) {
        val layout = ProjectLayout(typeModule = "game", sceneModule = "game", modules = mapOf("game" to ProjectModule(true), "tools" to ProjectModule(false)))
        val tools = ProjectResource(ResourceGroup.SPRITES, File("tools/src/main/composeResources/drawable/logo.png"), "tools/src/main/composeResources/drawable/logo.png", module = "tools")
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = mapOf(ResourceGroup.SPRITES to listOf(tools)),
                folderGroups = noFolders,
                folderActions = FolderTreeActions({ _, _ -> }, {}, { _, _ -> null }, { _, _ -> null }, { _, _, _ -> null }, { _, _, _ -> }, { _, _, _ -> null }, { _, _ -> }),
                dragState = SidebarDragState(),
                documentActions = DocumentActions(onOpen = {}, onCreate = { _, _ -> null }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {}),
                resourceActions = ResourceActions(),
                layout = layout,
            )
        }
        onNodeWithText("tools").performClick()
        onNodeWithText("logo.png").performMouseInput { rightClick() }
        onNodeWithText("Delete file").assertDoesNotExist()
        onAllNodesWithText("Sprites").fetchSemanticsNodes().let { assertEquals(2, it.size, "The game's empty group, and the tools' one.") }
        onAllNodesWithText("Sprites")[1].performMouseInput { rightClick() }
        onNodeWithText("Paste").assertDoesNotExist()
    }

    private class TextWithImage(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.stringFlavor, DataFlavor.imageFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor in transferDataFlavors

        override fun getTransferData(flavor: DataFlavor): Any = if (flavor == DataFlavor.stringFlavor) "words" else image
    }

    private class FileList(private val files: List<File>) : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.javaFileListFlavor

        override fun getTransferData(flavor: DataFlavor): Any = files
    }

    private class ImageSelection(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor

        override fun getTransferData(flavor: DataFlavor): Any = image
    }
}
