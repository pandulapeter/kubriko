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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object DeleteFolder : WriteCommand<DeleteFolder.Input> {
    override val name: String = "delete_folder"
    override val description: String = "Deletes an empty folder of Actor types, Manager types, or Scenes. Move what is in it out first."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateFolder(input.group, input.path) ?: when {
        input.path.isEmpty() -> Rejection(CommandProblem.FOLDER_NOT_EMPTY, input.path)
        !context.isFolderEmpty(input.group, input.path) -> Rejection(CommandProblem.FOLDER_NOT_EMPTY, input.path)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.deleteFolder(input.group, input.path)
        return Applied(inverse = Invocation(CreateFolder, CreateFolder.Input(input.group, input.path)))
    }

    @Serializable
    public data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path: folder names separated by slashes, like enemies/flying.")
        @SerialName("path") val path: String,
    )
}
