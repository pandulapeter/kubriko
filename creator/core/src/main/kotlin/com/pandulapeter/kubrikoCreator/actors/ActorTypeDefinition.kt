/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A block-built Actor type, as stored in `.kubriko/actors/<name>.json`. Kubriko Creator generates a Kotlin class from it.
 *
 * @param name The Actor type's name, which is also its Kotlin class name and its Scene type ID.
 * @param color The Actor's color as `0xAARRGGBB`, stored as `"#RRGGBB"` ([HexColorSerializer]).
 */
@Serializable
@Description("A block-built Actor type: how its Actors look, and the Events they react to with Blocks.")
public data class ActorTypeDefinition(
    @Description("The name of the Actor type, and of its generated Kotlin class: an uppercase letter, then letters and numbers.")
    @SerialName("name") override val name: String,
    @Description("What the Actor type is for in the game, in one short line (\"the ball the player bounces off the paddle\"), or none.")
    @SerialName("description") override val description: String? = null,
    @Description("The Actor type it inherits from, like GameMaker's parent objects: it gets the parent's Properties and Events (an Event of its own of the same kind replaces the parent's), and the parent stands for it in collision Events and other Blocks. None for no parent.")
    @SerialName("parent") val parent: String? = null,
    @Description("The shape its Actors are drawn as, and of their collision mask.")
    @SerialName("shape") val shape: ActorShape = ActorShape.CIRCLE,
    @Description("The image its Actors are drawn as instead of the shape, by its path from composeResources (drawable/ball.png), or none to draw the shape. The shape is still the collision mask's.")
    @SerialName("sprite") val sprite: String? = null,
    @Description("The color its Actors are drawn with, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
    @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long = 0xFF6060AA,
    @Description("Whether its sprite is drawn tinted with its color (multiplied by it, so white leaves it as it is). Its shape is always drawn in its color.")
    @SerialName("tintsSprite") val tintsSprite: Boolean = false,
    @Description("The width of a newly placed Actor, in scene units.")
    @SerialName("width") val width: Float = 80f,
    @Description("The height of a newly placed Actor, in scene units.")
    @SerialName("height") val height: Float = 80f,
    @Description(STAYS_INSIDE_SCENE_DESCRIPTION)
    @SerialName("staysInsideScene") val staysInsideScene: Boolean = false,
    @Description("The values each of its Actors keeps, like a score, which Blocks can change.")
    @SerialName("properties") override val properties: List<PropertyDefinition> = emptyList(),
    @Description("The Events its Actors react to, in order. Each runs its Blocks in order.")
    @SerialName("events") override val events: List<EventDefinition> = emptyList(),
    @Transient override val folder: String = "",
) : BlockTypeDefinition {
    override val kind: BlockTypeKind get() = BlockTypeKind.ACTOR

    override fun withName(name: String): ActorTypeDefinition = copy(name = name)

    override fun withDescription(description: String?): ActorTypeDefinition = copy(description = description)

    override fun withFolder(folder: String): ActorTypeDefinition = copy(folder = folder)

    override fun withProperties(properties: List<PropertyDefinition>): ActorTypeDefinition = copy(properties = properties)

    override fun withEvents(events: List<EventDefinition>): ActorTypeDefinition = copy(events = events)

    public companion object {

        /**
         * What [staysInsideScene] means, for every tool schema that sets it.
         */
        public const val STAYS_INSIDE_SCENE_DESCRIPTION: String = "Whether its Actors are pushed back inside the Scene's bounds whenever they reach past an edge, rather than moving on past it."
    }
}
