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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

public object ViewPageImage : ReadCommand<ViewPageImage.Input> {
    override val name: String = "view_page_image"
    override val description: String = "Shows an image a page shows, by its path as read_pages lists it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val root = context.project.rootDirectory.canonicalFile
        val file = File(root, input.image).canonicalFile
        val isInProject = file.path.startsWith(root.path + File.separator)
        if (!isInProject || file.extension.lowercase() !in PageImages.EXTENSIONS) return CommandOutcome.Rejected(Rejection(CommandProblem.PAGE_IMAGE_UNREADABLE, input.image))
        val png = withContext(context.ioDispatcher) { pngOf(file) } ?: return CommandOutcome.Rejected(Rejection(CommandProblem.PAGE_IMAGE_UNREADABLE, input.image))
        return CommandOutcome.Success(JsonPrimitive(input.image), image = png)
    }

    /**
     * The image in [file] as a PNG no wider than [MAXIMUM_WIDTH], or null if it can't be read.
     */
    private fun pngOf(file: File): ByteArray? {
        val image = try {
            ImageIO.read(file)
        } catch (_: IOException) {
            null
        } ?: return null
        if (image.width <= MAXIMUM_WIDTH && file.extension.equals("png", ignoreCase = true)) return file.readBytes()
        val scale = (MAXIMUM_WIDTH.toFloat() / image.width).coerceAtMost(1f)
        val result = BufferedImage((image.width * scale).toInt().coerceAtLeast(1), (image.height * scale).toInt().coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
        result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(image, 0, 0, result.width, result.height, null)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(result, "png", it) }.toByteArray()
    }

    private const val MAXIMUM_WIDTH = 1280

    @Serializable
    public data class Input(
        @Description("The image's path from the project's root, as read_pages lists it.")
        @SerialName("image") val image: String,
    )
}
