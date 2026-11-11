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
import kotlinx.serialization.Transient

@OptIn(EditorMutation::class)
public object MoveFolder : WriteCommand<MoveFolder.Input> {
    override val name: String = "move_folder"
    override val description: String = "Moves a folder, with everything in it, into another folder of its group, or out of every folder. What follows is " +
        "as for rename_folder."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateFolder(input.group, input.path)
        ?: when {
            input.path.isEmpty() -> Rejection(CommandProblem.FOLDER_TOP)
            FolderPaths.isWithin(input.folder, input.path) -> Rejection(CommandProblem.FOLDER_INTO_ITSELF, input.path)
            FolderPaths.parentOf(input.path) == input.folder -> Rejection(CommandProblem.FOLDER_ALREADY_THERE, input.path)
            else -> context.validateFolderRelocation(input.group, input.path, input.newPath)
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val newPath = input.newPath
        return context.relocateFolder(input.group, input.path, newPath, input.movesOnly) { movesOnly ->
            Invocation(MoveFolder, Input(input.group, newPath, FolderPaths.parentOf(input.path), movesOnly))
        }
    }

    /**
     * @param movesOnly Whether an audio folder's files only move, leaving what uses them: how undo and redo move them,
     *   while the steps beside it put back exactly what the move changed.
     */
    @Serializable
    public data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path, as for rename_folder.")
        @SerialName("path") val path: String,
        @Description("The folder to move it into, or empty for none (for audio, files itself).")
        @SerialName("folder") val folder: String,
        @Transient val movesOnly: Boolean = false,
    ) {
        /**
         * Where the folder is once it moved.
         */
        val newPath: String get() = FolderPaths.child(folder, FolderPaths.nameOf(path))
    }
}
