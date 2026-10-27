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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.withBlockReplaced
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
import kubriko.creator.generated.resources.command_edit_block

@OptIn(EditorMutation::class)
internal object EditBlock : WriteCommand<EditBlock.Input> {
    override val name = "edit_block"
    override val description = "Replaces a Block of an Event of a block-built Actor type, to change its values (or to swap it for another Block)."
    override val title = Res.string.command_edit_block
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType, input.eventIndex, input.blockIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateBlock(input.actorType, input.eventIndex, input.blockIndex)
        ?: context.requireActorType(input.actorType).let { validateBlock(input.block, it.events[input.eventIndex].type, it.properties) }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val previous = definition.events[input.eventIndex].blocks[input.blockIndex]
        context.requireActorTypes.put(definition.withBlockReplaced(input.eventIndex, input.blockIndex, input.block))
        return Applied(inverse = Invocation(EditBlock, input.copy(block = previous)))
    }

    override fun mergeKey(input: Input) = Triple(input.actorType, input.eventIndex, input.blockIndex)

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The index of the Block in the Event, starting from 0.")
        @SerialName("blockIndex") val blockIndex: Int,
        @Description("The Block that replaces it.")
        @SerialName("block") val block: BlockDefinition,
    )
}
