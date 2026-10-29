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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_actors_values

internal object SetActorsValues : WriteCommand<SetActorsValues.Input> {
    override val name = "set_actors_values"
    override val description = "Changes values of many Actors of a Scene at once, as one step to undo, each as set_actor_values does: where it is, " +
        "its size, rotation, and scale, and for block-built Actor types, its color and the values of its Properties. Values left out stay as they are."
    override val title = Res.string.command_set_actors_values
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActors(input.scene, input.actors.map { it.actorId })
        ?: run {
            val actors = context.readScene(input.scene).associateBy { it.id }
            input.actors.firstNotNullOfOrNull { changes -> context.validateActorValues(actors.getValue(changes.actorId), changes) }
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val changes = input.actors.associateBy { it.actorId }
        val previous = ArrayList<SetActorsState.ActorState>(changes.size)
        context.writeScene(
            input.scene,
            actors.map { actor ->
                changes[actor.id]?.let { change ->
                    previous += SetActorsState.ActorState(actor.id, actor.state)
                    actor.copy(state = change.applyTo(actor.state))
                } ?: actor
            },
        )
        return Applied(inverse = Invocation(SetActorsState, SetActorsState.Input(input.scene, previous)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The changes, one per Actor.")
        @SerialName("actors") val actors: List<ActorValueChanges>,
    )
}
