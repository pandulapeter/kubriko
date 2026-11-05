/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditPage
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.DocumentStore
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStore
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class, EditorMutation::class)
class OpenMarkdownTextTest {
    private val root = testDirectory()
    private val screenplay = ScreenplayStore(root)
    private val documents = DocumentStore(root)

    @Test
    fun aDocumentShowsItsTextFollowsChangesAndClosesOnceItIsGone() = uiTest {
        File(root, "docs").mkdirs()
        File(root, "docs/combat.md").writeText("Swords.")
        documents.refresh(listOf("docs/combat.md"))
        var goneCount = 0
        showing { Text(rememberOpenMarkdownText(screenplay, documents, WorkspaceSelection.Document("docs/combat.md")) { goneCount++ } ?: "Nothing") }
        waitUntil { onAllNodesWithText("Swords.").fetchSemanticsNodes().isNotEmpty() }
        documents.write("docs/combat.md", "Spears.")
        waitUntil { onAllNodesWithText("Spears.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, goneCount)
        documents.refresh(emptyList())
        waitUntil { goneCount == 1 }
    }

    @Test
    fun aPageShowsItsText() = uiTest {
        File(root, "screenplay").mkdirs()
        File(root, "screenplay/hero.md").writeText("A knight.")
        screenplay.refresh()
        showing { Text(rememberOpenMarkdownText(screenplay, documents, WorkspaceSelection.Page("hero")) {} ?: "Nothing") }
        waitUntil { onAllNodesWithText("A knight.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aPageIsSavedByEditPageAndADocumentByEditDocument() {
        assertEquals(EditPage, markdownEditOf(WorkspaceSelection.Page("hero"), "Text")?.command)
        assertEquals(EditDocument.Input("README.md", "Text"), markdownEditOf(WorkspaceSelection.Document("README.md"), "Text")?.input)
        assertNull(markdownEditOf(WorkspaceSelection.None, "Text"))
        assertEquals("docs/combat.md", markdownTitleOf(WorkspaceSelection.Document("docs/combat.md")))
        assertEquals("hero", markdownTitleOf(WorkspaceSelection.Page("hero")))
        assertNull(markdownTitleOf(WorkspaceSelection.Scene("level")))
    }
}
