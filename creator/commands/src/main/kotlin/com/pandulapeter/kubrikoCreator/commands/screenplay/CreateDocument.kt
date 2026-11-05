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
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object CreateDocument : WriteCommand<CreateDocument.Input> {
    override val name: String = "create_document"
    override val description: String = "Creates a Markdown document outside the screenplay, in any folder of the project (docs/, or a module's own). " +
        "The game's design goes into the screenplay's pages instead (create_page)."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Document(input.document)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateNewDocumentPath(input.document) ?: validateDocumentText(input.text)

    /**
     * Brings back a deleted document whatever its name and length, which the user may have given it.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? =
        if (ResourceScanner.isDocument(input.document)) context.validateFreeDocumentPath(input.document) else Rejection(CommandProblem.DOCUMENT_PATH_INVALID, input.document)

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        if (!context.documents.create(input.document, input.text)) return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_TAKEN, input.document))
        Applied(inverse = Invocation(DeleteDocument, DeleteDocument.Input(input.document)))
    }

    @Serializable
    public data class Input(
        @Description(DOCUMENT_PATH_DESCRIPTION)
        @SerialName("document") val document: String,
        @Description("The document's Markdown.")
        @SerialName("text") val text: String,
    )
}
