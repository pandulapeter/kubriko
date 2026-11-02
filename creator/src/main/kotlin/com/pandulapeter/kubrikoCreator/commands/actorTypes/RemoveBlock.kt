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

import com.pandulapeter.kubrikoCreator.actors.withBlockRemoved
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

@OptIn(EditorMutation::class)
internal object RemoveBlock : WriteCommand<RemoveBlock.Input> {
    override val name = "remove_block"
    override val description = "Removes a Block from an Event of a block-built Actor type or Manager type."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, input.eventIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateBlock(input.actorType, input.eventIndex, input.blockIndex)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val block = definition.events[input.eventIndex].blocks[input.blockIndex]
        context.requireBlockTypes.put(definition.withBlockRemoved(input.eventIndex, input.blockIndex))
        return Applied(inverse = Invocation(AddBlock, AddBlock.Input(input.actorType, input.eventIndex, block, index = input.blockIndex)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description(Descriptions.BLOCK_INDEX)
        @SerialName("blockIndex") val blockIndex: Int,
    )
}
