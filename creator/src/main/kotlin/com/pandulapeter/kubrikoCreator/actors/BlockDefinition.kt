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
    @Description("Sets a number Property of the type, or of a Manager type.")
    data class SetNumber(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The new value.")
        @SerialName("value") val value: Float = 0f,
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : BlockDefinition

    @Serializable
    @SerialName("changeNumber")
    @Description("Adds to a number Property of the type, or of a Manager type (or subtracts from it, with a negative amount).")
    data class ChangeNumber(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The amount added.")
        @SerialName("by") val by: Float = 1f,
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : BlockDefinition

    @Serializable
    @SerialName("setText")
    @Description("Sets a text Property of the type, or of a Manager type.")
    data class SetText(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The new text.")
        @SerialName("value") val value: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : BlockDefinition

    @Serializable
    @SerialName("setToggle")
    @Description("Turns a toggle Property of the type, or of a Manager type, on or off.")
    data class SetToggle(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("Whether it is on.")
        @SerialName("value") val value: Boolean = true,
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : BlockDefinition

    @Serializable
    @SerialName("addActor")
    @Description("Adds a new Actor of a block-built Actor type to the Scene, with that type's size and Property values, like one placed in the Scene editor. In a Manager type, the position is always relative to the center of the Scene.")
    data class AddActor(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String = "",
        @Description("The horizontal position, positive to the right.")
        @SerialName("x") val x: Float = 0f,
        @Description("The vertical position, positive downwards.")
        @SerialName("y") val y: Float = 0f,
        @Description("True to place it relative to this Actor, false to place it relative to the center of the Scene.")
        @SerialName("isRelative") val isRelative: Boolean = true,
    ) : BlockDefinition

    @Serializable
    @SerialName("moveTowardsNearest")
    @Description("Sets the velocity of the Actor towards the nearest other Actor of an Actor type, at a speed. Does nothing if there is none.")
    data class MoveTowardsNearest(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String = "",
        @Description("The speed, in scene units per second.")
        @SerialName("speed") val speed: Float = 200f,
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
    @SerialName("goToScene")
    @Description("Removes every Actor from the game and adds the Actors of another Scene instead. Manager types keep their Property values.")
    data class GoToScene(
        @Description("The name of the Scene.")
        @SerialName("scene") val scene: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("restartScene")
    @Description("Removes every Actor from the game and adds the Actors of the current Scene again, as they were placed. Manager types keep their Property values.")
    data object RestartScene : BlockDefinition

    @Serializable
    @SerialName("pauseGame")
    @Description("Pauses or resumes the game. While it is paused, Actors and Timers stop and every frame Events don't run, but key pressed Events still do, so a key can resume it.")
    data class PauseGame(
        @Description("Whether it pauses the game, resumes it, or switches between the two.")
        @SerialName("action") val action: PauseAction = PauseAction.TOGGLE,
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
    @Description("Writes a line to Kubriko Creator's console while the game runs, with the type as its source. The way to see what happens in the game: println doesn't reach the console.")
    data class Log(
        @Description("The text of the line. {name} is replaced by the value of the Property called name, and {Other.name} by a Property of the Manager type Other, as in \"Score: {Game.score}\".")
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

private const val PROPERTY_DESCRIPTION = "The name of one of the Properties of this kind, of the type itself or of the Manager type in manager."
internal const val MANAGER_DESCRIPTION = "The Manager type whose Property it uses. Left out for a Property of the type itself."

/**
 * The Property [this] Block changes (or checks, in an if Block), if it uses one.
 */
internal val BlockDefinition.propertyReference: PropertyReference?
    get() = when (this) {
        is BlockDefinition.SetNumber -> PropertyReference(property, PropertyKind.NUMBER, manager)
        is BlockDefinition.ChangeNumber -> PropertyReference(property, PropertyKind.NUMBER, manager)
        is BlockDefinition.SetText -> PropertyReference(property, PropertyKind.TEXT, manager)
        is BlockDefinition.SetToggle -> PropertyReference(property, PropertyKind.TOGGLE, manager)
        is BlockDefinition.If -> when (val condition = condition) {
            is ConditionDefinition.Number -> PropertyReference(condition.property, PropertyKind.NUMBER, condition.manager)
            is ConditionDefinition.Text -> PropertyReference(condition.property, PropertyKind.TEXT, condition.manager)
            is ConditionDefinition.Toggle -> PropertyReference(condition.property, PropertyKind.TOGGLE, condition.manager)
            is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.Chance -> null
        }
        else -> null
    }

/**
 * The Actor type [this] Block adds or moves towards, if it is one of those and one is chosen.
 */
internal val BlockDefinition.actorTypeReference
    get() = when (this) {
        is BlockDefinition.AddActor -> actorType
        is BlockDefinition.MoveTowardsNearest -> actorType
        else -> null
    }?.ifEmpty { null }

/**
 * [this] Block with the Actor type it adds or moves towards renamed from [oldName] to [newName], if it is that one.
 */
internal fun BlockDefinition.withActorTypeRenamed(oldName: String, newName: String) = when {
    this is BlockDefinition.AddActor && actorType == oldName -> copy(actorType = newName)
    this is BlockDefinition.MoveTowardsNearest && actorType == oldName -> copy(actorType = newName)
    else -> this
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
 * [this] Block with the Property of [manager] (the type's own one, if null) that it changes, or shows in a Log Block,
 * renamed from [oldName] to [newName].
 */
internal fun BlockDefinition.withPropertyRenamed(oldName: String, newName: String, manager: String? = null) = when {
    propertyReference?.let { it.manager == manager && it.name == oldName } == true -> withPropertyReference(manager, newName)
    this is BlockDefinition.Log -> copy(message = PropertyPlaceholders.withPropertyRenamed(message, manager, oldName, newName))
    else -> this
}

/**
 * [this] Block with the Properties of the Manager type [oldName] it uses (or shows, in a Log Block) taken from [newName]
 * instead.
 */
internal fun BlockDefinition.withManagerRenamed(oldName: String, newName: String) = when {
    propertyReference?.manager == oldName -> withPropertyReference(newName, propertyReference!!.name)
    this is BlockDefinition.Log -> copy(message = PropertyPlaceholders.withManagerRenamed(message, oldName, newName))
    else -> this
}

/**
 * [this] Block using the Property called [name] of [manager] (the type's own one, if null) instead, if it is one that
 * uses a Property.
 */
internal fun BlockDefinition.withPropertyReference(manager: String?, name: String) = when (this) {
    is BlockDefinition.SetNumber -> copy(manager = manager, property = name)
    is BlockDefinition.ChangeNumber -> copy(manager = manager, property = name)
    is BlockDefinition.SetText -> copy(manager = manager, property = name)
    is BlockDefinition.SetToggle -> copy(manager = manager, property = name)
    is BlockDefinition.If -> when (val condition = condition) {
        is ConditionDefinition.Number -> copy(condition = condition.copy(manager = manager, property = name))
        is ConditionDefinition.Text -> copy(condition = condition.copy(manager = manager, property = name))
        is ConditionDefinition.Toggle -> copy(condition = condition.copy(manager = manager, property = name))
        is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.Chance -> this
    }
    else -> this
}
