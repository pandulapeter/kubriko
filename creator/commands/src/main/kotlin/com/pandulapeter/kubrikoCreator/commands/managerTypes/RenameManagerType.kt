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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.commands.screenplay.typeMoves
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object RenameManagerType : WriteCommand<RenameManagerType.Input> {
    override val name: String = "rename_manager_type"
    override val description: String = "Renames a block-built Manager type, its generated Kotlin class, and every Block, log message, and text of every type that uses its Properties, and the screenplay's mentions of it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(if (context.blockTypes?.managerType(input.newName) != null) input.newName else input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType)
        ?: BlockTypeNaming.validate(input.newName, context.requireBlockTypes.takenNames, BlockTypeKind.MANAGER)?.let(::Rejection)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val moves = context.typeMoves(input.managerType, newName = input.newName)
        context.requireBlockTypes.rename(input.managerType, input.newName)
        return context.withMentionsFollowed(moves, Applied(inverse = Invocation(RenameManagerType, Input(managerType = input.newName, newName = input.managerType))))
    }

    @Serializable
    public data class Input(
        @Description("The current name of the Manager type.")
        @SerialName("managerType") val managerType: String,
        @Description("The new name: an uppercase letter, then letters and numbers.")
        @SerialName("newName") val newName: String,
    )
}
