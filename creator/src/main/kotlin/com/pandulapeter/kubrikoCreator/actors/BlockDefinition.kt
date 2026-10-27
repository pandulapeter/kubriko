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
    @SerialName("setNumber")
    @Description("Sets a number Property of the Actor.")
    data class SetNumber(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The new value.")
        @SerialName("value") val value: Float = 0f,
    ) : BlockDefinition

    @Serializable
    @SerialName("changeNumber")
    @Description("Adds to a number Property of the Actor (or subtracts from it, with a negative amount).")
    data class ChangeNumber(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The amount added.")
        @SerialName("by") val by: Float = 1f,
    ) : BlockDefinition

    @Serializable
    @SerialName("setText")
    @Description("Sets a text Property of the Actor.")
    data class SetText(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The new text.")
        @SerialName("value") val value: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("setToggle")
    @Description("Turns a toggle Property of the Actor on or off.")
    data class SetToggle(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("Whether it is on.")
        @SerialName("value") val value: Boolean = true,
    ) : BlockDefinition

    @Serializable
    @SerialName("startTimer")
    @Description("Starts one of the Actor's Timers, or starts it again from the beginning. When it finishes, the timerFinished Event of that Timer runs.")
    data class StartTimer(
        @Description("Which Timer, from 1 to 8.")
        @SerialName("timer") val timer: Int = 1,
        @Description("How long it takes, in seconds.")
        @SerialName("seconds") val seconds: Float = 1f,
        @Description("True to start it again each time it finishes, until a stopTimer Block stops it.")
        @SerialName("isRepeating") val isRepeating: Boolean = false,
    ) : BlockDefinition

    @Serializable
    @SerialName("stopTimer")
    @Description("Stops one of the Actor's Timers, so that it doesn't finish.")
    data class StopTimer(
        @Description("Which Timer, from 1 to 8.")
        @SerialName("timer") val timer: Int = 1,
    ) : BlockDefinition

    @Serializable
    @SerialName("if")
    @Description("Runs the Blocks after it, up to the matching else or end Block, only when its condition is true. An if Block without an end runs to the end of the Event.")
    data class If(
        @Description("What it checks.")
        @SerialName("condition") val condition: ConditionDefinition = ConditionDefinition.Chance(),
    ) : BlockDefinition

    @Serializable
    @SerialName("else")
    @Description("Between an if Block and its end: the Blocks after it run only when the if Block's condition is false.")
    data object Else : BlockDefinition

    @Serializable
    @SerialName("repeat")
    @Description("Runs the Blocks after it, up to the matching end Block, a number of times.")
    data class Repeat(
        @Description("How many times, from 0.")
        @SerialName("times") val times: Int = 3,
    ) : BlockDefinition

    @Serializable
    @SerialName("end")
    @Description("Ends the Blocks of the closest if or repeat Block before it that isn't ended yet.")
    data object End : BlockDefinition

    @Serializable
    @SerialName("log")
    @Description("Writes a line to Kubriko Creator's console while the game runs, with the Actor type as its source. The way to see what happens in the game: println doesn't reach the console.")
    data class Log(
        @Description("The text of the line. {name} is replaced by the value of the Property called name, as in \"Score: {score}\".")
        @SerialName("message") val message: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("kotlin")
    @Description("Kotlin statements inserted verbatim into the Event, in a scope of their own, for anything the other Blocks can't express. In scope: velocity (a SceneOffset in scene units per second), body (the BoxBody of the Actor), color, the Actor type's Properties (number Properties are Floats), actorManager, and viewportManager. println doesn't reach the console; use the log Block, or Logger.log.")
    data class Kotlin(
        @Description("The Kotlin statements.")
        @SerialName("code") val code: String = "",
    ) : BlockDefinition
}

private const val PROPERTY_DESCRIPTION = "The name of one of the Actor type's Properties of this kind."

/**
 * The Property [this] Block changes (or checks, in an if Block), if it uses one: its name, and the kind of Property it needs.
 */
internal val BlockDefinition.propertyReference: Pair<String, PropertyKind>?
    get() = when (this) {
        is BlockDefinition.SetNumber -> property to PropertyKind.NUMBER
        is BlockDefinition.ChangeNumber -> property to PropertyKind.NUMBER
        is BlockDefinition.SetText -> property to PropertyKind.TEXT
        is BlockDefinition.SetToggle -> property to PropertyKind.TOGGLE
        is BlockDefinition.If -> when (val condition = condition) {
            is ConditionDefinition.Number -> condition.property to PropertyKind.NUMBER
            is ConditionDefinition.Text -> condition.property to PropertyKind.TEXT
            is ConditionDefinition.Toggle -> condition.property to PropertyKind.TOGGLE
            is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.Chance -> null
        }
        else -> null
    }

/**
 * The Timer [this] Block starts or stops, if it is one of those.
 */
internal val BlockDefinition.timerNumber
    get() = when (this) {
        is BlockDefinition.StartTimer -> timer
        is BlockDefinition.StopTimer -> timer
        else -> null
    }

/**
 * [this] Block with the Property it changes (or shows, in a Log Block) renamed from [oldName] to [newName].
 */
internal fun BlockDefinition.withPropertyRenamed(oldName: String, newName: String) = when {
    propertyReference?.first == oldName -> withProperty(newName)
    this is BlockDefinition.Log -> copy(message = message.replace("{$oldName}", "{$newName}"))
    else -> this
}

/**
 * [this] Block using the Property called [name] instead, if it is one that uses a Property.
 */
internal fun BlockDefinition.withProperty(name: String) = when (this) {
    is BlockDefinition.SetNumber -> copy(property = name)
    is BlockDefinition.ChangeNumber -> copy(property = name)
    is BlockDefinition.SetText -> copy(property = name)
    is BlockDefinition.SetToggle -> copy(property = name)
    is BlockDefinition.If -> when (val condition = condition) {
        is ConditionDefinition.Number -> copy(condition = condition.copy(property = name))
        is ConditionDefinition.Text -> copy(condition = condition.copy(property = name))
        is ConditionDefinition.Toggle -> copy(condition = condition.copy(property = name))
        is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.Chance -> this
    }
    else -> this
}
