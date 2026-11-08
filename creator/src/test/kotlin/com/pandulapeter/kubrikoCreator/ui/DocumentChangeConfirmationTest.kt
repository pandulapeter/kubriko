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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DocumentChangeConfirmationTest {

    @Test
    fun deletingADocumentSaysWhatHappensToTheFile() = uiTest(width = 700, height = 400) {
        var confirmed = 0
        showing { DocumentChangeConfirmation(PendingDocumentChange(Invocation(DeleteDocument, DeleteDocument.Input("docs/combat.md"))), unusedImagesOf = { emptyList() }, validate = { null }, onConfirm = { confirmed++ }, onDismiss = {}) }
        onNodeWithText("Delete docs/combat.md?").assertExists()
        saveSnapshot("document-delete-confirmation")
        onNodeWithText("Delete").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun addingADocumentToTheScreenplayNamesThePageItBecomesWhichCanStillBeChanged() = uiTest(width = 700, height = 400) {
        var dismissed = 0
        val confirmed = mutableListOf<Invocation<*>>()
        showing {
            DocumentChangeConfirmation(
                PendingDocumentChange(Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input("docs/combat.md", "combat"))),
                unusedImagesOf = { emptyList() },
                validate = { invocation -> (invocation.input as AddDocumentToScreenplay.Input).page.takeIf { it == "hero" }?.let { Rejection(CommandProblem.PAGE_TAKEN, it) } },
                onConfirm = { confirmed += it },
                onDismiss = { dismissed++ },
            )
        }
        onNodeWithText("Add docs/combat.md to the screenplay?").assertExists()
        onNodeWithText("screenplay/combat.md", substring = true).assertExists()
        saveSnapshot("document-add-to-screenplay-confirmation")
        onNode(hasSetTextAction()).performTextReplacement("hero")
        onNodeWithText("There's already a page called hero.").assertExists()
        onNodeWithText("Add to the screenplay").assertIsNotEnabled()
        onNode(hasSetTextAction()).performTextReplacement("fights")
        onNodeWithText("screenplay/fights.md", substring = true).assertExists()
        onNodeWithText("Add to the screenplay").performClick()
        assertEquals(listOf<Invocation<*>>(Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input("docs/combat.md", "fights"))), confirmed)
        onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun aPageADocumentBecomesIsOfferedUnderAFreeName() {
        assertEquals("combat", freePageName("combat") { false })
        assertEquals("README 3", freePageName("README") { it == "README" || it == "README 2" })
    }

    @Test
    fun deletingAPageOffersItsImagesOnlyWhenItHasSomeOnlyItShows() = uiTest(width = 700, height = 400) {
        val confirmed = mutableListOf<Invocation<*>>()
        var page by mutableStateOf("hero")
        showing {
            DocumentChangeConfirmation(
                PendingDocumentChange(Invocation(DeletePage, DeletePage.Input(page))),
                unusedImagesOf = { if (it == "hero") listOf("screenplay/hero-1.png", "screenplay/hero-2.png") else emptyList() },
                validate = { null },
                onConfirm = { confirmed += it },
                onDismiss = {},
            )
        }
        onNodeWithText("Also delete the 2 images nothing else shows").assertExists()
        saveSnapshot("page-delete-confirmation")
        onNodeWithText("Also delete the 2 images nothing else shows").performClick()
        onNodeWithText("Delete").performClick()
        assertEquals(listOf<Any>(DeletePage.Input("hero", deleteImages = false)), confirmed.map { it.input })
        page = "villain"
        waitUntil { confirmed.size == 2 }
        assertEquals(DeletePage.Input("villain"), confirmed.last().input)
    }
}
