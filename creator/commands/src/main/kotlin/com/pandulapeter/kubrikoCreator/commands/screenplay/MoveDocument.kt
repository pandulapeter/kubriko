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
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object MoveDocument : WriteCommand<MoveDocument.Input> {
    override val name: String = "move_document"
    override val description: String = "Renames a Markdown document outside the screenplay, or moves it into another folder. " +
        "add_document_to_screenplay moves one into the screenplay."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget =
        CommandTarget.Document(if (context.documents.isKnown(input.newDocument)) input.newDocument else input.document)

    override fun targets(context: CommandContext, input: Input): Set<CommandTarget> = setOf(CommandTarget.Document(input.document), CommandTarget.Document(input.newDocument))

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateDocument(input.document)
        ?: if (input.newDocument == input.document) Rejection(CommandProblem.DOCUMENT_TAKEN, input.newDocument) else context.validateNewDocumentPath(input.newDocument, moved = input.document)

    /**
     * Moves a document back to its path, which the user may have chosen in a way Kubriko Creator wouldn't.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validateDocument(input.document)
        ?: if (ResourceScanner.isDocument(input.newDocument)) {
            context.validateFreeDocumentPath(input.newDocument, moved = input.document)
        } else {
            Rejection(CommandProblem.DOCUMENT_PATH_INVALID, input.newDocument)
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        if (!input.newDocument.equals(input.document, ignoreCase = true) && context.documents.fileOf(input.newDocument).exists()) {
            return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_TAKEN, input.newDocument))
        }
        if (!context.documents.move(input.document, input.newDocument)) return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_NOT_MOVED, input.document))
        context.withMentionsFollowed(listOf(PathMove(input.document, input.newDocument)), Applied(inverse = Invocation(MoveDocument, Input(input.newDocument, input.document))))
    }

    @Serializable
    public data class Input(
        @Description(DOCUMENT_PATH_DESCRIPTION)
        @SerialName("document") val document: String,
        @Description("Its new path from the project's root, with .md, like docs/notes.md.")
        @SerialName("newDocument") val newDocument: String,
    )
}
