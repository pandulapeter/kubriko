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
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object EditDocument : WriteCommand<EditDocument.Input> {
    override val name: String = "edit_document"
    override val description: String = "Replaces a Markdown document outside the screenplay, or only one of its sections."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Document(input.document)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateDocument(input.document)

    /**
     * Refuses a text past [DOCUMENT_MAXIMUM_LENGTH] only when it is longer than the document was, so the user's own
     * longer document can still be edited, and what undo brings back is never refused.
     */
    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val previous = context.documents.read(input.document) ?: return@withContext Applied.failed(Rejection(CommandProblem.DOCUMENT_UNREADABLE, input.document))
        val text = input.section?.let { section ->
            PageText.withSectionReplaced(previous, section, input.text)
                ?: return@withContext Applied.failed(missingSection(input.document, section, previous))
        } ?: input.text
        if (text.length > previous.length) validateDocumentText(text)?.let { return@withContext Applied.failed(it) }
        context.documents.write(input.document, text)
        Applied(inverse = Invocation(EditDocument, Input(input.document, previous)))
    }

    /**
     * Typing into one document, or one of its sections, is one step to undo.
     */
    override fun mergeKey(input: Input): Any? = Triple(input.document, input.section, name)

    /**
     * @param section The heading of the section [text] replaces, or null for the whole document.
     */
    @Serializable
    public data class Input(
        @Description(DOCUMENT_PATH_DESCRIPTION)
        @SerialName("document") val document: String,
        @Description("The Markdown: the whole document, or the section with its own heading line when section is given.")
        @SerialName("text") val text: String,
        @Description("The heading of the one section to replace (without the # marks), or none for the whole document.")
        @SerialName("section") val section: String? = null,
    )
}
