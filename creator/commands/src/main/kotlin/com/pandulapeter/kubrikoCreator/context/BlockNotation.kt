/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import com.pandulapeter.kubrikoCreator.actors.readsEdges

/**
 * Events and Blocks written as the editor shows them, one short line each, for tool results that are cheaper to read than
 * their JSON: `When it collides with Brick` and `Change score of Game by 10`. Values left at nothing read `(none)`.
 * It is for the AI, so it stays in English, like the tools' descriptions.
 */
internal object BlockNotation {

    fun of(event: EventDefinition): String = when (event.type) {
        EventType.GAME_STARTED -> "When the game starts"
        EventType.ADDED_TO_SCENE -> "When added to the Scene"
        EventType.EVERY_FRAME -> "Every frame"
        EventType.KEY_PRESSED -> "When ${event.key?.composeKeyName ?: NONE} is pressed"
        EventType.EDGE_TOUCHED -> "When an edge of the screen is touched"
        EventType.LEFT_SCENE -> "When it leaves the Scene"
        EventType.COLLISION -> "When it collides with ${event.otherActorType ?: NONE}"
        EventType.TIMER_FINISHED -> "When Timer ${event.timer ?: NONE} finishes"
    }

    /**
     * The Event's title and its Blocks, each on a line of its own after its index, indented by the if and repeat Blocks
     * it is in.
     */
    fun withBlocks(event: EventDefinition): String {
        val (nesting, _) = BlockNesting.of(event.blocks)
        return buildString {
            append(of(event))
            event.blocks.forEachIndexed { index, block ->
                append('\n')
                append("  ".repeat(nesting[index].depth + 1))
                append(index)
                append(' ')
                append(of(block))
            }
        }
    }

    fun of(block: BlockDefinition): String = when (block) {
        is BlockDefinition.SetVelocity -> "Set velocity to ${point(block.x, block.y)}"
        is BlockDefinition.AddToVelocity -> "Add ${point(block.x, block.y)} to velocity"
        BlockDefinition.Stop -> "Stop"
        BlockDefinition.RunParentEvent -> "Run the parent's Event"
        is BlockDefinition.JumpTo -> "Jump to position ${point(block.x, block.y)}"
        BlockDefinition.BounceOffEdge -> "Bounce off the edge"
        BlockDefinition.RemoveSelf -> "Remove this Actor"
        BlockDefinition.BounceOffOther -> "Bounce off it"
        BlockDefinition.RemoveOther -> "Remove the other Actor"
        is BlockDefinition.SetNumber -> "Set ${property(block.property, block.manager)} to ${value(block.value)}"
        is BlockDefinition.ChangeNumber -> "Change ${property(block.property, block.manager)} by ${value(block.by)}"
        is BlockDefinition.SetText -> "Set ${property(block.property, block.manager)} to ${text(block.value)}"
        is BlockDefinition.SetToggle -> "Turn ${property(block.property, block.manager)} ${if (block.value) "on" else "off"}"
        is BlockDefinition.AddActor -> "Add an Actor of ${name(block.actorType)} at ${point(block.x, block.y)} " + when {
            !block.isRelative -> "from the center"
            block.x.readsEdges || block.y.readsEdges -> "from this Actor (an edge is the Scene's own)"
            else -> "from this Actor"
        }
        is BlockDefinition.MoveTowardsNearest -> "Move towards the nearest ${name(block.actorType)} at speed ${value(block.speed)}"
        is BlockDefinition.StartTimer -> "Start Timer ${block.timer} for ${value(block.seconds)} s" + if (block.isRepeating) ", repeating" else ""
        is BlockDefinition.StopTimer -> "Stop Timer ${block.timer}"
        is BlockDefinition.GoToScene -> "Go to Scene ${name(block.scene)}"
        BlockDefinition.RestartScene -> "Restart the Scene"
        is BlockDefinition.PauseGame -> when (block.action) {
            PauseAction.PAUSE -> "Pause the game"
            PauseAction.RESUME -> "Resume the game"
            PauseAction.TOGGLE -> "Pause or resume the game"
        }
        is BlockDefinition.PlaySound -> "Play sound ${name(block.sound)}"
        is BlockDefinition.PlayMusic -> "Play music ${name(block.music)}" + if (block.isLooping) "" else ", once"
        BlockDefinition.StopMusic -> "Stop the music"
        is BlockDefinition.SetFrame -> "Show frame ${value(block.frame)} of the sprite"
        is BlockDefinition.SetAnimation -> "Play the sprite's frames at speed ${value(block.speed)}" + if (block.isLooping) ", looping" else ", once"
        is BlockDefinition.If -> "If ${of(block.condition)}"
        BlockDefinition.Else -> "Else"
        is BlockDefinition.Repeat -> "Repeat ${value(block.times)} times"
        BlockDefinition.End -> "End"
        is BlockDefinition.Log -> "Write to the console ${text(block.message)}"
        is BlockDefinition.Kotlin -> "Run Kotlin code ${text(block.code)}"
    }

    fun of(condition: ConditionDefinition): String = when (condition) {
        is ConditionDefinition.Number -> "${property(condition.property, condition.manager)} ${of(condition.comparison)} ${value(condition.value)}"
        is ConditionDefinition.Text -> "${property(condition.property, condition.manager)} ${if (condition.isEqual) "is" else "isn't"} ${text(condition.value)}"
        is ConditionDefinition.Toggle -> "${property(condition.property, condition.manager)} ${if (condition.isOn) "is on" else "is off"}"
        is ConditionDefinition.KeyHeld -> "${condition.key.composeKeyName} ${if (condition.isHeld) "is held" else "isn't held"}"
        is ConditionDefinition.Velocity -> "the ${if (condition.axis == Axis.HORIZONTAL) "horizontal" else "vertical"} velocity ${of(condition.comparison)} ${value(condition.value)}"
        is ConditionDefinition.Chance -> "by chance (${value(condition.percent)}%)"
        is ConditionDefinition.Paused -> if (condition.isPaused) "the game is paused" else "the game isn't paused"
    }

    /**
     * [value] without a fraction when it has none: `200`, not `200.0`.
     */
    fun number(value: Float) = if (value.isFinite() && value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

    private fun of(comparison: Comparison) = when (comparison) {
        Comparison.LESS -> "is less than"
        Comparison.LESS_OR_EQUAL -> "is at most"
        Comparison.EQUAL -> "is"
        Comparison.NOT_EQUAL -> "isn't"
        Comparison.GREATER_OR_EQUAL -> "is at least"
        Comparison.GREATER -> "is more than"
    }

    /**
     * A number a Block takes: `200`, `a random number from 1 to 5`, `speed of Game`, `the right edge + 50`, or
     * `a random number from the top edge to the bottom edge`.
     */
    fun value(value: NumberValue) = when (value) {
        is NumberValue.Fixed -> number(value.value)
        is NumberValue.Random -> "a random ${if (value.isWhole) "whole " else ""}number from ${number(value.from)} to ${number(value.to)}"
        is NumberValue.Property -> property(value.property, value.manager)
        is NumberValue.Edge -> edge(value.edge, value.offset)
        is NumberValue.BetweenEdges -> "a random number from ${edge(value.from, value.fromOffset)} to ${edge(value.to, value.toOffset)}"
    }

    private fun edge(edge: SceneEdge, offset: Float) = "the ${edge.name.lowercase()} edge" + when {
        offset > 0f -> " + ${number(offset)}"
        offset < 0f -> " - ${number(-offset)}"
        else -> ""
    }

    private fun point(x: NumberValue, y: NumberValue) = "(${value(x)}, ${value(y)})"

    private fun property(name: String, manager: String?) = name(name) + (manager?.let { " of $it" } ?: "")

    private fun name(value: String) = value.ifEmpty { NONE }

    private fun text(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private const val NONE = "(none)"
}
