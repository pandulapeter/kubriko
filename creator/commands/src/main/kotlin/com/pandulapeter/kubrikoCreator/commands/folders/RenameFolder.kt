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
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.scenes.sceneNotMoved
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object RenameFolder : WriteCommand<RenameFolder.Input> {
    override val name: String = "rename_folder"
    override val description: String = "Renames a folder of Actor types, Manager types, or Scenes, keeping everything in it where it is in it. A Scene's name " +
        "is its path, so renaming its folder renames the Scene, and every goToScene Block follows."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateFolder(input.group, input.path) ?: when {
        input.path.isEmpty() || FolderPaths.validateName(input.newName) != null -> Rejection(NameProblem.FOLDER_INVALID)
        else -> context.validateNewFolder(input.group, renamed(input))
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val newPath = renamed(input)
        context.moveFolder(input.group, input.path, newPath)?.let { scene -> return Applied(inverse = null, failure = sceneNotMoved(scene)) }
        return Applied(inverse = Invocation(RenameFolder, Input(input.group, newPath, FolderPaths.nameOf(input.path))))
    }

    private fun renamed(input: Input) = FolderPaths.child(FolderPaths.parentOf(input.path), input.newName)

    @Serializable
    public data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path: folder names separated by slashes, like enemies/flying.")
        @SerialName("path") val path: String,
        @Description("Its new name (only the last part of the path changes).")
        @SerialName("newName") val newName: String,
    )
}
