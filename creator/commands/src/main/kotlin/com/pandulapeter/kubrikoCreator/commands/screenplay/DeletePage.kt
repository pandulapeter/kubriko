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
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.projectReferences
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@OptIn(EditorMutation::class)
public object DeletePage : WriteCommand<DeletePage.Input> {
    override val name: String = "delete_page"
    override val description: String = "Deletes a page of the screenplay."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Page(input.page)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val references = context.projectReferences(RelationSubject.Page(input.page))
        val unlinked = references.markdownAfterDeleting(references.usagesOf(RelationSubject.Page(input.page)))
        return withContext(context.ioDispatcher) {
            val text = context.screenplay.read(input.page) ?: return@withContext Applied.failed(Rejection(CommandProblem.PAGE_MISSING, input.page))
            val images = if (input.deleteImages) context.unusedImagesOf(input.page) else emptyList()
            val removed = if (images.isEmpty()) null else ReplaceFiles.apply(context, ReplaceFiles.Input(images.associateWith { null }))
            val unlinking = try {
                context.screenplay.delete(input.page)
                if (unlinked.isEmpty()) null else RestoreMarkdown.apply(context, RestoreMarkdown.Input(unlinked))
            } catch (exception: Exception) {
                if (context.screenplay.read(input.page) == null) runCatching { CreatePage.apply(context, CreatePage.Input(input.page, text)) }.exceptionOrNull()?.let(exception::addSuppressed)
                removed?.inverse?.let { restoring -> runCatching { restoring.apply(context) }.exceptionOrNull()?.let(exception::addSuppressed) }
                throw exception
            }
            val recreating = Invocation(CreatePage, CreatePage.Input(input.page, text))
            unlinking?.failure?.let { failure ->
                recreating.apply(context)
                removed?.inverse?.apply(context)
                return@withContext Applied.failed(failure)
            }
            val inverses = listOfNotNull(unlinking?.inverse, recreating, removed?.inverse)
            Applied(
                inverse = inverses.singleOrNull() ?: Invocation(CompositeCommand, CompositeCommand.Input(inverses)),
                details = if (removed == null && unlinked.isEmpty()) {
                    null
                } else {
                    buildJsonObject {
                        if (removed != null) put("deletedImages", JsonArray(images.map(::JsonPrimitive)))
                        if (unlinked.isNotEmpty()) put("linksToItBecameText", JsonArray(unlinked.keys.map(::JsonPrimitive)))
                    }
                },
            )
        }
    }

    @Serializable
    public data class Input(
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
        @Description("Whether to delete the page's own images too (next to it and named after it) that no other page or document shows.")
        @SerialName("deleteImages") val deleteImages: Boolean = false,
    )
}
