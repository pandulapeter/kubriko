/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.withBlockMoved
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_move_block
import kubriko.creator.generated.resources.error_command_index_invalid

@OptIn(EditorMutation::class)
internal object MoveBlock : WriteCommand<MoveBlock.Input> {
    override val name = "move_block"
    override val description = "Moves a Block to another place in its Event, which changes the order Blocks run in."
    override val title = Res.string.command_move_block
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, input.eventIndex, input.targetIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateBlock(input.actorType, input.eventIndex, input.blockIndex)
        ?: if (input.targetIndex !in context.requireType(input.actorType).events[input.eventIndex].blocks.indices) {
            Rejection(Res.string.error_command_index_invalid, input.targetIndex.toString())
        } else {
            null
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireBlockTypes.put(context.requireType(input.actorType).withBlockMoved(input.eventIndex, input.blockIndex, input.targetIndex))
        return Applied(inverse = Invocation(MoveBlock, input.copy(blockIndex = input.targetIndex, targetIndex = input.blockIndex)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type or Manager type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The index of the Block in the Event, starting from 0.")
        @SerialName("blockIndex") val blockIndex: Int,
        @Description("The index the Block ends up at.")
        @SerialName("targetIndex") val targetIndex: Int,
    )
}
