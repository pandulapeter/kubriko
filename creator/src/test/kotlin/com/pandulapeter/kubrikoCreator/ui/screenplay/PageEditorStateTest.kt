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

import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageEditorStateTest {
    private val page = "# The hero\n\nA knight.\n\n## Moves\n\nRuns.\n"
    private val saved = mutableListOf<String>()
    private val state = PageEditorState(page) { saved += it }

    @Test
    fun aChunkIsTypedIntoInItsFrameAndSavedWhole() {
        state.edit(1)
        assertEquals("A knight.", state.session?.draft?.text)
        state.onDraftChange(TextFieldValue("A knight.\n\nWho jumps."))
        state.save()
        assertEquals(listOf("# The hero\n\nA knight.\n\nWho jumps.\n\n## Moves\n\nRuns.\n"), saved)
        state.onPageChanged(saved.last())
        assertEquals("A knight.\n\nWho jumps.", state.session?.draft?.text)
    }

    @Test
    fun goingToAChunkAfterTheOneTypedIntoFindsItInTheSavedPage() {
        state.edit(1)
        state.onDraftChange(TextFieldValue("A knight.\n\nWho jumps."))
        state.edit(state.indexOf(1, isAfter = true))
        assertEquals("Runs.", state.session?.draft?.text)
        assertEquals(1, saved.size)
    }

    @Test
    fun aChangeMadeElsewhereWhileTypingEndsTyping() {
        state.edit(0)
        state.onPageChanged("# Changed by the AI\n")
        assertNull(state.session)
        assertEquals("# Changed by the AI\n", state.text)
    }

    @Test
    fun aNewChunkIsAddedAtTheEndOnlyIfSomethingWasTyped() {
        state.append()
        state.finish()
        assertEquals(emptyList(), saved)
        state.append()
        state.onDraftChange(TextFieldValue("## Planned"))
        state.finish()
        assertEquals(page.trimEnd('\n') + "\n\n## Planned\n", saved.single())
    }
}
