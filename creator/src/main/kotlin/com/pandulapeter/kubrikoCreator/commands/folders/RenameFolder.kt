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
import kubriko.creator.generated.resources.command_rename_folder
import kubriko.creator.generated.resources.error_folder_name_invalid

@OptIn(EditorMutation::class)
internal object RenameFolder : WriteCommand<RenameFolder.Input> {
    override val name = "rename_folder"
    override val description = "Renames a folder of Actor types or Manager types, keeping everything in it (types and folders) where it is in it."
    override val title = Res.string.command_rename_folder
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateFolder(input.group, input.path) ?: when {
        input.path.isEmpty() || FolderPaths.validateName(input.newName) != null -> Rejection(Res.string.error_folder_name_invalid)
        else -> context.validateNewFolder(input.group, renamed(input))
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val blockTypes = context.requireBlockTypes
        val newPath = renamed(input)
        fun moved(path: String) = newPath + path.removePrefix(input.path)
        context.foldersOf(input.group).filter { FolderPaths.isWithin(it, input.path) }.forEach { blockTypes.createFolder(input.group.kind, moved(it)) }
        blockTypes.types.value.filter { it.kind == input.group.kind && FolderPaths.isWithin(it.folder, input.path) }.forEach { blockTypes.put(it.withFolder(moved(it.folder))) }
        blockTypes.deleteFolder(input.group.kind, input.path)
        return Applied(inverse = Invocation(RenameFolder, Input(input.group, newPath, FolderPaths.nameOf(input.path))))
    }

    private fun renamed(input: Input) = FolderPaths.child(FolderPaths.parentOf(input.path), input.newName)

    @Serializable
    data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path: folder names separated by slashes, like enemies/flying.")
        @SerialName("path") val path: String,
        @Description("Its new name (only the last part of the path changes).")
        @SerialName("newName") val newName: String,
    )
}
