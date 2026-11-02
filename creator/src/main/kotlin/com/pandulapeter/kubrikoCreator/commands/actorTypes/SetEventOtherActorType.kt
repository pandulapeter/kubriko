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

import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.withEventOtherActorType
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
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed

@OptIn(EditorMutation::class)
internal object SetEventOtherActorType : WriteCommand<SetEventOtherActorType.Input> {
    override val name = "set_event_other_actor_type"
    override val description = "Sets the Actor type a collision Event of a block-built Actor type reacts to overlapping."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, eventIndex = input.eventIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex) ?: when {
        context.requireType(input.actorType).events[input.eventIndex].type != EventType.COLLISION -> Rejection(Res.string.error_command_other_actor_type_not_allowed)
        input.otherActorType != null && context.requireBlockTypes.actorType(input.otherActorType) == null -> Rejection(Res.string.error_command_actor_type_missing, input.otherActorType)
        else -> null
    }

    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: if (context.requireType(input.actorType).events[input.eventIndex].type != EventType.COLLISION) Rejection(Res.string.error_command_other_actor_type_not_allowed) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val previous = definition.events[input.eventIndex].otherActorType
        context.requireBlockTypes.put(definition.withEventOtherActorType(input.eventIndex, input.otherActorType))
        return Applied(inverse = Invocation(SetEventOtherActorType, input.copy(otherActorType = previous)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The name of the other Actor type, or null for none, which makes the Event never happen.")
        @SerialName("otherActorType") val otherActorType: String?,
    )
}
