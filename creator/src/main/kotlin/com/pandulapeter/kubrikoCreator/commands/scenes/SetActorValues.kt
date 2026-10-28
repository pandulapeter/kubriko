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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.fits
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_actor_values
import kubriko.creator.generated.resources.error_command_actor_property_missing
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_property_value_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_not_block_built
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kubriko.creator.generated.resources.error_command_size_invalid

internal object SetActorValues : WriteCommand<SetActorValues.Input> {
    override val name = "set_actor_values"
    override val description = "Changes values of one Actor of a Scene: where it is, its size, rotation, and scale, and for block-built Actor types, " +
        "its color and the values of its Properties (each Actor keeps its own). Values left out stay as they are."
    override val title = Res.string.command_set_actor_values
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorId) ?: run {
        val actor = context.readScene(input.scene).requireActor(input.actorId)
        val definition = context.blockTypes?.get(actor.typeId) as? ActorTypeDefinition
        val numbers = listOfNotNull(input.x, input.y, input.width, input.height, input.rotation, input.scaleHorizontal, input.scaleVertical)
        when {
            SceneDocument.stateObject(actor.state) == null -> Rejection(Res.string.error_command_scene_content_invalid)
            numbers.any { !it.isFinite() } || input.properties.any { it.number?.isFinite() == false } -> Rejection(Res.string.error_command_number_invalid)
            listOfNotNull(input.width, input.height).any { it <= 0f } -> Rejection(Res.string.error_command_size_invalid)
            (input.color != null || input.properties.isNotEmpty()) && definition == null -> Rejection(Res.string.error_command_scene_actor_not_block_built, actor.typeId)
            else -> input.properties.firstNotNullOfOrNull { value ->
                val property = definition?.properties?.firstOrNull { it.name == value.name }
                    ?: return@firstNotNullOfOrNull Rejection(Res.string.error_command_actor_property_missing, actor.typeId, value.name)
                val kind = PropertyKind.of(property)
                if (value.toJson()?.fits(kind) == true) null else Rejection(Res.string.error_command_property_value_invalid, value.name)
            }
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val previous = actors.requireActor(input.actorId)
        val state = checkNotNull(
            ActorStateEdits.withChanges(
                previous.state,
                ActorStateEdits.Changes(
                    x = input.x,
                    y = input.y,
                    width = input.width,
                    height = input.height,
                    rotationInDegrees = input.rotation,
                    scaleHorizontal = input.scaleHorizontal,
                    scaleVertical = input.scaleVertical,
                    color = input.color,
                    properties = input.properties.associate { it.name to checkNotNull(it.toJson()) },
                ),
            ),
        )
        context.writeScene(input.scene, actors.withActorChanged(input.actorId) { it.copy(state = state) })
        return Applied(inverse = Invocation(SetActorState, SetActorState.Input(input.scene, input.actorId, previous.state)))
    }

    override fun mergeKey(input: Input) = listOf(
        input.scene, input.actorId, input.x != null, input.y != null, input.width != null, input.height != null, input.rotation != null,
        input.scaleHorizontal != null, input.scaleVertical != null, input.color != null, input.properties.map { it.name },
    )

    @Serializable
    data class Input(
        @Description("The name of the Scene: its file name without .json.")
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
        @SerialName("properties") val properties: List<PropertyValue> = emptyList(),
    )

    @Serializable
    @Description("The value of one Property: a number, a text, or a toggle, as the Property's kind is.")
    data class PropertyValue(
        @Description("The name of the Property.")
        @SerialName("name") val name: String,
        @Description("The value of a number Property.")
        @SerialName("number") val number: Float? = null,
        @Description("The value of a text Property.")
        @SerialName("text") val text: String? = null,
        @Description("The value of a toggle Property.")
        @SerialName("toggle") val toggle: Boolean? = null,
    ) {
        /**
         * The value as it is saved, or null unless exactly one kind of value is given.
         */
        fun toJson() = listOfNotNull(number?.let(::JsonPrimitive), text?.let(::JsonPrimitive), toggle?.let(::JsonPrimitive)).singleOrNull()
    }
}
