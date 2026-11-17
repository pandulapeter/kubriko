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
public object MoveFontToFolder : WriteCommand<MoveFontToFolder.Input> {
    override val name: String = "move_font_to_folder"
    override val description: String = "Moves a font into a folder of the Fonts group, or out of every folder. Only the sidebar changes: the file stays in font."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePutInVirtualFolder(FolderGroup.FONTS, input.font, input.folder)

    override suspend fun apply(context: CommandContext, input: Input): Applied =
        context.putInVirtualFolder(FolderGroup.FONTS, input.font, input.folder) { folder -> Invocation(MoveFontToFolder, input.copy(folder = folder)) }

    @Serializable
    public data class Input(
        @Description("The font's path from composeResources, like font/pixel.ttf.")
        @SerialName("font") val font: String,
        @Description("The folder's path (folder names separated by slashes, like enemies/flying), or an empty text for none.")
        @SerialName("folder") val folder: String,
    )
}
