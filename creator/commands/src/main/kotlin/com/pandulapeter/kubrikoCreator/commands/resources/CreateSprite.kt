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
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.validateFolder
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

@OptIn(EditorMutation::class)
public object CreateSprite : WriteCommand<CreateSprite.Input> {
    override val name: String = "create_sprite"
    override val description: String = "Makes a new sprite: a see-through PNG of a size, right in drawable, to draw on with draw_pixels."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        ResourceKind.of(input.path) != ResourceKind.SPRITE || !input.path.endsWith(".png") -> Rejection(CommandProblem.SPRITE_PATH_INVALID, input.path)
        input.width !in 1..EditSprite.MAXIMUM_SIZE || input.height !in 1..EditSprite.MAXIMUM_SIZE -> Rejection(CommandProblem.SPRITE_SIZE_INVALID, EditSprite.MAXIMUM_SIZE.toString())
        else -> context.validateNewResource(input.path) ?: context.validateFolder(FolderGroup.SPRITES, input.folder)
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val failure = withContext(context.ioDispatcher) {
            if (context.resourceFilesOnDisk().isTaken(input.path)) return@withContext Rejection(CommandProblem.RESOURCE_TAKEN, input.path)
            val file = try {
                ReplaceFiles.writeNew(context, context.projectPathOf(File(context.scenes.resourcesDirectory, input.path)), PixelImage(input.width, input.height).toPng())
            } catch (exception: IOException) {
                return@withContext Rejection(CommandProblem.SPRITE_NOT_WRITTEN, input.path, exception.message ?: exception.toString())
            }
            if (input.folder.isNotEmpty()) {
                try {
                    context.spriteFolders.move(ResourceKind.nameOf(input.path), input.folder)
                } catch (exception: IOException) {
                    ReplaceFiles.delete(context, file)
                    return@withContext Rejection(CommandProblem.SPRITE_NOT_WRITTEN, input.path, exception.message ?: exception.toString())
                }
            }
            null
        }
        return if (failure == null) Applied(inverse = Invocation(DeleteResource, DeleteResource.Input(input.path, keepsUsages = true))) else Applied.failed(failure)
    }

    @Serializable
    public data class Input(
        @Description("Its path from composeResources: drawable/, a name of lowercase letters, digits, and dashes, and .png, like drawable/ship.png.")
        @SerialName("path") val path: String,
        @Description("Its width, in pixels.")
        @SerialName("width") val width: Int = 32,
        @Description("Its height, in pixels.")
        @SerialName("height") val height: Int = 32,
        @Description("The folder of the Sprites group to put it in, or an empty text for none.")
        @SerialName("folder") val folder: String = "",
    )
}
