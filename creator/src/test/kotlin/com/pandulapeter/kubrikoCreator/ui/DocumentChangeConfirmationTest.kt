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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
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
        showing { DocumentChangeConfirmation(PendingDocumentChange(Invocation(DeleteDocument, DeleteDocument.Input("docs/combat.md"))), onConfirm = { confirmed++ }, onDismiss = {}) }
        onNodeWithText("Delete docs/combat.md?").assertExists()
        saveSnapshot("document-delete-confirmation")
        onNodeWithText("Delete").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun addingADocumentToTheScreenplayNamesThePageItBecomes() = uiTest(width = 700, height = 400) {
        var dismissed = 0
        showing {
            DocumentChangeConfirmation(
                PendingDocumentChange(Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input("docs/combat.md", "combat"))),
                onConfirm = {},
                onDismiss = { dismissed++ },
            )
        }
        onNodeWithText("Add docs/combat.md to the screenplay?").assertExists()
        onNodeWithText("screenplay/combat.md", substring = true).assertExists()
        saveSnapshot("document-add-to-screenplay-confirmation")
        onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed)
    }
}
