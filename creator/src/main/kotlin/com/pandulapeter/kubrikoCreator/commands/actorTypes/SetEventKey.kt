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
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.withEventKey
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
import kubriko.creator.generated.resources.command_set_event_key
import kubriko.creator.generated.resources.error_command_key_not_allowed

@OptIn(EditorMutation::class)
internal object SetEventKey : WriteCommand<SetEventKey.Input> {
    override val name = "set_event_key"
    override val description = "Sets the key a keyPressed Event of a block-built Actor type reacts to."
    override val title = Res.string.command_set_event_key
    override val inputSerializer = Input.serializer()

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: if (context.requireActorType(input.actorType).events[input.eventIndex].type != EventType.KEY_PRESSED) Rejection(Res.string.error_command_key_not_allowed) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val previous = definition.events[input.eventIndex].key ?: KeyChoice.SPACE
        context.requireActorTypes.put(definition.withEventKey(input.eventIndex, input.key))
        return Applied(inverse = Invocation(SetEventKey, input.copy(key = previous)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The key.")
        @SerialName("key") val key: KeyChoice,
    )
}
