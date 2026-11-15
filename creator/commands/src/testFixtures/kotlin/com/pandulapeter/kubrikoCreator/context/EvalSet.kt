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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ColorValue
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.actors.TextFont
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.scenes.ManagerSceneValues
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.json.floatOrNull
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Realistic requests on [BreakoutFixture] (or on an empty project), each with a check of what the project has to become,
 * worded loosely enough that any reasonable way of doing it passes. `EvalSetTest` replays each one's [EvalRecordings] for
 * free, and the live run (`EvalSetLiveTest`) has the real agent answer each one, recording what it called.
 */
public object EvalSet {

    /**
     * @param id Names its recording ([EvalRecordings]).
     * @param text What the user asks, word for word.
     * @param isBlank Whether it is asked of an empty project rather than of [BreakoutFixture].
     * @param check Fails when the project isn't what [text] asks for.
     */
    public class Request(
        public val id: String,
        public val text: String,
        public val isBlank: Boolean = false,
        public val check: (CommandContext) -> Unit,
    )

    public val requests: List<Request> = listOf(
        Request("ball-starts-faster", "Make the Ball start faster.") { context -> assertBallStartsFaster(context) },
        Request("bat", "Add a Bat that the player moves with A and D without it leaving the screen, and place it in level-1.") { context ->
            val bat = context.actorType("Bat")
            val keys = bat.events.filter { it.type == EventType.KEY_PRESSED }.map { it.key } +
                bat.events.flatMap { it.blocks }.mapNotNull { ((it as? BlockDefinition.If)?.condition as? ConditionDefinition.KeyHeld)?.key }
            val steersWithWasd = bat.events.any { event -> event.blocks.any { it is BlockDefinition.MoveWithKeys && it.keys.usesWasd } }
            assertTrue(steersWithWasd || (KeyChoice.of("A") in keys && KeyChoice.of("D") in keys), "The Bat reacts to $keys.")
            assertTrue(bat.staysInsideScene, "The Bat can leave the Scene.")
            assertTrue(SceneDocument.parse(context.scenes.read("level-1")!!).any { it.typeId == "Bat" }, "No Bat is placed in level-1.")
        },
        Request("bricks-give-20-points", "Make every Brick give 20 points.") { context ->
            val scoring = context.blocks().filterIsInstance<BlockDefinition.ChangeNumber>().filter { it.property == "score" }
            assertEquals(listOf<NumberValue>(NumberValue.Fixed(20f)), scoring.map { it.by }.distinct(), "The score changes by ${scoring.map { it.by }}.")
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
        Request("game-over-text", "Give Game an isOver toggle, and while it's on, show \"Game over\" in the middle of the screen in bold serif letters.") { context ->
            val game = context.managerType("Game")
            val toggles = game.properties.filterIsInstance<PropertyDefinition.Toggle>().map { it.name }
            val texts = game.texts.filter { it.text.contains("game over", ignoreCase = true) }
            assertTrue(
                texts.any { text ->
                    val shownWhen = text.shownWhen as? ConditionDefinition.Toggle
                    text.position == ScreenPosition.CENTER && text.font == TextFont.SERIF && text.isBold &&
                        shownWhen != null && shownWhen.isOn && shownWhen.property in toggles && shownWhen.manager in setOf(null, "Game")
                },
                "No bold serif Game over text in the middle shows while a toggle of Game is on: $texts",
            )
        },
        Request("level-1-starts-with-5-lives", "Have the level-1 Scene give Game 5 lives when it starts; other levels keep Game's usual 3.") { context ->
            val lives = context.managerType("Game").properties.filterIsInstance<PropertyDefinition.Number>().single { it.name == "lives" }
            assertEquals(3f, lives.initialValue, "Game's lives start at ${lives.initialValue} everywhere.")
            val values = ManagerSceneValues.valuesByManagerType(SceneDocument.parse(context.scenes.read("level-1")!!), setOf("Game"))["Game"]
            assertEquals(5f, values?.get("lives")?.floatOrNull, "level-1 gives Game $values.")
        },
        Request("paddle-hit-color", "Give the Paddle a color Property, hitColor, and turn the Paddle that color whenever the Ball hits it.") { context ->
            val paddle = context.actorType("Paddle")
            val colors = paddle.properties.filterIsInstance<PropertyDefinition.Color>().map { it.name }
            val setColors = paddle.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.SetColor>()
            assertTrue(
                setColors.any { block -> block.colors.any { it is ColorValue.Property && it.manager == null && it.property in colors } },
                "The Paddle's color Properties are $colors, and it sets its color with $setColors.",
            )
        },
        Request("spawner", "Add a Spawner Actor type that adds an Actor of a type it keeps in a Property, Brick to begin with, every 2 seconds.") { context ->
            val spawner = context.actorType("Spawner")
            val types = spawner.properties.filterIsInstance<PropertyDefinition.ActorType>().filter { it.initialValue == "Brick" }.map { it.name }
            val adds = spawner.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.AddActor>()
            assertTrue(
                adds.any { add -> add.actorType.let { it is ActorTypeValue.Property && it.manager == null && it.property in types } },
                "The Spawner's Actor type Properties starting at Brick are $types, and it adds Actors with $adds.",
            )
        },
        Request("paddle-moves-with-keys", "Simplify how the Paddle moves: one Move with keys Block on the arrow keys instead of its key Events.") { context ->
            val paddle = context.actorType("Paddle")
            val everyFrame = paddle.events.filter { it.type == EventType.EVERY_FRAME }.flatMap { it.blocks }
            assertTrue(everyFrame.any { it is BlockDefinition.MoveWithKeys && it.keys.usesArrows }, "The Paddle's Every frame Events are $everyFrame.")
            assertTrue(BlockDefinition.Stop !in everyFrame, "The Paddle still stops every frame: $everyFrame")
            assertEquals(emptyList(), paddle.events.filter { it.type == EventType.KEY_PRESSED }, "The Paddle keeps key Events.")
        },
        Request("win-when-bricks-are-gone", "Go to a new win Scene once the last Brick is broken.") { context ->
            val removals = context.blockTypes!!.types.value.filterIsInstance<ManagerTypeDefinition>().flatMap { it.events }
                .filter { it.type == EventType.ACTOR_REMOVED && it.otherActorType == "Brick" }
            assertTrue(
                removals.any { event ->
                    event.blocks.any { block -> block is BlockDefinition.If && block.condition.holdsOnlyWithoutBricks() } &&
                        event.blocks.any { it is BlockDefinition.GoToScene && it.scene != "level-1" && context.scenes.exists(it.scene) }
                },
                "No Manager type goes to a Scene of its own once no Brick is left: $removals",
            )
        },
        Request(
            id = "shooter",
            text = "Make a side-scrolling shooter: the player's ship flies up and down on the left and shoots at aliens coming from the right, which explode when hit, with stars scrolling past behind.",
            isBlank = true,
        ) { context ->
            val types = context.blockTypes!!.types.value
            val blocks = context.blocks()
            assertTrue(
                types.any { type -> type.events.any { it.type == EventType.KEY_PRESSED } } || blocks.any { it is BlockDefinition.MoveWithKeys },
                "Nothing reacts to the player's keys.",
            )
            assertTrue(blocks.any { it is BlockDefinition.AddActor }, "Nothing adds Actors: no shots, aliens, or stars come.")
            assertTrue(
                types.any { type -> type.events.any { event -> event.type == EventType.COLLISION && event.blocks.any { it == BlockDefinition.RemoveOther || it == BlockDefinition.RemoveSelf } } },
                "Nothing is removed when it is hit.",
            )
            assertTrue(context.scenes.names().any { context.scenes.actors(it)?.isNotEmpty() == true }, "No Scene places an Actor.")
        },
    )

    /**
     * Whether this counts the Bricks in a way that holds once none is left, but not while one is.
     */
    private fun ConditionDefinition.holdsOnlyWithoutBricks(): Boolean {
        if (this !is ConditionDefinition.ActorCount || actorType != "Brick") return false
        val value = (value as? NumberValue.Fixed)?.value ?: return false
        fun holdsFor(count: Float) = when (comparison) {
            Comparison.LESS -> count < value
            Comparison.LESS_OR_EQUAL -> count <= value
            Comparison.EQUAL -> count == value
            Comparison.NOT_EQUAL -> count != value
            Comparison.GREATER_OR_EQUAL -> count >= value
            Comparison.GREATER -> count > value
        }
        return holdsFor(0f) && !holdsFor(1f)
    }

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
        val x = assertIs<NumberValue.Fixed>(start.x, "The Ball starts at $start.").value
        val y = assertIs<NumberValue.Fixed>(start.y, "The Ball starts at $start.").value
        assertTrue(hypot(x, y) > hypot(200f, -300f), "The Ball starts at $start.")
    }

    private fun CommandContext.blocks() = blockTypes!!.types.value.flatMap { type -> type.events.flatMap { it.blocks } }

    private fun CommandContext.actorType(name: String) = blockTypes!!.get(name) as ActorTypeDefinition

    private fun CommandContext.managerType(name: String) = blockTypes!!.get(name) as ManagerTypeDefinition
}
