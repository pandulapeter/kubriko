/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_delete_folder
import kubriko.creator.generated.resources.error_folder_not_empty

@OptIn(EditorMutation::class)
internal object DeleteFolder : WriteCommand<DeleteFolder.Input> {
    override val name = "delete_folder"
    override val description = "Deletes an empty folder of Actor types or Manager types. Move what is in it out first."
    override val title = Res.string.command_delete_folder
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateFolder(input.group, input.path) ?: when {
        input.path.isEmpty() -> Rejection(Res.string.error_folder_not_empty, input.path)
        context.requireBlockTypes.types.value.any { it.kind == input.group.kind && FolderPaths.isWithin(it.folder, input.path) } ||
            context.foldersOf(input.group).any { it != input.path && FolderPaths.isWithin(it, input.path) } -> Rejection(Res.string.error_folder_not_empty, input.path)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireBlockTypes.deleteFolder(input.group.kind, input.path)
        return Applied(inverse = Invocation(CreateFolder, CreateFolder.Input(input.group, input.path)))
    }

    @Serializable
    data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path: folder names separated by slashes, like enemies/flying.")
        @SerialName("path") val path: String,
    )
}
