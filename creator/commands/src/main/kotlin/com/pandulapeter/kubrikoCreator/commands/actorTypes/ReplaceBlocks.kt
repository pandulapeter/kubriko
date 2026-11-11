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
import com.pandulapeter.kubrikoCreator.actors.withBlocksReplaced
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Several Blocks of an Event added, removed, or rewritten in one call and one undo step, where one add_block or remove_block
 * per Block would cost the agent a call each.
 */
@OptIn(EditorMutation::class)
public object ReplaceBlocks : WriteCommand<ReplaceBlocks.Input> {
    override val name: String = "replace_blocks"
    override val description: String = "Replaces count Blocks from index in an Event with blocks, in one undo step: adds several (count 0), removes a run " +
        "(no blocks), or rewrites one."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType, input.eventIndex, blockIndex = input.index)

    override fun validate(context: CommandContext, input: Input): Rejection? = validateRange(context, input) ?: context.requireType(input.actorType).let { type ->
        input.blocks.firstNotNullOfOrNull { validateBlock(it, type.events[input.eventIndex].type, context.scopeOf(type)) }
    }

    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = validateRange(context, input)

    private fun validateRange(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex) ?: run {
        val size = context.requireType(input.actorType).events[input.eventIndex].blocks.size
        when {
            input.count < 0 || (input.count == 0 && input.blocks.isEmpty()) -> Rejection(CommandProblem.BLOCKS_NOTHING_TO_REPLACE)
            input.index !in 0..size -> Rejection(LookupProblem.BLOCK_INSERTION_INDEX_INVALID, input.actorType, input.eventIndex.toString(), size.toString())
            input.count > 0 && input.index == size -> Rejection(LookupProblem.BLOCKS_NONE_TO_REMOVE, input.actorType, input.eventIndex.toString(), input.index.toString())
            input.count > size - input.index -> Rejection(
                LookupProblem.BLOCKS_COUNT_TOO_LARGE,
                input.actorType,
                input.eventIndex.toString(),
                (size - 1).toString(),
                input.index.toString(),
                (size - input.index).toString(),
            )
            else -> null
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val removed = definition.events[input.eventIndex].blocks.subList(input.index, input.index + input.count).toList()
        context.requireBlockTypes.put(definition.withBlocksReplaced(input.eventIndex, input.index, input.count, input.blocks))
        return Applied(inverse = Invocation(ReplaceBlocks, Input(input.actorType, input.eventIndex, input.index, input.blocks.size, removed)))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The index of the first Block replaced, or where the blocks go, from 0.")
        @SerialName("index") val index: Int,
        @Description("How many Blocks from index to remove.")
        @SerialName("count") val count: Int = 0,
        @Description("The Blocks that take their place.")
        @SerialName("blocks") val blocks: List<BlockDefinition> = emptyList(),
    )
}
