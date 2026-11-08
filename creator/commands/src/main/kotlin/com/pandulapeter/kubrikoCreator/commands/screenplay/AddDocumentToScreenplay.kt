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
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object AddDocumentToScreenplay : WriteCommand<AddDocumentToScreenplay.Input> {
    override val name: String = "add_document_to_screenplay"
    override val description: String = "Moves a Markdown document into the screenplay as a page, for a document that describes the game's design. " +
        "The file is the user's, so only when they asked for it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget =
        if (context.documents.isKnown(input.document)) CommandTarget.Document(input.document) else CommandTarget.Page(input.page)

    override fun targets(context: CommandContext, input: Input): Set<CommandTarget> = setOf(CommandTarget.Document(input.document), CommandTarget.Page(input.page))

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateDocument(input.document) ?: context.validateNewPageName(input.page)

    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validateDocument(input.document)
        ?: context.screenplay.existingName(input.page)?.let { Rejection(CommandProblem.PAGE_TAKEN, input.page) }

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        if (context.screenplay.scanExistingName(input.page) != null) return@withContext Applied.failed(Rejection(CommandProblem.PAGE_TAKEN, input.page))
        val text = context.documents.read(input.document) ?: return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_UNREADABLE, input.document))
        validatePageText(text)?.let { return@withContext Applied.failed(it) }
        if (!context.documents.move(input.document, PageNaming.pathOf(input.page))) return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_NOT_MOVED, input.document))
        context.screenplay.refresh()
        context.withMentionsFollowed(
            moves = listOf(PathMove(input.document, PageNaming.pathOf(input.page))),
            applied = Applied(inverse = Invocation(MovePageToDocument, MovePageToDocument.Input(page = input.page, document = input.document))),
        )
    }

    @Serializable
    public data class Input(
        @Description(DOCUMENT_PATH_DESCRIPTION)
        @SerialName("document") val document: String,
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
    )
}
