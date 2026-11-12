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
import com.pandulapeter.kubrikoCreator.schema.SummarizedSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One action an Event performs. Each one becomes a few lines of ordinary Kotlin in the generated Actor type.
 *
 * Speeds and velocities are in scene units per second. Every number is a [NumberValue], so it can also be random or a
 * Property's value.
 */
@Serializable
@Description("A Block: its type and that type's properties.")
@SummarizedSchema("Which Block; get_block_catalog lists their properties and the Events allowing them.")
public sealed interface BlockDefinition {

    @Serializable
    @SerialName("setVelocity")
    @Description("Sets the velocity of the Actor, in scene units per second.")
    public data class SetVelocity(
        @Description("The horizontal velocity, positive to the right.")
        @SerialName("x") val x: NumberValue = NumberValue.Fixed(0f),
        @Description("The vertical velocity, positive downwards.")
        @SerialName("y") val y: NumberValue = NumberValue.Fixed(0f),
    ) : BlockDefinition

    @Serializable
    @SerialName("addToVelocity")
    @Description("Adds to the velocity of the Actor, in scene units per second.")
    public data class AddToVelocity(
        @Description("Added to the horizontal velocity, positive to the right.")
        @SerialName("x") val x: NumberValue = NumberValue.Fixed(0f),
        @Description("Added to the vertical velocity, positive downwards.")
        @SerialName("y") val y: NumberValue = NumberValue.Fixed(0f),
    ) : BlockDefinition

    @Serializable
    @SerialName("runParentEvent")
    @Description("Runs the Blocks of the parent Actor type's Event that this Event replaces (GameMaker's event_inherited), here. Only in an Actor type with a parent.")
    public data object RunParentEvent : BlockDefinition

    @Serializable
    @SerialName("stop")
    @Description("Sets the velocity of the Actor to zero.")
    public data object Stop : BlockDefinition

    @Serializable
    @SerialName("jumpTo")
    @Description("Moves the Actor to a position in the Scene at once.")
    public data class JumpTo(
        @Description("The horizontal position, where 0 is the center of the Scene.")
        @SerialName("x") val x: NumberValue = NumberValue.Fixed(0f),
        @Description("The vertical position, where 0 is the center of the Scene, positive downwards.")
        @SerialName("y") val y: NumberValue = NumberValue.Fixed(0f),
    ) : BlockDefinition

    @Serializable
    @SerialName("bounceOffEdge")
    @Description("Turns the velocity of the Actor away from the edge of the screen it touched. Only allowed in edgeTouched Events.")
    public data object BounceOffEdge : BlockDefinition

    @Serializable
    @SerialName("removeSelf")
    @Description("Removes the Actor from the Scene.")
    public data object RemoveSelf : BlockDefinition

    @Serializable
    @SerialName("bounceOffOther")
    @Description("Turns the velocity of the Actor away from the Actor it collided with, along the axis it was hit on. Only allowed in collision Events.")
    public data object BounceOffOther : BlockDefinition

    @Serializable
    @SerialName("removeOther")
    @Description("Removes the Actor this one collided with from the Scene. Only allowed in collision Events.")
    public data object RemoveOther : BlockDefinition

    @Serializable
    @SerialName("setNumber")
    @Description("Sets a number Property of the type, or of a Manager type.")
    public data class SetNumber(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The new value.")
        @SerialName("value") val value: NumberValue = NumberValue.Fixed(0f),
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : BlockDefinition

    @Serializable
    @SerialName("changeNumber")
    @Description("Adds to a number Property of the type, or of a Manager type (or subtracts from it, with a negative amount).")
    public data class ChangeNumber(
        @Description(PROPERTY_DESCRIPTION)
        @SerialName("property") val property: String = "",
        @Description("The amount added.")
        @SerialName("by") val by: NumberValue = NumberValue.Fixed(1f),
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : BlockDefinition

    @Serializable
    @SerialName("setText")
    @Description("Sets a text Property of the type, or of a Manager type.")
    public data class SetText(
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
    public data class SetToggle(
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
    public data class AddActor(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String = "",
        @Description("The horizontal position, positive to the right.")
        @SerialName("x") val x: NumberValue = NumberValue.Fixed(0f),
        @Description("The vertical position, positive downwards.")
        @SerialName("y") val y: NumberValue = NumberValue.Fixed(0f),
        @Description("True to place it relative to this Actor, false to place it relative to the center of the Scene.")
        @SerialName("isRelative") val isRelative: Boolean = true,
    ) : BlockDefinition

    @Serializable
    @SerialName("moveTowardsNearest")
    @Description("Sets the velocity of the Actor towards the nearest other Actor of an Actor type, at a speed. Does nothing if there is none.")
    public data class MoveTowardsNearest(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String = "",
        @Description("The speed, in scene units per second.")
        @SerialName("speed") val speed: NumberValue = NumberValue.Fixed(200f),
    ) : BlockDefinition

    @Serializable
    @SerialName("startTimer")
    @Description("Starts one of the Actor's Timers, or starts it again from the beginning. When it finishes, the timerFinished Event of that Timer runs.")
    public data class StartTimer(
        @Description("The name of the Timer, like spawnEnemy: a letter, then letters and digits, starting lowercase. A Timer exists once a Block or an Event names it.")
        @Serializable(with = TimerNameSerializer::class)
        @SerialName("timer") val timer: String = TimerNaming.DEFAULT_NAME,
        @Description("How long it takes, in seconds. A random number or a Property's value is worked out when the Block runs, and a repeating Timer keeps it for every round; for a new one each round, start the Timer again in its timerFinished Event instead.")
        @SerialName("seconds") val seconds: NumberValue = NumberValue.Fixed(1f),
        @Description("True to start it again each time it finishes, until a stopTimer Block stops it.")
        @SerialName("isRepeating") val isRepeating: Boolean = false,
    ) : BlockDefinition

    @Serializable
    @SerialName("stopTimer")
    @Description("Stops one of the Actor's Timers, so that it doesn't finish.")
    public data class StopTimer(
        @Description("The name of the Timer, like spawnEnemy: a letter, then letters and digits, starting lowercase. A Timer exists once a Block or an Event names it.")
        @Serializable(with = TimerNameSerializer::class)
        @SerialName("timer") val timer: String = TimerNaming.DEFAULT_NAME,
    ) : BlockDefinition

    @Serializable
    @SerialName("goToScene")
    @Description("Removes every Actor from the game and adds the Actors of another Scene instead. Manager types keep their Property values.")
    public data class GoToScene(
        @Description("The name of the Scene.")
        @SerialName("scene") val scene: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("restartScene")
    @Description("Removes every Actor from the game and adds the Actors of the current Scene again, as they were placed. Manager types keep their Property values.")
    public data object RestartScene : BlockDefinition

    @Serializable
    @SerialName("pauseGame")
    @Description("Pauses or resumes the game. While it is paused, Actors and Timers stop, and neither every frame Events nor the key pressed Events of Actor types run, but those of Manager types still do, so a key can resume it.")
    public data class PauseGame(
        @Description("Whether it pauses the game, resumes it, or switches between the two.")
        @SerialName("action") val action: PauseAction = PauseAction.TOGGLE,
    ) : BlockDefinition

    @Serializable
    @SerialName("playSound")
    @Description("Plays a sound effect once. Several sounds, and the same sound several times, can play at once.")
    public data class PlaySound(
        @Description(SOUND_DESCRIPTION)
        @SerialName("sound") val sound: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("playMusic")
    @Description("Plays music instead of the music that plays now (if it is the same music, it keeps playing). It pauses while the game's window isn't focused, and goes on when it is again.")
    public data class PlayMusic(
        @Description(MUSIC_DESCRIPTION)
        @SerialName("music") val music: String = "",
        @Description("True to start it again each time it ends, false to play it once.")
        @SerialName("isLooping") val isLooping: Boolean = true,
    ) : BlockDefinition

    @Serializable
    @SerialName("stopMusic")
    @Description("Stops the music that plays, whether a playMusic Block or the Scene settings started it.")
    public data object StopMusic : BlockDefinition

    @Serializable
    @SerialName("setFrame")
    @Description("Shows a frame of the Actor's sprite, when its sprite is cut into frames; the animation goes on from there.")
    public data class SetFrame(
        @Description("The frame, from 0 for the first; one past the last shows the last.")
        @Serializable(with = WholeNumberValueSerializer::class)
        @SerialName("frame") val frame: NumberValue = NumberValue.Fixed(0f),
    ) : BlockDefinition

    @Serializable
    @SerialName("setAnimation")
    @Description("Sets how the Actor's sprite plays its frames, when it is cut into them. Actors start at speed 1, looping.")
    public data class SetAnimation(
        @Description("How fast the frames play: 1 for the sprite's own frames per second, 2 for twice as fast, 0 to stop on the frame shown, and below 0 to play backwards.")
        @SerialName("speed") val speed: NumberValue = NumberValue.Fixed(1f),
        @Description("Whether the animation starts over after its last frame, rather than stopping on it.")
        @SerialName("isLooping") val isLooping: Boolean = true,
    ) : BlockDefinition

    @Serializable
    @SerialName("if")
    @Description("Runs the Blocks after it, up to the matching else or end Block, only when its condition is true. An if Block without an end runs to the end of the Event.")
    public data class If(
        @Description("What it checks.")
        @SerialName("condition") val condition: ConditionDefinition = ConditionDefinition.Chance(),
    ) : BlockDefinition

    @Serializable
    @SerialName("else")
    @Description("Between an if Block and its end: the Blocks after it run only when the if Block's condition is false.")
    public data object Else : BlockDefinition

    @Serializable
    @SerialName("repeat")
    @Description("Runs the Blocks after it, up to the matching end Block, a number of times.")
    public data class Repeat(
        @Description("How many times, from 0.")
        @Serializable(with = WholeNumberValueSerializer::class)
        @SerialName("times") val times: NumberValue = NumberValue.Fixed(3f),
    ) : BlockDefinition

    @Serializable
    @SerialName("end")
    @Description("Ends the Blocks of the closest if or repeat Block before it that isn't ended yet.")
    public data object End : BlockDefinition

    @Serializable
    @SerialName("log")
    @Description("Writes a line to Kubriko Creator's console while the game runs, with the type as its source. The way to see what happens in the game: println doesn't reach the console.")
    public data class Log(
        @Description("The text of the line. {name} is replaced by the value of the Property called name, and {Other.name} by a Property of the Manager type Other, as in \"Score: {Game.score}\".")
        @SerialName("message") val message: String = "",
    ) : BlockDefinition

    @Serializable
    @SerialName("kotlin")
    @Description("Kotlin statements inserted verbatim into the Event, in a scope of their own, for anything the other Blocks can't express. In scope: the type's Properties (number Properties are Floats), actorManager, and viewportManager, and in an Actor type also velocity (a SceneOffset in scene units per second), body (the BoxBody of the Actor), and color. Also deltaTimeInMilliseconds (an Int) in every-frame, edge-touched, and Timer Events, kubriko in added-to-the-Scene and game-started Events, and other (the Actor collided with) in collision Events. `return` ends this Event's Blocks, and those of the type's later Events of the same kind. Leading import lines become imports of the generated file. println doesn't reach the console; use the log Block (Logger.log reaches it only from the desktop).")
    public data class Kotlin(
        @Description("The Kotlin statements.")
        @SerialName("code") val code: String = "",
    ) : BlockDefinition
}

private const val PROPERTY_DESCRIPTION = "The name of one of the Properties of this kind, of the type itself or of the Manager type in manager."
internal const val SOUND_DESCRIPTION = "The sound: a .wav file in the project's composeResources, as its path from there, like files/sounds/hit.wav."
/**
 * How a music field is described to the AI, which the command that changes a Scene's settings uses too.
 */
public const val MUSIC_DESCRIPTION: String = "The music: an .mp3 file in the project's composeResources, as its path from there, like files/music/theme.mp3."
internal const val MANAGER_DESCRIPTION = "The Manager type whose Property it uses. Left out for a Property of the type itself."
