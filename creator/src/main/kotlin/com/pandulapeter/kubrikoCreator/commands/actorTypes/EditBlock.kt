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
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_edit_block

@OptIn(EditorMutation::class)
internal object EditBlock : WriteCommand<EditBlock.Input> {
    override val name = "edit_block"
    override val description = "Replaces a Block of an Event of a block-built Actor type or Manager type, to change its values (or to swap it for another Block)."
    override val title = Res.string.command_edit_block
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, input.eventIndex, input.blockIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateBlock(input.actorType, input.eventIndex, input.blockIndex)
        ?: context.requireType(input.actorType).let { validateBlock(input.block, it.events[input.eventIndex].type, context.scopeOf(it)) }

    override fun validateRestoring(context: CommandContext, input: Input) = context.validateBlock(input.actorType, input.eventIndex, input.blockIndex)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val previous = definition.events[input.eventIndex].blocks[input.blockIndex]
        context.requireBlockTypes.put(definition.withBlockReplaced(input.eventIndex, input.blockIndex, input.block))
        return Applied(inverse = Invocation(EditBlock, input.copy(block = previous)))
    }

    override fun mergeKey(input: Input) = Triple(input.actorType, input.eventIndex, input.blockIndex)

    @Serializable
    data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description(Descriptions.BLOCK_INDEX)
        @SerialName("blockIndex") val blockIndex: Int,
        @Description("The Block that replaces it.")
        @SerialName("block") val block: BlockDefinition,
    )
}
