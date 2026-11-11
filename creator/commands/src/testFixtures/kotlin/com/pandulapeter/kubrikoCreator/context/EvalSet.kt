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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Realistic requests on [BreakoutFixture], each with a check of what the project has to become, worded loosely enough
 * that any reasonable way of doing it passes. `EvalSetTest` replays each one's [EvalRecordings] for free, and the live
 * run (`EvalSetLiveTest`) has the real agent answer each one, recording what it called.
 */
public object EvalSet {

    /**
     * @param id Names its recording ([EvalRecordings]).
     * @param text What the user asks, word for word.
     * @param check Fails when the project isn't what [text] asks for.
     */
    public class Request(
        public val id: String,
        public val text: String,
        public val check: (CommandContext) -> Unit,
    )

    public val requests: List<Request> = listOf(
        Request("ball-starts-faster", "Make the Ball start faster.") { context -> assertBallStartsFaster(context) },
        Request("bat", "Add a Bat that the player moves with A and D, and place it in level-1.") { context ->
            val keys = context.actorType("Bat").events.filter { it.type == EventType.KEY_PRESSED }.map { it.key }
            assertTrue(KeyChoice.of("A") in keys && KeyChoice.of("D") in keys, "The Bat reacts to $keys.")
            assertTrue(SceneDocument.parse(context.scenes.read("level-1")!!).any { it.typeId == "Bat" }, "No Bat is placed in level-1.")
        },
        Request("bricks-give-20-points", "Make every Brick give 20 points.") { context ->
            val scoring = context.blocks().filterIsInstance<BlockDefinition.ChangeNumber>().filter { it.property == "score" }
            assertEquals(listOf(20f), scoring.map { it.by }.distinct(), "The score changes by ${scoring.map { it.by }}.")
        },
        Request("log-when-the-game-starts", "Log a message when the game starts.") { context ->
            val events = context.blockTypes!!.types.value.filterIsInstance<ManagerTypeDefinition>().flatMap { it.events }
            assertTrue(events.any { event -> event.type == EventType.GAME_STARTED && event.blocks.any { it is BlockDefinition.Log } }, "No Manager type logs when the game starts.")
        },
        Request("score-at-the-top", "Show the score at the top center of the screen instead of the top left.") { context ->
            val score = context.managerType("Game").texts.filter { "{score}" in it.text }
            assertEquals(listOf(ScreenPosition.TOP), score.map { it.position }, "The score is shown at ${score.map { it.position }}.")
        },
        Request("screenplay-follows-a-change", "Write the first page of the screenplay, then make the Ball start faster and say so there.") { context ->
            assertBallStartsFaster(context)
            val page = context.screenplay.read("README").orEmpty()
            assertTrue(Regex("fast|speed|quick", RegexOption.IGNORE_CASE).containsMatchIn(page), "The first page doesn't say the Ball is faster: $page")
        },
        Request(
            "paddle-stops-on-release",
            "The Paddle should stop once neither arrow key is held, but now it stops whenever Left isn't held, even while Right is.",
        ) { context ->
            val everyFrame = context.actorType("Paddle").events.filter { it.type == EventType.EVERY_FRAME }.map { it.blocks }
            listOf(emptySet(), setOf(KeyChoice.LEFT), setOf(KeyChoice.RIGHT), setOf(KeyChoice.LEFT, KeyChoice.RIGHT)).forEach { held ->
                val stops = everyFrame.map { stopsWhile(it, held) }
                assertTrue(null !in stops, "The Paddle's Every frame Events check more than which keys are held: $everyFrame")
                assertEquals(held.isEmpty(), true in stops, "With $held held, the Paddle ${if (true in stops) "stops" else "doesn't stop"}.")
            }
        },
    )

    /**
     * Whether [blocks] reach a Stop Block while the [held] keys are held, following their if, else, repeat, and end
     * Blocks, or null when one of their conditions is anything but which keys are held.
     */
    private fun stopsWhile(blocks: List<BlockDefinition>, held: Set<KeyChoice>): Boolean? {
        val sections = ArrayDeque<Pair<Boolean, Boolean>>()
        fun isRunning() = sections.all { (isTrue, _) -> isTrue }
        blocks.forEach { block ->
            when (block) {
                is BlockDefinition.If -> {
                    val condition = block.condition as? ConditionDefinition.KeyHeld ?: return null
                    val isTrue = (condition.key in held) == condition.isHeld
                    sections.addLast(isTrue to isTrue)
                }
                is BlockDefinition.Repeat -> sections.addLast(true to true)
                BlockDefinition.Else -> sections.removeLastOrNull()?.let { (_, condition) -> sections.addLast(!condition to condition) }
                BlockDefinition.End -> sections.removeLastOrNull()
                BlockDefinition.Stop -> if (isRunning()) return true
                else -> Unit
            }
        }
        return false
    }

    private fun assertBallStartsFaster(context: CommandContext) {
        val start = context.actorType("Ball").events.first { it.type == EventType.ADDED_TO_SCENE }.blocks.filterIsInstance<BlockDefinition.SetVelocity>().last()
        assertTrue(hypot(start.x, start.y) > hypot(200f, -300f), "The Ball starts at $start.")
    }

    private fun CommandContext.blocks() = blockTypes!!.types.value.flatMap { type -> type.events.flatMap { it.blocks } }

    private fun CommandContext.actorType(name: String) = blockTypes!!.get(name) as ActorTypeDefinition

    private fun CommandContext.managerType(name: String) = blockTypes!!.get(name) as ManagerTypeDefinition
}
