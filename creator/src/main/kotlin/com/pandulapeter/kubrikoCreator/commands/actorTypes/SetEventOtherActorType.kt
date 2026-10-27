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
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_event_other_actor_type
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed

@OptIn(EditorMutation::class)
internal object SetEventOtherActorType : WriteCommand<SetEventOtherActorType.Input> {
    override val name = "set_event_other_actor_type"
    override val description = "Sets the Actor type a collision Event of a block-built Actor type reacts to overlapping."
    override val title = Res.string.command_set_event_other_actor_type
    override val inputSerializer = Input.serializer()

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex) ?: when {
        context.requireActorType(input.actorType).events[input.eventIndex].type != EventType.COLLISION -> Rejection(Res.string.error_command_other_actor_type_not_allowed)
        input.otherActorType != null && context.requireActorTypes[input.otherActorType] == null -> Rejection(Res.string.error_command_actor_type_missing, input.otherActorType)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val previous = definition.events[input.eventIndex].otherActorType
        context.requireActorTypes.put(definition.withEventOtherActorType(input.eventIndex, input.otherActorType))
        return Applied(inverse = Invocation(SetEventOtherActorType, input.copy(otherActorType = previous)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The name of the other Actor type, or null for none, which makes the Event never happen.")
        @SerialName("otherActorType") val otherActorType: String?,
    )
}
