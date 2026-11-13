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
public object SetMovement : WriteCommand<SetMovement.Input> {
    override val name: String = "set_movement"
    override val description: String = "Changes how an Actor type's Actors move. Settings left out stay as they are."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        context.requireBlockTypes.put(definition.copy(staysInsideScene = input.staysInsideScene ?: definition.staysInsideScene))
        return Applied(
            inverse = Invocation(
                command = SetMovement,
                input = Input(
                    actorType = input.actorType,
                    staysInsideScene = input.staysInsideScene?.let { definition.staysInsideScene },
                ),
            ),
        )
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.ACTOR_TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("Whether its Actors are pushed back inside the Scene's bounds whenever they reach past an edge, rather than moving on past it.")
        @SerialName("staysInsideScene") val staysInsideScene: Boolean? = null,
    )
}
