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
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.withEventInserted
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object AddEvent : WriteCommand<AddEvent.Input> {
    override val name: String = "add_event"
    override val description: String = "Adds an Event to a block-built Actor type or Manager type, optionally with its Blocks. A keyPressed Event reacts to the space key unless another key is given."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType, eventIndex = input.index ?: context.blockTypes?.get(input.actorType)?.events?.lastIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateType(input.actorType)
        ?: validateEvent(input.event, context.scopeOf(context.requireType(input.actorType)))

    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validateType(input.actorType)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val eventIndex = input.index?.coerceIn(0, definition.events.size) ?: definition.events.size
        context.requireBlockTypes.put(definition.withEventInserted(eventIndex, input.event))
        return Applied(inverse = Invocation(RemoveEvent, RemoveEvent.Input(input.actorType, eventIndex)))
    }

    private val Input.event get() = EventDefinition(
        type = type,
        key = key ?: KeyChoice.SPACE.takeIf { type == EventType.KEY_PRESSED },
        otherActorType = otherActorType,
        timer = timer ?: TimerNaming.DEFAULT_NAME.takeIf { type == EventType.TIMER_FINISHED },
        blocks = blocks,
    )

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("When the Event happens.")
        @SerialName("type") val type: EventType,
        @Description("The key a keyPressed Event reacts to, " + KeyChoice.DESCRIPTION + " Spacebar unless another one is given. Only keyPressed Events have one.")
        @SerialName("key") val key: KeyChoice? = null,
        @Description("The Actor type a collision Event reacts to overlapping. Only collision Events have one.")
        @SerialName("otherActorType") val otherActorType: String? = null,
        @Description("The name of the Timer a timerFinished Event reacts to, like spawnEnemy: a letter, then letters and digits, starting lowercase; timer unless another one is given. Only timerFinished Events have one.")
        @SerialName("timer") val timer: String? = null,
        @Description("The Blocks the Event runs, in order.")
        @SerialName("blocks") val blocks: List<BlockDefinition> = emptyList(),
        @Description("Where the Event goes in the Event list, starting from 0. Last if left out.")
        @SerialName("index") val index: Int? = null,
    )
}
