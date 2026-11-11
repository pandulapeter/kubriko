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

import com.pandulapeter.kubrikoCreator.actors.withEventMoved
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
public object MoveEvent : WriteCommand<MoveEvent.Input> {
    override val name: String = "move_event"
    override val description: String = "Moves an Event of a block-built Actor type or Manager type to another place in its Event list. The order of Events is the order they run in when they happen on the same frame."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType, eventIndex = input.targetIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: validateIndex(input.targetIndex, context.requireType(input.actorType).events.lastIndex)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireBlockTypes.put(context.requireType(input.actorType).withEventMoved(input.eventIndex, input.targetIndex))
        return Applied(inverse = Invocation(MoveEvent, Input(input.actorType, eventIndex = input.targetIndex, targetIndex = input.eventIndex)))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The index the Event ends up at.")
        @SerialName("targetIndex") val targetIndex: Int,
    )
}
