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

import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.withBlockMoved
import com.pandulapeter.kubrikoCreator.actors.withBlocksMovedToEvent
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.validateIndex
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object MoveBlock : WriteCommand<MoveBlock.Input> {
    override val name: String = "move_block"
    override val description: String = "Moves a Block within its Event or into another one; an if or repeat Block takes its section."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType, input.targetEvent, input.targetIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = validateStructure(context, input) ?: if (input.isBetweenEvents) validateBetweenEvents(context, input) else null

    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = validateStructure(context, input)

    private fun validateStructure(context: CommandContext, input: Input) = context.validateBlock(input.actorType, input.eventIndex, input.blockIndex)
        ?: context.validateEvent(input.actorType, input.targetEvent)
        ?: context.requireType(input.actorType).events[input.targetEvent].blocks.let { validateIndex(input.targetIndex, if (input.isBetweenEvents) it.size else it.lastIndex) }

    private fun validateBetweenEvents(context: CommandContext, input: Input): Rejection? {
        val definition = context.requireType(input.actorType)
        val blocks = definition.events[input.eventIndex].blocks
        val target = definition.events[input.targetEvent]
        val moving = BlockNesting.movingBetweenEvents(blocks, input.blockIndex) ?: return Rejection(CommandProblem.BLOCK_SECTION_MARKER_BETWEEN_EVENTS)
        if (moving.isUnendedSection && input.targetIndex != target.blocks.size) return Rejection(CommandProblem.BLOCK_SECTION_UNENDED_NOT_LAST)
        val scope = context.scopeOf(definition)
        return moving.indexes.firstNotNullOfOrNull { validateBlock(blocks[it], target.type, scope) }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val moved = if (input.isBetweenEvents) {
            val moving = checkNotNull(BlockNesting.movingBetweenEvents(definition.events[input.eventIndex].blocks, input.blockIndex))
            definition.withBlocksMovedToEvent(input.eventIndex, moving.indexes, input.targetEvent, input.targetIndex)
        } else {
            definition.withBlockMoved(input.eventIndex, input.blockIndex, input.targetIndex)
        }
        context.requireBlockTypes.put(moved)
        val inverse = Input(input.actorType, input.targetEvent, input.targetIndex, input.blockIndex, targetEventIndex = input.eventIndex.takeIf { input.isBetweenEvents })
        return Applied(inverse = Invocation(MoveBlock, inverse))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description(Descriptions.BLOCK_INDEX)
        @SerialName("blockIndex") val blockIndex: Int,
        @Description("The index the Block ends up at; into another Event, up to its Block count, and only that for a section without an end Block.")
        @SerialName("targetIndex") val targetIndex: Int,
        @Description("Another Event to move it into.")
        @SerialName("targetEventIndex") val targetEventIndex: Int? = null,
    ) {
        internal val targetEvent get() = targetEventIndex ?: eventIndex
        internal val isBetweenEvents get() = targetEvent != eventIndex
    }
}
