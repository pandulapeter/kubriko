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
import kubriko.creator.generated.resources.command_move_actor_order
import kubriko.creator.generated.resources.error_command_index_invalid

internal object MoveActorOrder : WriteCommand<MoveActorOrder.Input> {
    override val name = "move_actor_order"
    override val description = "Moves an Actor to another place in a Scene's list of Actors, which is the order they are added to the game in. " +
        "Of two Actors with the same drawing order, the later one is drawn on top."
    override val title = Res.string.command_move_actor_order
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorId)
        ?: if (input.index !in context.readScene(input.scene).indices) Rejection(Res.string.error_command_index_invalid, input.index.toString()) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene).toMutableList()
        val index = actors.indexOfFirst { it.id == input.actorId }
        actors.add(input.index, actors.removeAt(index))
        context.writeScene(input.scene, actors)
        return Applied(inverse = Invocation(MoveActorOrder, input.copy(index = index)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The ID of the Actor in the Scene, as read_scene returns it.")
        @SerialName("actorId") val actorId: String,
        @Description("Where it goes in the Scene's list of Actors, starting from 0 (added first); the number of Actors minus 1 puts it last.")
        @SerialName("index") val index: Int,
    )
}
