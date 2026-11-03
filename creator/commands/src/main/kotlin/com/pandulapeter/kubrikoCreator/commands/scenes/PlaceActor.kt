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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object PlaceActor : WriteCommand<PlaceActor.Input> {
    override val name: String = "place_actor"
    override val description: String = "Places an Actor of a block-built Actor type in a Scene, at the size its Actor type gives it. " +
        "Returns its actorId. Scene coordinates are in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePlacements(input.scene, listOf(input.placement))

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val id = context.placeActors(input.scene, listOf(input.placement)).single().id
        return Applied(
            inverse = Invocation(RemoveActor, RemoveActor.Input(input.scene, id)),
            result = buildJsonObject { put("actorId", id) },
        )
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("The name of the block-built Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The horizontal position, in scene units.")
        @SerialName("x") val x: Float,
        @Description("The vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float,
        @Description("The ID the new Actor gets: letters, numbers, dashes, and underscores. Left out, it gets a new one.")
        @SerialName("actorId") val actorId: String? = null,
    ) {
        internal val placement get() = PlaceActors.Placement(actorType, x, y, actorId)
    }
}
