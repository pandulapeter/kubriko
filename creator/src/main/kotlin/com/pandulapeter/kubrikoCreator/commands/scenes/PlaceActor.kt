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
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateActorType
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_place_actor
import kubriko.creator.generated.resources.error_command_number_invalid

internal object PlaceActor : WriteCommand<PlaceActor.Input> {
    override val name = "place_actor"
    override val description = "Places an Actor of a block-built Actor type in a Scene, at the size its Actor type gives it. " +
        "Returns its index in the Scene's Actor list. Scene coordinates are in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val title = Res.string.command_place_actor
    override val inputSerializer = Input.serializer()

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateScene(input.scene)
        ?: context.validateActorType(input.actorType)
        ?: if (input.x.isFinite() && input.y.isFinite()) null else Rejection(Res.string.error_command_number_invalid)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val definition = context.requireActorType(input.actorType)
        context.writeScene(input.scene, actors + SceneActorEntry(input.actorType, SceneDocument.blockBuiltState(input.x, input.y, definition.width, definition.height)))
        return Applied(
            inverse = Invocation(RemoveActor, RemoveActor.Input(input.scene, actors.size)),
            result = buildJsonObject { put("actorIndex", actors.size) },
        )
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its file name without .json.")
        @SerialName("scene") val scene: String,
        @Description("The name of the block-built Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The horizontal position, in scene units.")
        @SerialName("x") val x: Float,
        @Description("The vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float,
    )
}
