/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object SetActorState : WriteCommand<SetActorState.Input> {
    override val name = "set_actor_state"
    override val description = "Replaces the whole saved state of an Actor of a Scene, as read_scene returns it: for values set_actor_values doesn't cover, " +
        "such as those of hand-written Actor types."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateStates(input.scene, listOf(input.actorState))

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previous = context.replaceStates(input.scene, listOf(input.actorState)).single()
        return Applied(inverse = Invocation(SetActorState, input.copy(state = previous.state)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description(Descriptions.SCENE_ACTOR_ID)
        @SerialName("actorId") val actorId: String,
        @Description("The new saved state of the Actor, as a JSON object in a string.")
        @SerialName("state") val state: String,
    ) {
        val actorState get() = SetActorsState.ActorState(actorId, state)
    }
}
