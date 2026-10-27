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
import com.pandulapeter.kubrikoCreator.actors.withBlockInserted
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
import kubriko.creator.generated.resources.command_add_block

@OptIn(EditorMutation::class)
internal object AddBlock : WriteCommand<AddBlock.Input> {
    override val name = "add_block"
    override val description = "Adds a Block to an Event of a block-built Actor type. Some Blocks are only allowed in some Events; the Block catalog says which."
    override val title = Res.string.command_add_block
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType, input.eventIndex, blockIndex = input.index ?: context.blockTypes?.get(input.actorType)?.events?.getOrNull(input.eventIndex)?.blocks?.lastIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: context.requireActorType(input.actorType).let { validateBlock(input.block, it.events[input.eventIndex].type, it.properties, context.requireActorTypes.actorTypes.value.map { type -> type.name }) }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val blocks = definition.events[input.eventIndex].blocks
        val blockIndex = input.index?.coerceIn(0, blocks.size) ?: blocks.size
        context.requireActorTypes.put(definition.withBlockInserted(input.eventIndex, blockIndex, input.block))
        return Applied(inverse = Invocation(RemoveBlock, RemoveBlock.Input(input.actorType, input.eventIndex, blockIndex)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The Block to add.")
        @SerialName("block") val block: BlockDefinition,
        @Description("Where the Block goes in the Event, starting from 0. Last if left out.")
        @SerialName("index") val index: Int? = null,
    )
}
