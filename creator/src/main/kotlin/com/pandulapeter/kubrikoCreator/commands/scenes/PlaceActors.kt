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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_place_actors

internal object PlaceActors : WriteCommand<PlaceActors.Input> {
    override val name = "place_actors"
    override val description = "Places many Actors of block-built Actor types in a Scene at once, like a grid of bricks, as one step to undo. " +
        "Each one gets the size its Actor type gives it, and they are added at the end of the Scene in the order given. Returns their actorIds in that order. " +
        "Scene coordinates are in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val title = Res.string.command_place_actors
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePlacements(input.scene, input.actors)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val placed = context.placeActors(input.scene, input.actors)
        return Applied(
            inverse = Invocation(RemoveActors, RemoveActors.Input(input.scene, placed.map { it.id })),
            result = buildJsonObject { putJsonArray("actorIds") { placed.forEach { add(it.id) } } },
        )
    }

    @Serializable
    data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("The Actors to place, in the order they are added.")
        @SerialName("actors") val actors: List<Placement>,
    )

    @Serializable
    @Description("One Actor to place.")
    data class Placement(
        @Description("The name of the block-built Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The horizontal position, in scene units.")
        @SerialName("x") val x: Float,
        @Description("The vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float,
        @Description("The ID the new Actor gets: letters, numbers, dashes, and underscores. Left out, it gets a new one.")
        @SerialName("actorId") val actorId: String? = null,
    )
}
