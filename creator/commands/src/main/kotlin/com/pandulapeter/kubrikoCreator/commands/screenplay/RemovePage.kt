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
import com.pandulapeter.kubrikoCreator.commands.NotATool
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Deletes a page's file, and its own images when asked, leaving what mentions it as it is: what [DeletePage] does once
 * the mentions are dealt with, and what undoing a page's creation does.
 */
@OptIn(EditorMutation::class)
internal object RemovePage : WriteCommand<DeletePage.Input> {
    override val name = "remove_page"
    override val description = ""
    override val inputSerializer: KSerializer<DeletePage.Input> = NotATool("RemovePage")

    override fun target(context: CommandContext, input: DeletePage.Input) = CommandTarget.Page(input.page)

    override fun validate(context: CommandContext, input: DeletePage.Input): Rejection? = context.validatePage(input.page)

    override suspend fun apply(context: CommandContext, input: DeletePage.Input): Applied = withContext(context.ioDispatcher) {
        val text = context.screenplay.read(input.page) ?: return@withContext Applied.failed(missingPage(input.page, context.screenplay.scanNames()))
        val images = if (input.deleteImages) context.unusedImagesOf(input.page) else emptyList()
        val removed = if (images.isEmpty()) null else ReplaceFiles.apply(context, ReplaceFiles.Input(images.associateWith { null }))
        try {
            context.screenplay.delete(input.page)
        } catch (exception: Exception) {
            removed?.inverse?.let { restoring -> runCatching { restoring.apply(context) }.exceptionOrNull()?.let(exception::addSuppressed) }
            throw exception
        }
        val recreating = Invocation(CreatePage, CreatePage.Input(input.page, text))
        if (removed == null) return@withContext Applied(inverse = recreating)
        Applied(
            inverse = Invocation(CompositeCommand, CompositeCommand.Input(listOfNotNull(recreating, removed.inverse))),
            details = buildJsonObject { put("deletedImages", JsonArray(images.map(::JsonPrimitive))) },
        )
    }
}
