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
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.commands.game.rejection
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException

@OptIn(EditorMutation::class)
public object AddPageImage : WriteCommand<AddPageImage.Input> {
    override val name: String = "add_page_image"
    override val description: String = "Puts an image on a page: a screenshot of the game start_game started, copied next to the page, or a sprite, " +
        "shown as the game draws it now. Show what was built this way."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Page(input.page)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page)
        ?: Rejection(CommandProblem.PAGE_IMAGE_SOURCE).takeIf { listOf(input.sprite != null, input.screenshot, input.file != null).count { it } != 1 }
        ?: input.sprite?.let(context::validateSprite)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val image = when {
            input.sprite != null -> null
            input.screenshot -> when (val step = context.runner.captureScreen()) {
                is GameStep.Done -> step.png?.let { png -> PageImages.encoded(png, "png") } ?: return Applied.failed(Rejection(CommandProblem.GAME_NO_ANSWER))
                else -> return Applied.failed(checkNotNull(step.rejection()))
            }
            else -> withContext(context.ioDispatcher) { readImage(File(checkNotNull(input.file))) } ?: return Applied.failed(Rejection(CommandProblem.PAGE_IMAGE_UNREADABLE, input.file.orEmpty()))
        }
        return withContext(context.ioDispatcher) { addImage(context, input, image) }
    }

    private fun readImage(file: File) = try {
        PageImages.encoded(file.readBytes(), file.extension)
    } catch (_: IOException) {
        null
    }

    private fun addImage(context: CommandContext, input: Input, image: PageImages.Encoded?): Applied {
        val pagePath = PageNaming.pathOf(input.page)
        val text = context.readMarkdown(pagePath) ?: return Applied.failed(Rejection(CommandProblem.DOCUMENT_UNREADABLE, pagePath))
        val root = context.project.rootDirectory
        val imagePath = if (image == null) {
            context.spriteFiles()?.files?.get(input.sprite)?.let(context::projectPathOf) ?: return Applied.failed(Rejection(CommandProblem.SPRITE_MISSING, input.sprite.orEmpty()))
        } else {
            val folder = File(root, pagePath).parentFile
            val taken = folder.list().orEmpty().mapTo(HashSet()) { it.lowercase() }
            PageImages.nextPathOf(input.page, image.extension) { File(it).name.lowercase() in taken }
        }
        val label = input.description.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]").replace("\n", " ")
        val withImage = PageImages.withLine(text, "![$label](${PageLinks.destinationOf(pagePath, imagePath)})", input.section)
            ?: return Applied.failed(missingSection(input.page, input.section.orEmpty(), text))
        val restoring = Invocation(RestoreMarkdown, RestoreMarkdown.Input(mapOf(pagePath to text)))
        if (image == null) {
            context.screenplay.write(input.page, withImage)
            return Applied(inverse = restoring, details = buildJsonObject { put("image", imagePath) })
        }
        val written = ReplaceFiles.writeNew(context, imagePath, image.bytes)
        try {
            context.screenplay.write(input.page, withImage)
        } catch (exception: Exception) {
            runCatching { ReplaceFiles.delete(context, written) }.exceptionOrNull()?.let(exception::addSuppressed)
            throw exception
        }
        val removing = Invocation(ReplaceFiles, ReplaceFiles.Input(mapOf(imagePath to null)))
        return Applied(inverse = Invocation(CompositeCommand, CompositeCommand.Input(listOf(restoring, removing))), details = buildJsonObject { put("image", imagePath) })
    }

    @Serializable
    public data class Input(
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
        @Description("What the image shows, in a few words: its text for readers who can't see it.")
        @SerialName("description") val description: String,
        @Description("A sprite, by its path from composeResources, like drawable/ball.png. Give this, screenshot, or file.")
        @SerialName("sprite") val sprite: String? = null,
        @Description("Whether to show what the game start_game started shows right now.")
        @SerialName("screenshot") val screenshot: Boolean = false,
        @Description("An image file on this computer to copy next to the page, by its full path.")
        @SerialName("file") val file: String? = null,
        @Description("The heading of the section to end with it (without #), or none for the end of the page, before Planned.")
        @SerialName("section") val section: String? = null,
    )
}
