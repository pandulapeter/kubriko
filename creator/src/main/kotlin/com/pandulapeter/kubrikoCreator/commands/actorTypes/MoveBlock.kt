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
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
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
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description(Descriptions.BLOCK_INDEX)
        @SerialName("blockIndex") val blockIndex: Int,
        @Description("The index the Block ends up at.")
        @SerialName("targetIndex") val targetIndex: Int,
    )
}
