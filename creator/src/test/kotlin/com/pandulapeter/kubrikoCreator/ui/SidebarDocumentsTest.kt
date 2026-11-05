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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
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

@OptIn(ExperimentalTestApi::class)
class SidebarDocumentsTest {
    private fun document(path: String, module: String = "") = ProjectResource(ResourceGroup.DOCUMENTS, File(path), path, module = module)

    private fun kotlin(module: String) = ProjectResource(ResourceGroup.KOTLIN, File("$module/src/main/kotlin/Main.kt"), "$module/src/main/kotlin/Main.kt", "", module)

    private val severalModules = ProjectLayout(sceneModule = "game", modules = mapOf("" to ProjectModule(false), "game" to ProjectModule(true), "editor/model" to ProjectModule(false)))
    private val documents = listOf(document("README.md"), document("docs/combat.md"), document("docs/levels/cave.md"), document("editor/README.md"), document("game/NOTES.md", "game"))
    private val resources = mapOf(ResourceGroup.DOCUMENTS to documents, ResourceGroup.KOTLIN to listOf(kotlin("game"), kotlin("editor/model")))

    @Test
    fun aDocumentIsInTheBandOfTheFolderItIsInNeverInAGroup() {
        val modules = sidebarModules(resources, emptyMap(), severalModules)
        val byPath = modules.associateBy { it.path }
        assertEquals(listOf("README.md", "docs/combat.md", "docs/levels/cave.md"), byPath.getValue("").documents)
        assertEquals(listOf("editor/README.md"), byPath.getValue("editor").documents, "A folder holding only modules is a band of its own")
        assertEquals(listOf("game/NOTES.md"), byPath.getValue("game").documents)
        assertEquals(emptyList(), byPath.getValue("editor/model").documents)
        modules.forEach { assertEquals(false, ResourceGroup.DOCUMENTS in it.resourceGroups) }
        assertEquals(listOf("README.md" to "", "docs/combat.md" to "docs", "docs/levels/cave.md" to "docs/levels"), byPath.getValue("").documentItems())
        assertEquals(listOf("editor/README.md" to ""), byPath.getValue("editor").documentItems())
        assertEquals("editor/notes", byPath.getValue("editor").documentFolderPath("notes"))
    }

    @Test
    fun aModuleWithOnlyDocumentsIsListedAndFilteringNarrowsThemByName() {
        val modules = sidebarModules(mapOf(ResourceGroup.DOCUMENTS to listOf(document("tools/README.md"))), emptyMap(), ProjectLayout(sceneModule = "game", modules = mapOf("game" to ProjectModule(true), "tools/cli" to ProjectModule(false))))
        assertEquals(listOf("game", "tools"), modules.map { it.path })
        val filtered = filteredModules(sidebarModules(resources, emptyMap(), severalModules), "cave")
        assertEquals(listOf("docs/levels/cave.md"), filtered.first { it.path == "" }.documents)
    }

    @Test
    fun theDocumentFoldersStartClosedUnderTheirModule() {
        val root = sidebarModules(resources, emptyMap(), severalModules).first { it.path == "" }
        val nodes = root.outlineNodes(isGrouped = true).filter { it.key.startsWith(SidebarKeys.documents("")) }
        assertEquals(listOf(SidebarKeys.documents(""), SidebarKeys.folder(SidebarKeys.documents(""), "docs"), SidebarKeys.folder(SidebarKeys.documents(""), "docs/levels")), nodes.map { it.key })
        assertEquals(listOf(true, false, false), nodes.map { it.isExpandedByDefault })
        assertEquals(SidebarKeys.module(""), nodes.first().parent)
    }

    @Test
    fun documentsAreRowsThatOpenAndOfferWhatCanBeDoneToThem() = uiTest(width = 300, height = 600) {
        val opened = mutableListOf<String>()
        val created = mutableListOf<Pair<String, String>>()
        val renamed = mutableListOf<Pair<String, String>>()
        val deleted = mutableListOf<String>()
        val added = mutableListOf<String>()
        val actions = DocumentActions(
            onOpen = { opened += it },
            onCreate = { folder, name -> null.also { created += folder to name } },
            onRename = { path, name -> null.also { renamed += path to name } },
            onDelete = { deleted += it },
            onAddToScreenplay = { added += it },
        )
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = mapOf(ResourceGroup.DOCUMENTS to listOf(document("README.md"), document("docs/combat.md"))),
                folderGroups = emptyMap(),
                folderActions = FolderTreeActions({ _, _ -> }, {}, { _, _ -> null }, { _, _ -> null }, { _, _, _ -> null }, { _, _ -> }, { _, _, _ -> null }, { _, _ -> }),
                dragState = SidebarDragState(),
                documentActions = actions,
                selectedDocument = "README.md",
            )
        }
        onNodeWithText("README.md").assertExists()
        onNodeWithText("combat.md").assertDoesNotExist()
        saveSnapshot("sidebar-documents")
        onNodeWithText("docs").performClick()
        onNodeWithText("combat.md").performClick()
        assertEquals(listOf("docs/combat.md"), opened)
        onNodeWithText("combat.md").performMouseInput { rightClick() }
        onNodeWithText("Rename").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextReplacement("fights")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("docs/combat.md" to "fights"), renamed)
        onNodeWithText("docs").performMouseInput { rightClick() }
        onNodeWithText("New document").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("bosses")
        saveSnapshot("sidebar-new-document")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("docs" to "bosses"), created)
        onNodeWithText("README.md").performMouseInput { rightClick() }
        onNodeWithText("Add to the screenplay").performClick()
        onNodeWithText("README.md").performMouseInput { rightClick() }
        onNodeWithText("Delete document").performClick()
        assertEquals(listOf("README.md"), added)
        assertEquals(listOf("README.md"), deleted)
    }

    @Test
    fun aDocumentDraggedOntoAFolderABandOrAnotherDocumentMovesIntoItsFolderInAnyModule() = uiTest(width = 300, height = 600) {
        val moves = mutableListOf<Pair<String, String>>()
        val dragState = SidebarDragState().apply { onMoveToFolder = { group, name, folder -> if (group == null) moves += name to folder } }
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = emptyMap(),
                folderActions = FolderTreeActions({ _, _ -> }, {}, { _, _ -> null }, { _, _ -> null }, { _, _, _ -> null }, { _, _ -> }, { _, _, _ -> null }, { _, _ -> }),
                dragState = dragState,
                documentActions = DocumentActions(onOpen = {}, onCreate = { _, _ -> null }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {}),
                layout = severalModules,
            )
        }
        onNodeWithText(ROOT_MODULE).performClick()
        dragOnto("README.md", "docs")
        dragOnto("NOTES.md", "editor")
        dragOnto("NOTES.md", ROOT_MODULE)
        assertEquals(listOf("README.md" to "docs", "game/NOTES.md" to "editor", "game/NOTES.md" to ""), moves)
    }

    @Test
    fun aBandOffersANewDocumentInItsFolder() = uiTest(width = 300, height = 600) {
        val created = mutableListOf<Pair<String, String>>()
        showing {
            ProjectSidebar(
                modifier = Modifier.fillMaxSize(),
                resources = resources,
                folderGroups = emptyMap(),
                folderActions = FolderTreeActions({ _, _ -> }, {}, { _, _ -> null }, { _, _ -> null }, { _, _, _ -> null }, { _, _ -> }, { _, _, _ -> null }, { _, _ -> }),
                dragState = SidebarDragState(),
                documentActions = DocumentActions(onOpen = {}, onCreate = { folder, name -> null.also { created += folder to name } }, onRename = { _, _ -> null }, onDelete = {}, onAddToScreenplay = {}),
                layout = severalModules,
            )
        }
        onNodeWithText("editor").performMouseInput { rightClick() }
        onNodeWithText("New document").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("plan")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("editor" to "plan"), created)
    }

    private companion object {
        const val ROOT_MODULE = "Root module"
    }
}
