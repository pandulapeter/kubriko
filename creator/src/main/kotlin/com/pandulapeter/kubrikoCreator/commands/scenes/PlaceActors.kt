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
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateActorType
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_place_actors
import kubriko.creator.generated.resources.error_command_number_invalid

internal object PlaceActors : WriteCommand<PlaceActors.Input> {
    override val name = "place_actors"
    override val description = "Places many Actors of block-built Actor types in a Scene at once, like a grid of bricks, as one step to undo. " +
        "Each one gets the size its Actor type gives it, and they are added at the end of the Scene in the order given. Returns their actorIds in that order. " +
        "Scene coordinates are in scene units, with 0, 0 in the center of the Scene and y growing downwards."
    override val title = Res.string.command_place_actors
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateScene(input.scene)
        ?: validateActorList(input.actors.map { it.actorId })
        ?: run {
            val ids = context.readScene(input.scene).mapTo(HashSet()) { it.id }
            input.actors.firstNotNullOfOrNull { placement ->
                context.validateActorType(placement.actorType)
                    ?: (if (placement.x.isFinite() && placement.y.isFinite()) null else Rejection(Res.string.error_command_number_invalid))
                    ?: placement.actorId?.let { id -> validateNewActorId(id) { it in ids } }
            }
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val ids = actors.mapTo(HashSet()) { it.id }
        input.actors.forEach { placement -> placement.actorId?.let(ids::add) }
        val placed = input.actors.map { placement ->
            val definition = context.requireActorType(placement.actorType)
            val id = placement.actorId ?: SceneDocument.newId(ids).also(ids::add)
            SceneActorEntry(placement.actorType, SceneDocument.blockBuiltState(placement.x, placement.y, definition.width, definition.height), id)
        }
        context.writeScene(input.scene, actors + placed)
        return Applied(
            inverse = Invocation(RemoveActors, RemoveActors.Input(input.scene, placed.map { it.id })),
            result = buildJsonObject { putJsonArray("actorIds") { placed.forEach { add(it.id) } } },
        )
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
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
