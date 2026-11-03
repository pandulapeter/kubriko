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

internal object SetActorsState : WriteCommand<SetActorsState.Input> {
    override val name = "set_actors_state"
    override val description = "Replaces the whole saved states of many Actors of a Scene at once, as read_scene returns them, as one step to undo: " +
        "for values set_actors_values doesn't cover, such as those of hand-written Actor types."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateStates(input.scene, input.actors)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previous = context.replaceStates(input.scene, input.actors)
        return Applied(inverse = Invocation(SetActorsState, input.copy(actors = previous)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("The new states, one per Actor.")
        @SerialName("actors") val actors: List<ActorState>,
    )

    @Serializable
    @Description("The new saved state of one Actor.")
    data class ActorState(
        @Description(Descriptions.SCENE_ACTOR_ID)
        @SerialName("actorId") val actorId: String,
        @Description("The new saved state of the Actor, as a JSON object in a string.")
        @SerialName("state") val state: String,
    )
}
