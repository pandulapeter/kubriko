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

import com.pandulapeter.kubrikoCreator.commands.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One action an Event performs. Each one becomes a few lines of ordinary Kotlin in the generated Actor type.
 *
 * Speeds and velocities are in scene units per second.
 */
@Serializable
@Description("One statement an Event runs. The type property says which Block it is.")
internal sealed interface BlockDefinition {

    @Serializable
    @SerialName("setVelocity")
    @Description("Sets the velocity of the Actor, in scene units per second.")
    data class SetVelocity(
        @Description("The horizontal velocity, positive to the right.")
        @SerialName("x") val x: Float = 0f,
        @Description("The vertical velocity, positive downwards.")
        @SerialName("y") val y: Float = 0f,
    ) : BlockDefinition

    @Serializable
    @SerialName("addToVelocity")
    @Description("Adds to the velocity of the Actor, in scene units per second.")
    data class AddToVelocity(
        @Description("Added to the horizontal velocity, positive to the right.")
        @SerialName("x") val x: Float = 0f,
        @Description("Added to the vertical velocity, positive downwards.")
        @SerialName("y") val y: Float = 0f,
    ) : BlockDefinition

    @Serializable
    @SerialName("stop")
    @Description("Sets the velocity of the Actor to zero.")
    data object Stop : BlockDefinition

    @Serializable
    @SerialName("jumpTo")
    @Description("Moves the Actor to a position in the Scene at once.")
    data class JumpTo(
        @Description("The horizontal position, where 0 is the center of the Scene.")
        @SerialName("x") val x: Float = 0f,
        @Description("The vertical position, where 0 is the center of the Scene, positive downwards.")
        @SerialName("y") val y: Float = 0f,
    ) : BlockDefinition

    @Serializable
    @SerialName("bounceOffEdge")
    @Description("Turns the velocity of the Actor away from the edge of the screen it touched. Only allowed in edgeTouched Events.")
    data object BounceOffEdge : BlockDefinition

    @Serializable
    @SerialName("removeSelf")
    @Description("Removes the Actor from the Scene.")
    data object RemoveSelf : BlockDefinition

    @Serializable
    @SerialName("bounceOffOther")
    @Description("Turns the velocity of the Actor away from the Actor it collided with, along the axis it was hit on. Only allowed in collision Events.")
    data object BounceOffOther : BlockDefinition

    @Serializable
    @SerialName("removeOther")
    @Description("Removes the Actor this one collided with from the Scene. Only allowed in collision Events.")
    data object RemoveOther : BlockDefinition

    @Serializable
    @SerialName("log")
    @Description("Writes a line to Kubriko Creator's console while the game runs, with the Actor type as its source. The way to see what happens in the game: println doesn't reach the console.")
    data class Log(
        @Description("The text of the line.")
        @SerialName("message") val message: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("kotlin")
    @Description("Kotlin statements inserted verbatim into the Event, in a scope of their own, for anything the other Blocks can't express. In scope: velocity (a SceneOffset in scene units per second), body (the BoxBody of the Actor), color, actorManager, and viewportManager. println doesn't reach the console; use the log Block, or Logger.log.")
    data class Kotlin(
        @Description("The Kotlin statements.")
        @SerialName("code") val code: String = "",
    ) : BlockDefinition
}
