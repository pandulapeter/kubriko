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

import com.pandulapeter.kubrikoCreator.actors.withEventRemoved
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
import kubriko.creator.generated.resources.command_remove_event

@OptIn(EditorMutation::class)
internal object RemoveEvent : WriteCommand<RemoveEvent.Input> {
    override val name = "remove_event"
    override val description = "Removes an Event and its Blocks from a block-built Actor type or Manager type."
    override val title = Res.string.command_remove_event
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, eventIndex = input.eventIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val event = definition.events[input.eventIndex]
        context.requireBlockTypes.put(definition.withEventRemoved(input.eventIndex))
        return Applied(
            inverse = Invocation(
                command = AddEvent,
                input = AddEvent.Input(
                    actorType = input.actorType,
                    type = event.type,
                    key = event.key,
                    otherActorType = event.otherActorType,
                    blocks = event.blocks,
                    index = input.eventIndex,
                ),
            ),
        )
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type or Manager type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
    )
}
