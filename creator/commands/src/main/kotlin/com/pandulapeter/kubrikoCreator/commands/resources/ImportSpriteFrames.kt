/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

@OptIn(EditorMutation::class)
public object ImportSpriteFrames : WriteCommand<ImportSpriteFrames.Input> {
    override val name: String = "import_sprite_frames"
    override val description: String = "Makes a new sprite of images from this computer, one frame each, in their order: a sheet of one row, cut into " +
        "those frames (set_sprite_sheet). The images must be of one size."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        input.files.isEmpty() -> Rejection(SpriteProblem.FRAMES_NONE)
        ResourceKind.of(input.path) != ResourceKind.SPRITE || !input.path.endsWith(".png") -> Rejection(SpriteProblem.SPRITE_PATH_INVALID, input.path)
        else -> context.validateNewResource(input.path)
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val failure = withContext(context.ioDispatcher) {
            val images = input.files.map { path ->
                val image = try {
                    PixelImage.of(File(path).readBytes())
                } catch (_: IOException) {
                    null
                }
                image ?: return@withContext Rejection(CommandProblem.RESOURCE_SOURCE_UNREADABLE, path)
            }
            val first = images.first()
            images.indexOfFirst { it.width != first.width || it.height != first.height }.takeIf { it >= 0 }?.let { index ->
                return@withContext Rejection(SpriteProblem.FRAMES_DIFFER_IN_SIZE, input.files[index], first.width.toString(), first.height.toString())
            }
            if (first.width * images.size > EditSprite.MAXIMUM_SIZE) return@withContext Rejection(SpriteProblem.SPRITE_SIZE_INVALID, EditSprite.MAXIMUM_SIZE.toString())
            if (context.resourceFilesOnDisk().isTaken(input.path)) return@withContext Rejection(CommandProblem.RESOURCE_TAKEN, input.path)
            val sheet = SpriteSheet(frameWidth = first.width, frameHeight = first.height, frameCount = images.size, framesPerRow = images.size)
            val strip = images.foldIndexed(SpriteFrames.of(PixelImage(first.width * images.size, first.height), sheet)) { index, frames, image -> frames.withFrame(index, image) }
            val file = try {
                ReplaceFiles.writeNew(context, context.projectPathOf(File(context.scenes.resourcesDirectory, input.path)), strip.toImage().toPng())
            } catch (exception: IOException) {
                return@withContext Rejection(SpriteProblem.SPRITE_NOT_WRITTEN, input.path, exception.message ?: exception.toString())
            }
            try {
                context.spriteFolders.setSheet(ResourceKind.nameOf(input.path), sheet.takeIf { images.size > 1 })
            } catch (exception: IOException) {
                ReplaceFiles.delete(context, file)
                return@withContext Rejection(SpriteProblem.SPRITE_SHEET_NOT_WRITTEN, input.path, exception.message ?: exception.toString())
            }
            null
        }
        return if (failure == null) Applied(inverse = Invocation(DeleteResource, DeleteResource.Input(input.path, keepsUsages = true))) else Applied.failed(failure)
    }

    @Serializable
    public data class Input(
        @Description("The images, by their full paths on this computer, one frame each, in order.")
        @SerialName("files") val files: List<String>,
        @Description("The new sprite's path from composeResources, like drawable/hero-walk.png.")
        @SerialName("path") val path: String,
    )
}
