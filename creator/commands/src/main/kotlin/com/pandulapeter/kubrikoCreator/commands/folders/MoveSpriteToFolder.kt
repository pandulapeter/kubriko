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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.resources.validateExistingResource
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

@OptIn(EditorMutation::class)
public object MoveSpriteToFolder : WriteCommand<MoveSpriteToFolder.Input> {
    override val name: String = "move_sprite_to_folder"
    override val description: String = "Moves a sprite into a folder of the Sprites group, or out of every folder. Only the sidebar changes: the image stays in drawable."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePutInVirtualFolder(FolderGroup.SPRITES, input.sprite, input.folder)

    override suspend fun apply(context: CommandContext, input: Input): Applied =
        context.putInVirtualFolder(FolderGroup.SPRITES, input.sprite, input.folder) { folder -> Invocation(MoveSpriteToFolder, input.copy(folder = folder)) }

    @Serializable
    public data class Input(
        @Description("The sprite's path from composeResources, like drawable/ball.png.")
        @SerialName("sprite") val sprite: String,
        @Description("The folder's path (folder names separated by slashes, like enemies/flying), or an empty text for none.")
        @SerialName("folder") val folder: String,
    )
}
