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
import kubriko.creator.generated.resources.command_move_actor
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_without_position

internal object MoveActor : WriteCommand<MoveActor.Input> {
    override val name = "move_actor"
    override val description = "Moves an Actor of a Scene to another position, in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val title = Res.string.command_move_actor
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorId) ?: when {
        !input.x.isFinite() || !input.y.isFinite() -> Rejection(Res.string.error_command_number_invalid)
        SceneDocument.positionOf(context.readScene(input.scene).requireActor(input.actorId).state) == null -> Rejection(Res.string.error_command_scene_actor_without_position, input.actorId)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val (x, y) = checkNotNull(SceneDocument.positionOf(actors.requireActor(input.actorId).state))
        context.writeScene(input.scene, actors.withActorChanged(input.actorId) { it.copy(state = checkNotNull(SceneDocument.withPosition(it.state, input.x, input.y))) })
        return Applied(inverse = Invocation(MoveActor, input.copy(x = x, y = y)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The ID of the Actor in the Scene, as read_scene returns it.")
        @SerialName("actorId") val actorId: String,
        @Description("The new horizontal position, in scene units.")
        @SerialName("x") val x: Float,
        @Description("The new vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float,
    )
}
