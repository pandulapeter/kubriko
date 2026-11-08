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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NotATool
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * Moves a page out of the screenplay to where it was a document: what undoes [AddDocumentToScreenplay]. Kubriko
 * Creator's own building block; not a tool, since a page is never made a document otherwise.
 */
@OptIn(EditorMutation::class)
internal object MovePageToDocument : WriteCommand<MovePageToDocument.Input> {
    override val name = "move_page_to_document"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("MovePageToDocument")

    override fun target(context: CommandContext, input: Input) =
        if (context.screenplay.existingName(input.page) == input.page) CommandTarget.Page(input.page) else CommandTarget.Document(input.document)

    override fun targets(context: CommandContext, input: Input) = setOf(CommandTarget.Page(input.page), CommandTarget.Document(input.document))

    override fun validate(context: CommandContext, input: Input) = context.validatePage(input.page) ?: context.validateFreeDocumentPath(input.document)

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        if (context.documents.fileOf(input.document).exists()) return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_TAKEN, input.document))
        if (!context.documents.move(PageNaming.pathOf(input.page), input.document)) return@withContext Applied.failed(Rejection(CommandProblem.PAGE_NOT_MOVED, input.page))
        context.screenplay.refresh()
        context.withMentionsFollowed(
            moves = listOf(PathMove(PageNaming.pathOf(input.page), input.document)),
            applied = Applied(inverse = Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input(document = input.document, page = input.page))),
        )
    }

    data class Input(
        val page: String,
        val document: String,
    )
}
