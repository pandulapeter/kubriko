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
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NotATool
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.io.File

/**
 * Gives each page of [Input.pages] showing the image at [Input.path] a copy of it ([Input.bytes]) next to it, as a
 * page's own images are kept, and shows that copy instead: what a deleted sprite leaves of the pages' pictures of it, so
 * they still show what they did. A page that no longer shows it is left alone. Undo writes the pages back byte for byte
 * and deletes the copies.
 */
@OptIn(EditorMutation::class)
internal object KeepPageImages : WriteCommand<KeepPageImages.Input> {
    override val name = "keep_page_images"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("KeepPageImages")

    override fun target(context: CommandContext, input: Input) = input.pages.firstOrNull()?.let(::markdownTargetOf)

    override fun targets(context: CommandContext, input: Input) = input.pages.mapTo(LinkedHashSet(), ::markdownTargetOf)

    override fun validate(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val previous = LinkedHashMap<String, String>()
        val copies = ArrayList<File>()
        try {
            input.pages.forEach { pagePath ->
                val page = PageNaming.nameOf(pagePath) ?: return@forEach
                val text = context.readMarkdown(pagePath) ?: return@forEach
                if (PageLinks.linksOf(text).none { it.isImage && PageLinks.resolve(pagePath, it.destination) == input.path }) return@forEach
                val folder = File(context.project.rootDirectory, pagePath).parentFile
                val taken = folder.list().orEmpty().mapTo(HashSet()) { it.lowercase() }
                val copyPath = PageImages.nextPathOf(page, input.path.substringAfterLast('.').lowercase()) { File(it).name.lowercase() in taken }
                copies += ReplaceFiles.writeNew(context, copyPath, input.bytes)
                context.writeMarkdown(pagePath, PageLinks.withMovesFollowed(text, pagePath, listOf(PathMove(input.path, copyPath))))
                previous[pagePath] = text
            }
        } catch (exception: Exception) {
            previous.forEach { (written, before) -> runCatching { context.writeMarkdown(written, before) }.exceptionOrNull()?.let(exception::addSuppressed) }
            copies.forEach { copy -> runCatching { ReplaceFiles.delete(context, copy) }.exceptionOrNull()?.let(exception::addSuppressed) }
            throw exception
        }
        val restoring = listOfNotNull(
            previous.takeIf { it.isNotEmpty() }?.let { Invocation(RestoreMarkdown, RestoreMarkdown.Input(it)) },
            copies.takeIf { it.isNotEmpty() }?.let { files -> Invocation(ReplaceFiles, ReplaceFiles.Input(files.associate { context.projectPathOf(it) to null })) },
        )
        Applied(inverse = restoring.takeIf { it.isNotEmpty() }?.let { Invocation(CompositeCommand, CompositeCommand.Input(it)) } ?: Invocation(KeepPageImages, Input(input.path, input.bytes, emptyList())))
    }

    /**
     * @param path The image the pages show, a path from the project's root.
     * @param bytes What the image's file holds, read before it is deleted.
     * @param pages The pages to look in, by their paths from the project's root.
     */
    class Input(
        val path: String,
        val bytes: ByteArray,
        val pages: List<String>,
    )
}
