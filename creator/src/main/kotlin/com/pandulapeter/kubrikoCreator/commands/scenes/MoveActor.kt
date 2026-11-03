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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object MoveActor : WriteCommand<MoveActor.Input> {
    override val name = "move_actor"
    override val description = "Moves an Actor of a Scene to another position, in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorId) ?: when {
        !input.x.isFinite() || !input.y.isFinite() -> Rejection(CommandProblem.NUMBER_INVALID)
        SceneDocument.positionOf(context.readScene(input.scene).requireActor(input.actorId).state) == null -> Rejection(CommandProblem.SCENE_ACTOR_WITHOUT_POSITION, input.actorId)
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
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description(Descriptions.SCENE_ACTOR_ID)
        @SerialName("actorId") val actorId: String,
        @Description("The new horizontal position, in scene units.")
        @SerialName("x") val x: Float,
        @Description("The new vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float,
    )
}
