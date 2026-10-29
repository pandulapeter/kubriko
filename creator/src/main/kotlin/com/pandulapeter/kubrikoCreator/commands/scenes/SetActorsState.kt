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
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_actors_state
import kubriko.creator.generated.resources.error_command_scene_content_invalid

internal object SetActorsState : WriteCommand<SetActorsState.Input> {
    override val name = "set_actors_state"
    override val description = "Replaces the whole saved states of many Actors of a Scene at once, as read_scene returns them, as one step to undo: " +
        "for values set_actors_values doesn't cover, such as those of hand-written Actor types."
    override val title = Res.string.command_set_actors_state
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActors(input.scene, input.actors.map { it.actorId })
        ?: if (input.actors.any { SceneDocument.stateObject(it.state) == null }) Rejection(Res.string.error_command_scene_content_invalid) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val states = input.actors.associate { it.actorId to it.state }
        val previous = ArrayList<ActorState>(states.size)
        context.writeScene(
            input.scene,
            actors.map { actor ->
                states[actor.id]?.let { state ->
                    previous += ActorState(actor.id, actor.state)
                    actor.copy(state = state)
                } ?: actor
            },
        )
        return Applied(inverse = Invocation(SetActorsState, input.copy(actors = previous)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The new states, one per Actor.")
        @SerialName("actors") val actors: List<ActorState>,
    )

    @Serializable
    @Description("The new saved state of one Actor.")
    data class ActorState(
        @Description("The ID of the Actor in the Scene, as read_scene returns it.")
        @SerialName("actorId") val actorId: String,
        @Description("The new saved state of the Actor, as a JSON object in a string.")
        @SerialName("state") val state: String,
    )
}
