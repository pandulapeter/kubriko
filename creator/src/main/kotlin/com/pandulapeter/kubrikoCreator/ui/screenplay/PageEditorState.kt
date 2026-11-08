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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.pandulapeter.kubrikoCreator.project.withUnixLineBreaks
import com.pandulapeter.kubrikoCreator.screenplay.PageChunks
import com.pandulapeter.kubrikoCreator.screenplay.PageText

/**
 * What the page editor edits: the page as last known, and the one chunk being typed into ([Session]), framed by the text
 * around it, so that the chunks around it stay where they are while it is typed into, blank lines and all.
 *
 * The page is held with `\n` line breaks, since chunks are; the store writes the page's own back.
 *
 * @param onTextChange Saves the whole page, which arrives back through [onPageChanged].
 */
internal class PageEditorState(
    text: String,
    var onTextChange: (String) -> Unit,
) {
    /**
     * @param isNew Whether the chunk is a new one at the end not saved yet, which is left out if nothing was typed into it.
     */
    data class Session(
        val frame: PageChunks.Frame,
        val draft: TextFieldValue,
        val isNew: Boolean = false,
    )

    var session: Session? by mutableStateOf(null)
        private set
    var text: String by mutableStateOf(text.withUnixLineBreaks())
        private set
    private var lastSaved: String? = null

    /**
     * Follows the page as it is on disk: a change made elsewhere while a chunk is being typed into ends that, since the
     * frame around it no longer matches.
     */
    fun onPageChanged(changed: String) {
        val page = changed.withUnixLineBreaks()
        if (page == text) return
        if (session != null && page != lastSaved) session = null
        text = page
    }

    /**
     * Starts typing into chunk [index] of the page as it is now, ending the chunk typed into before.
     */
    fun edit(index: Int) {
        finish()
        val chunk = PageChunks.of(text).getOrNull(index) ?: return
        session = Session(PageChunks.frameOf(text, chunk), TextFieldValue(chunk.text, TextRange(chunk.text.length)))
    }

    /**
     * Starts typing a new chunk at the end of the page.
     */
    fun append() {
        finish()
        session = Session(PageChunks.appendingTo(text), TextFieldValue(""), isNew = true)
    }

    fun onDraftChange(draft: TextFieldValue) {
        session = session?.copy(draft = draft)
    }

    /**
     * Saves what was typed, if anything changed.
     */
    fun save() {
        val current = session ?: return
        if (current.isNew && current.draft.text.isBlank()) return
        val composed = current.frame.compose(current.draft.text)
        // Once saved, a new chunk is part of the page: clearing it removes it like any other.
        if (current.isNew) session = current.copy(isNew = false)
        if (composed == text) return
        text = composed
        lastSaved = composed
        onTextChange(composed)
    }

    /**
     * Saves what was typed and stops typing.
     */
    fun finish() {
        save()
        session = null
    }

    /**
     * The heading of the section the chunk being typed into is in, or null when none is being typed into or it is in
     * none: where an image put on the page now goes.
     */
    fun currentSection(): String? {
        val current = session ?: return null
        return PageText.sectionsOf(current.frame.before + current.draft.text).lastOrNull()?.heading
    }

    /**
     * The index in the page, once what is typed is saved, of chunk [index] of the text before the one being typed into,
     * or of chunk [index] of the text after it if [isAfter].
     */
    fun indexOf(index: Int, isAfter: Boolean): Int {
        val current = session ?: return index
        if (!isAfter) return index
        return PageChunks.of(current.frame.before).size + PageChunks.of(current.draft.text).size + index
    }
}
