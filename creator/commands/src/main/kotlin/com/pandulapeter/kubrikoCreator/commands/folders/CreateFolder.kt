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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object CreateFolder : WriteCommand<CreateFolder.Input> {
    override val name: String = "create_folder"
    override val description: String = "Creates a folder in the sidebar for Actor types, Manager types, Scenes, pages, sprites, sounds, or music, inside another one if the path has several names."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateNewFolder(input.group, input.path)

    /**
     * Accepts a folder that is still there: deleting one leaves its directory while a file nothing lists is in it (a
     * `.DS_Store`, or an image another page shows), and undoing that delete has to go on.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? =
        if (input.path in context.foldersOf(input.group)) null else validate(context, input)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.createFolder(input.group, input.path)
        return Applied(inverse = Invocation(DeleteFolder, DeleteFolder.Input(input.group, input.path, keepsContents = true)))
    }

    @Serializable
    public data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The new folder's path: folder names separated by slashes, like enemies/flying, the last one being the new folder (for audio, from composeResources/files).")
        @SerialName("path") val path: String,
    )
}
