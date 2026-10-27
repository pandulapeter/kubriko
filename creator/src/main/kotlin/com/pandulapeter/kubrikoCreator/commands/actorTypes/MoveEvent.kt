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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_move_event
import kubriko.creator.generated.resources.error_command_index_invalid

@OptIn(EditorMutation::class)
internal object MoveEvent : WriteCommand<MoveEvent.Input> {
    override val name = "move_event"
    override val description = "Moves an Event of a block-built Actor type to another place in its Event list. The order of Events is the order they run in when they happen on the same frame."
    override val title = Res.string.command_move_event
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType, eventIndex = input.targetIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: if (input.targetIndex !in context.requireActorType(input.actorType).events.indices) Rejection(Res.string.error_command_index_invalid, input.targetIndex.toString()) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireActorTypes.put(context.requireActorType(input.actorType).withEventMoved(input.eventIndex, input.targetIndex))
        return Applied(inverse = Invocation(MoveEvent, Input(input.actorType, eventIndex = input.targetIndex, targetIndex = input.eventIndex)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The index the Event ends up at.")
        @SerialName("targetIndex") val targetIndex: Int,
    )
}
