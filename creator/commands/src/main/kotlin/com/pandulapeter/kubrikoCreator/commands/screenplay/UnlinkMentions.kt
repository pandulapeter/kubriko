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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NotATool
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Turns the links of the pages [Input.files] to the project's file [Input.path] into plain text, as each file
 * is when it applies, so that nothing written to them meanwhile is lost: what a delete leaves of the mentions of what it
 * deleted. Its details name the files it changed ([UNLINKED]); undo writes them back byte for byte ([RestoreMarkdown]).
 */
@OptIn(EditorMutation::class)
internal object UnlinkMentions : WriteCommand<UnlinkMentions.Input> {
    override val name = "unlink_mentions"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("UnlinkMentions")

    /**
     * The details' key of the files changed.
     */
    const val UNLINKED = "unlinked"

    override fun target(context: CommandContext, input: Input) = input.files.firstOrNull()?.let(::markdownTargetOf)

    override fun targets(context: CommandContext, input: Input) = input.files.mapTo(LinkedHashSet(), ::markdownTargetOf)

    override fun validate(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val previous = LinkedHashMap<String, String>()
        try {
            input.files.forEach { file ->
                val text = context.readMarkdown(file) ?: return@forEach
                val unlinked = PageLinks.withLinksToPathAsText(text, file, input.path)
                if (unlinked != text) {
                    context.writeMarkdown(file, unlinked)
                    previous[file] = text
                }
            }
        } catch (exception: Exception) {
            previous.forEach { (written, before) -> runCatching { context.writeMarkdown(written, before) }.exceptionOrNull()?.let(exception::addSuppressed) }
            throw exception
        }
        Applied(
            // Nothing changed is undone by doing nothing, which this does again.
            inverse = if (previous.isEmpty()) Invocation(UnlinkMentions, Input(input.path, emptyList())) else Invocation(RestoreMarkdown, RestoreMarkdown.Input(previous)),
            details = buildJsonObject { put(UNLINKED, JsonArray(previous.keys.map(::JsonPrimitive))) },
        )
    }

    /**
     * @param path The file the links to turn into text point at, a path from the project's root.
     * @param files The pages to look in, by their paths from the project's root.
     */
    data class Input(
        val path: String,
        val files: List<String>,
    )
}
