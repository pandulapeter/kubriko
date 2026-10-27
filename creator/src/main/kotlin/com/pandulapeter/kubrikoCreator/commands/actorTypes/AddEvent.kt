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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.withEventInserted
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
import kubriko.creator.generated.resources.command_add_event

@OptIn(EditorMutation::class)
internal object AddEvent : WriteCommand<AddEvent.Input> {
    override val name = "add_event"
    override val description = "Adds an Event to a block-built Actor type, optionally with its Blocks. A keyPressed Event reacts to the space key unless another key is given."
    override val title = Res.string.command_add_event
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType, eventIndex = input.index ?: context.actorTypes?.get(input.actorType)?.events?.lastIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType)
        ?: validateEvent(input.event, context.requireActorTypes.actorTypes.value.map { it.name }, context.requireActorType(input.actorType).properties)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val eventIndex = input.index?.coerceIn(0, definition.events.size) ?: definition.events.size
        context.requireActorTypes.put(definition.withEventInserted(eventIndex, input.event))
        return Applied(inverse = Invocation(RemoveEvent, RemoveEvent.Input(input.actorType, eventIndex)))
    }

    private val Input.event get() = EventDefinition(type = type, key = key ?: KeyChoice.SPACE.takeIf { type == EventType.KEY_PRESSED }, otherActorType = otherActorType, blocks = blocks)

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("When the Event happens.")
        @SerialName("type") val type: EventType,
        @Description("The key a keyPressed Event reacts to. Only keyPressed Events have one.")
        @SerialName("key") val key: KeyChoice? = null,
        @Description("The Actor type a collision Event reacts to overlapping. Only collision Events have one.")
        @SerialName("otherActorType") val otherActorType: String? = null,
        @Description("The Blocks the Event runs, in order.")
        @SerialName("blocks") val blocks: List<BlockDefinition> = emptyList(),
        @Description("Where the Event goes in the Event list, starting from 0. Last if left out.")
        @SerialName("index") val index: Int? = null,
    )
}
