/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_rename_manager_type

@OptIn(EditorMutation::class)
internal object RenameManagerType : WriteCommand<RenameManagerType.Input> {
    override val name = "rename_manager_type"
    override val description = "Renames a block-built Manager type, its generated Kotlin class, and every Block, log message, and text of every type that uses its Properties."
    override val title = Res.string.command_rename_manager_type
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(if (context.blockTypes?.managerType(input.newName) != null) input.newName else input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType)
        ?: BlockTypeNaming.validate(input.newName, context.requireBlockTypes.types.value.map { it.name })?.let(::Rejection)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireBlockTypes.rename(input.managerType, input.newName)
        return Applied(inverse = Invocation(RenameManagerType, Input(managerType = input.newName, newName = input.managerType)))
    }

    @Serializable
    data class Input(
        @Description("The current name of the Manager type.")
        @SerialName("managerType") val managerType: String,
        @Description("The new name: an uppercase letter, then letters and numbers.")
        @SerialName("newName") val newName: String,
    )
}
