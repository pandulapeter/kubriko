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
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_move_actor
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_without_position

internal object MoveActor : WriteCommand<MoveActor.Input> {
    override val name = "move_actor"
    override val description = "Moves an Actor of a Scene to another position, in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val title = Res.string.command_move_actor
    override val inputSerializer = Input.serializer()

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorIndex) ?: when {
        !input.x.isFinite() || !input.y.isFinite() -> Rejection(Res.string.error_command_number_invalid)
        SceneDocument.positionOf(context.readScene(input.scene)[input.actorIndex].state) == null -> Rejection(Res.string.error_command_scene_actor_without_position, input.actorIndex.toString())
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val actor = actors[input.actorIndex]
        val (x, y) = checkNotNull(SceneDocument.positionOf(actor.state))
        val moved = actor.copy(state = checkNotNull(SceneDocument.withPosition(actor.state, input.x, input.y)))
        context.writeScene(input.scene, actors.mapIndexed { index, it -> if (index == input.actorIndex) moved else it })
        return Applied(inverse = Invocation(MoveActor, input.copy(x = x, y = y)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its file name without .json.")
        @SerialName("scene") val scene: String,
        @Description("The index of the Actor in the Scene's Actor list, starting from 0.")
        @SerialName("actorIndex") val actorIndex: Int,
        @Description("The new horizontal position, in scene units.")
        @SerialName("x") val x: Float,
        @Description("The new vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float,
    )
}
