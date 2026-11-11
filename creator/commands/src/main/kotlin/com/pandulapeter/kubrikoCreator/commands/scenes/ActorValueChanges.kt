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
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.actorTypes.fits
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * The values of one Actor of a Scene that [SetActorValues] and [SetActorsValues] change; those left null stay as they are.
 */
@Serializable
@Description("The values to change of one Actor of the Scene. Values left out stay as they are.")
public data class ActorValueChanges(
    @Description(Descriptions.SCENE_ACTOR_ID)
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
) {
    @Serializable
    @Description("The value of one Property: a number, a text, or a toggle, as the Property's kind is.")
    public data class PropertyValue(
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
        internal fun toJson() = listOfNotNull(number?.let(::JsonPrimitive), text?.let(::JsonPrimitive), toggle?.let(::JsonPrimitive)).singleOrNull()
    }
}

/**
 * Why [changes] can't be made to [actor], or null if they can.
 */
internal fun CommandContext.validateActorValues(actor: SceneActorEntry, changes: ActorValueChanges): Rejection? {
    val definition = blockTypes?.get(actor.typeId) as? ActorTypeDefinition
    val numbers = with(changes) { listOfNotNull(x, y, width, height, rotation, scaleHorizontal, scaleVertical) }
    return when {
        SceneDocument.stateObject(actor.state) == null -> Rejection(CommandProblem.SCENE_CONTENT_INVALID)
        numbers.any { !it.isFinite() } || changes.properties.any { it.number?.isFinite() == false } -> Rejection(CommandProblem.NUMBER_INVALID)
        listOfNotNull(changes.width, changes.height).any { it <= 0f } -> Rejection(CommandProblem.SIZE_INVALID)
        (changes.color != null || changes.properties.isNotEmpty()) && definition == null -> Rejection(CommandProblem.SCENE_ACTOR_NOT_BLOCK_BUILT, actor.typeId)
        else -> changes.properties.firstNotNullOfOrNull { value ->
            val property = definition?.properties?.firstOrNull { it.name == value.name }
                ?: return@firstNotNullOfOrNull SimilarNames.rejection(
                    CommandProblem.ACTOR_PROPERTY_MISSING,
                    CommandProblem.ACTOR_PROPERTY_MISSING_SIMILAR,
                    value.name,
                    definition?.properties.orEmpty().map { it.name },
                    actor.typeId,
                    value.name,
                )
            if (value.toJson()?.fits(PropertyKind.of(property)) == true) null else Rejection(CommandProblem.PROPERTY_VALUE_INVALID, value.name)
        }
    }
}

/**
 * [state] with [changes] made, which [validateActorValues] has accepted.
 */
internal fun ActorValueChanges.applyTo(state: String) = checkNotNull(
    ActorStateEdits.withChanges(
        state,
        ActorStateEdits.Changes(
            x = x,
            y = y,
            width = width,
            height = height,
            rotationInDegrees = rotation,
            scaleHorizontal = scaleHorizontal,
            scaleVertical = scaleVertical,
            color = color,
            properties = properties.associate { it.name to checkNotNull(it.toJson()) },
        ),
    ),
)
