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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PageEditorTest {
    private val page = "# The hero\n\nA knight who jumps between **floating** islands.\n\n## Moves\n\n- Runs\n- Jumps\n\n## Planned\n\n- Wall jump\n"

    @Test
    fun aPageShowsRenderedAndAChunkClickedIsEditedInPlace() = uiTest(width = 900, height = 640) {
        val saved = mutableListOf<String>()
        showing { PageEditor(name = "hero", text = page, onTextChange = { saved += it }) }
        onNodeWithText("The hero").assertExists()
        saveSnapshot("page-editor")
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNodeWithText("## Moves").assertExists()
        onNodeWithText("Bold").assertExists()
        saveSnapshot("page-editor-editing")
        assertEquals(emptyList(), saved)
    }

    @Test
    fun anEmptyPageSaysHowToStartIt() = uiTest(width = 700, height = 300) {
        showing { PageEditor(name = "villain", text = "", onTextChange = {}) }
        onNodeWithText("This page is empty. Add a paragraph to start it.").assertExists()
        onNodeWithText("Add a paragraph").performClick()
        waitForIdle()
        onNodeWithText("Heading").assertExists()
    }
}
