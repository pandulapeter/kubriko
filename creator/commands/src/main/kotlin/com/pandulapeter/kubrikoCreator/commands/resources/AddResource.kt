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
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

@OptIn(EditorMutation::class)
public object AddResource : WriteCommand<AddResource.Input> {
    override val name: String = "add_resource"
    override val description: String = "Copies a sprite, sound, or music file from this computer into composeResources, for the game to use by its path."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateNewResource(input.path) ?: run {
        val extension = File(input.file).extension
        if (extension.equals(input.path.substringAfterLast('.'), ignoreCase = true)) null else Rejection(CommandProblem.RESOURCE_SOURCE_EXTENSION, input.file, extension)
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val failure = withContext(context.ioDispatcher) {
            val bytes = try {
                File(input.file).readBytes()
            } catch (_: IOException) {
                return@withContext Rejection(CommandProblem.RESOURCE_SOURCE_UNREADABLE, input.file)
            }
            when {
                !ResourceKind.isContentOf(bytes, input.path.substringAfterLast('.')) -> Rejection(CommandProblem.RESOURCE_CONTENT_MISMATCH, input.file, input.path.substringAfterLast('.').lowercase())
                context.isResourceTakenOnDisk(input.path) -> Rejection(CommandProblem.RESOURCE_TAKEN, input.path)
                else -> {
                    ReplaceFiles.writeNew(context, context.projectPathOf(File(context.scenes.resourcesDirectory, input.path)), bytes)
                    null
                }
            }
        }
        return if (failure == null) Applied(inverse = Invocation(DeleteResource, DeleteResource.Input(input.path, keepsUsages = true))) else Applied.failed(failure)
    }

    @Serializable
    public data class Input(
        @Description("The file to copy, by its full path on this computer.")
        @SerialName("file") val file: String,
        @Description("Its path from composeResources, with the file's extension: drawable/ball.png for a sprite, files/sounds/hit.wav or files/music/theme.mp3.")
        @SerialName("path") val path: String,
    )
}
