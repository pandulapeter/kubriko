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
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_move_to_folder

@OptIn(EditorMutation::class)
internal object MoveToFolder : WriteCommand<MoveToFolder.Input> {
    override val name = "move_to_folder"
    override val description = "Moves an Actor type or a Manager type into a folder of its group, or out of every folder. Only the sidebar changes: " +
        "the type keeps its name and everything that refers to it. Scenes move with rename_scene."
    override val title = Res.string.command_move_to_folder
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.type)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateType(input.type)
        ?: context.validateFolder(FolderGroup.of(context.requireType(input.type).kind), input.folder)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.type)
        context.requireBlockTypes.put(definition.withFolder(input.folder))
        return Applied(inverse = Invocation(MoveToFolder, input.copy(folder = definition.folder)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type or Manager type.")
        @SerialName("type") val type: String,
        @Description("The folder's path (folder names separated by slashes, like enemies/flying), or an empty text for none.")
        @SerialName("folder") val folder: String,
    )
}
