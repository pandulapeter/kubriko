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
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * Writes pages and documents back as they were, by their paths from the project's root: how undoing a move puts back the
 * links that followed it ([withMentionsFollowed]), byte for byte.
 */
@OptIn(EditorMutation::class)
internal object RestoreMarkdown : WriteCommand<RestoreMarkdown.Input> {
    override val name = "restore_markdown"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("RestoreMarkdown")

    override fun target(context: CommandContext, input: Input) = targetOf(input.contents.keys.first())

    override fun targets(context: CommandContext, input: Input) = input.contents.keys.mapTo(LinkedHashSet(), ::targetOf)

    override fun validate(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val previous = LinkedHashMap<String, String>()
        try {
            input.contents.forEach { (path, text) ->
                val current = context.readMarkdown(path)
                if (current == null) {
                    previous.forEach { (written, before) -> context.writeMarkdown(written, before) }
                    return@withContext Applied.failed(Rejection(CommandProblem.MARKDOWN_MISSING, path))
                }
                context.writeMarkdown(path, text)
                previous[path] = current
            }
        } catch (exception: Exception) {
            previous.forEach { (written, before) -> runCatching { context.writeMarkdown(written, before) }.exceptionOrNull()?.let(exception::addSuppressed) }
            throw exception
        }
        Applied(inverse = Invocation(RestoreMarkdown, Input(previous)))
    }

    private fun targetOf(path: String) = pageOf(path)?.let { CommandTarget.Page(it) } ?: CommandTarget.Document(path)

    /**
     * @param contents The whole text of each page or document, by its path from the project's root.
     */
    data class Input(
        val contents: Map<String, String>,
    ) {
        init {
            require(contents.isNotEmpty()) { "Restoring Markdown needs at least one file." }
        }
    }
}
