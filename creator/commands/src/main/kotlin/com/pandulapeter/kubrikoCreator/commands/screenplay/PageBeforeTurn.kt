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
 * [sinceRevision], as undoing them ([com.pandulapeter.kubrikoCreator.commands.CommandExecutor.undoTurn]) brings it back:
 * only the conversation's changes newer than anyone else's count, as only those are undone. An empty text stands for a page
 * they created, and null for one they didn't change. The page is named as it is now, so one they moved is followed back
 * to where it was.
 */
public fun CommandHistory.pageBeforeTurn(conversationId: String, sinceRevision: Int, page: String): String? {
    val tracked = TrackedPage(page)
    for (entry in undoable.asReversed()) {
        if (entry.conversationId != conversationId || entry.revision <= sinceRevision) break
        tracked.revert(entry.revert)
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
            is EditPage.Input -> if (isTracked(input.page) && input.section == null) before = input.text
            is CreatePage.Input -> if (isTracked(input.page)) before = input.text
            is DeletePage.Input -> if (isTracked(input.page)) before = ""
            is MovePageToDocument.Input -> if (isTracked(input.page)) before = ""
            is MovePage.Input -> if (isTracked(input.page)) name = input.newPage
            is RestoreMarkdown.Input -> input.contents[PageNaming.pathOf(name)]?.let { before = it }
        }
    }

    private fun isTracked(page: String) = page == name
}
