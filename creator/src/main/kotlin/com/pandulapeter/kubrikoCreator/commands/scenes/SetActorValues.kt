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

import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
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
import kubriko.creator.generated.resources.command_set_actor_values

internal object SetActorValues : WriteCommand<SetActorValues.Input> {
    override val name = "set_actor_values"
    override val description = "Changes values of one Actor of a Scene: where it is, its size, rotation, and scale, and for block-built Actor types, " +
        "its color and the values of its Properties (each Actor keeps its own). Values left out stay as they are."
    override val title = Res.string.command_set_actor_values
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorId)
        ?: context.validateActorValues(context.readScene(input.scene).requireActor(input.actorId), input.changes)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val previous = actors.requireActor(input.actorId)
        val state = input.changes.applyTo(previous.state)
        context.writeScene(input.scene, actors.withActorChanged(input.actorId) { it.copy(state = state) })
        return Applied(inverse = Invocation(SetActorState, SetActorState.Input(input.scene, input.actorId, previous.state)))
    }

    override fun mergeKey(input: Input) = listOf(
        input.scene, input.actorId, input.x != null, input.y != null, input.width != null, input.height != null, input.rotation != null,
        input.scaleHorizontal != null, input.scaleVertical != null, input.color != null, input.properties.map { it.name },
    )

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The ID of the Actor in the Scene, as read_scene returns it.")
        @SerialName("actorId") val actorId: String,
        @Description("The new horizontal position, in scene units.")
        @SerialName("x") val x: Float? = null,
        @Description("The new vertical position, in scene units, positive downwards.")
        @SerialName("y") val y: Float? = null,
        @Description("The new width, in scene units.")
        @SerialName("width") val width: Float? = null,
        @Description("The new height, in scene units.")
        @SerialName("height") val height: Float? = null,
        @Description("The new rotation, in degrees, clockwise.")
        @SerialName("rotation") val rotation: Float? = null,
        @Description("How much it is stretched horizontally: 1 is its size.")
        @SerialName("scaleHorizontal") val scaleHorizontal: Float? = null,
        @Description("How much it is stretched vertically: 1 is its size.")
        @SerialName("scaleVertical") val scaleVertical: Float? = null,
        @Description("The new color of an Actor of a block-built Actor type, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
        @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long? = null,
        @Description("New values of the Properties of an Actor of a block-built Actor type.")
        @SerialName("properties") val properties: List<ActorValueChanges.PropertyValue> = emptyList(),
    ) {
        val changes get() = ActorValueChanges(actorId, x, y, width, height, rotation, scaleHorizontal, scaleVertical, color, properties)
    }
}
