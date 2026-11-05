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
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object DeleteDocument : WriteCommand<DeleteDocument.Input> {
    override val name: String = "delete_document"
    override val description: String = "Deletes a Markdown document outside the screenplay."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Document(input.document)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateDocument(input.document)

    /**
     * Refuses a file that isn't UTF-8 text, which undo couldn't bring back byte for byte.
     */
    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val text = context.documents.read(input.document) ?: return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_UNREADABLE, input.document))
        context.documents.delete(input.document)
        Applied(inverse = Invocation(CreateDocument, CreateDocument.Input(input.document, text)))
    }

    @Serializable
    public data class Input(
        @Description(DOCUMENT_PATH_DESCRIPTION)
        @SerialName("document") val document: String,
    )
}
