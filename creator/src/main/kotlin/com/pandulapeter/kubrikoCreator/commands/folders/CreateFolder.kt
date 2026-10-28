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
import kubriko.creator.generated.resources.command_create_folder

@OptIn(EditorMutation::class)
internal object CreateFolder : WriteCommand<CreateFolder.Input> {
    override val name = "create_folder"
    override val description = "Creates a folder in the sidebar for Actor types or Manager types, inside another one if the path has several names."
    override val title = Res.string.command_create_folder
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateNewFolder(input.group, input.path)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireBlockTypes.createFolder(input.group.kind, input.path)
        return Applied(inverse = Invocation(DeleteFolder, DeleteFolder.Input(input.group, input.path)))
    }

    @Serializable
    data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The new folder's path: folder names separated by slashes, like enemies/flying, the last one being the new folder.")
        @SerialName("path") val path: String,
    )
}
