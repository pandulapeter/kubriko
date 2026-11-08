/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming

/**
 * What the page [page] held before the changes the conversation at the tool address [conversationId] made after
 * [sinceRevision], which undoing them brings back: an empty text for a page they created, or null when they didn't
 * change it. The page is named as it is now, so one they moved is followed back to where it was.
 */
public fun CommandHistory.pageBeforeTurn(conversationId: String, sinceRevision: Int, page: String): String? {
    val tracked = TrackedPage(page)
    undoable.asReversed().forEach { entry ->
        if (entry.conversationId == conversationId && entry.revision > sinceRevision) tracked.revert(entry.revert)
    }
    return tracked.before
}

/**
 * A page followed back through the reverts of a turn's changes, newest first, as undoing them would apply them.
 */
private class TrackedPage(var name: String) {
    var before: String? = null

    fun revert(invocation: Invocation<*>) {
        when (val input = invocation.input) {
            is CompositeCommand.Input -> input.parts.forEach(::revert)
            is EditPage.Input -> if (input.page == name && input.section == null) before = input.text
            is CreatePage.Input -> if (input.page == name) before = input.text
            is DeletePage.Input -> if (input.page == name) before = ""
            is MovePage.Input -> if (input.page == name) name = input.newPage
            is RestoreMarkdown.Input -> input.contents[PageNaming.pathOf(name)]?.let { before = it }
        }
    }
}
