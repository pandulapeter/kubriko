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
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.withEventKey
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

@OptIn(EditorMutation::class)
public object SetEventKey : WriteCommand<SetEventKey.Input> {
    override val name: String = "set_event_key"
    override val description: String = "Sets the key a keyPressed Event of a block-built Actor type or Manager type reacts to."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType, eventIndex = input.eventIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: when {
            context.requireType(input.actorType).events[input.eventIndex].type != EventType.KEY_PRESSED -> Rejection(PlacementProblem.KEY_NOT_ALLOWED)
            !input.key.isKnown -> Rejection(LookupProblem.GAME_KEY_UNKNOWN, input.key.composeKeyName)
            else -> null
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val previous = definition.events[input.eventIndex].key ?: KeyChoice.SPACE
        context.requireBlockTypes.put(definition.withEventKey(input.eventIndex, input.key))
        return Applied(inverse = Invocation(SetEventKey, input.copy(key = previous)))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description(Descriptions.EVENT_INDEX)
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The key, " + KeyChoice.DESCRIPTION)
        @SerialName("key") val key: KeyChoice,
    )
}
