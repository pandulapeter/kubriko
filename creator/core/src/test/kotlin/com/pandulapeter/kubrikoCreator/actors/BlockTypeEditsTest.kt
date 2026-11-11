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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BlockTypeEditsTest {
    private val stop = BlockDefinition.Stop
    private val jump = BlockDefinition.JumpTo(1f, 2f)
    private val remove = BlockDefinition.RemoveSelf
    private val definition = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(stop, jump, remove)),
            EventDefinition(EventType.KEY_PRESSED, KeyChoice.LEFT),
        ),
    )

    @Test
    fun eventsAreInsertedAtTheirIndexClampedToTheList() {
        val frame = EventDefinition(EventType.EVERY_FRAME)
        assertEquals(listOf(EventType.EVERY_FRAME, EventType.ADDED_TO_SCENE, EventType.KEY_PRESSED), definition.withEventInserted(0, frame).events.map { it.type })
        assertEquals(frame, definition.withEventInserted(99, frame).events.last())
    }

    @Test
    fun eventsCanBeRemovedMovedAndRekeyed() {
        assertEquals(listOf(EventType.KEY_PRESSED), definition.withEventRemoved(0).events.map { it.type })
        assertEquals(listOf(EventType.KEY_PRESSED, EventType.ADDED_TO_SCENE), definition.withEventMoved(0, 1).events.map { it.type })
        assertEquals(KeyChoice.D, definition.withEventKey(1, KeyChoice.D).events[1].key)
        assertEquals("Paddle", definition.withEventOtherActorType(1, "Paddle").events[1].otherActorType)
        assertEquals(null, definition.withEventOtherActorType(1, "Paddle").withEventOtherActorType(1, null).events[1].otherActorType)
    }

    @Test
    fun blocksAreInsertedReplacedAndRemovedWithinTheirEvent() {
        assertEquals(listOf(stop), definition.withBlockInserted(1, 0, stop).events[1].blocks)
        assertEquals(listOf(stop, remove, jump, remove), definition.withBlockInserted(0, 1, remove).events[0].blocks)
        assertEquals(listOf(stop, BlockDefinition.JumpTo(5f, 5f), remove), definition.withBlockReplaced(0, 1, BlockDefinition.JumpTo(5f, 5f)).events[0].blocks)
        assertEquals(listOf(stop, remove), definition.withBlockRemoved(0, 1).events[0].blocks)
        assertEquals(definition.events[1], definition.withBlockRemoved(0, 1).events[1])
    }

    @Test
    fun blocksMoveWithinTheirEventAndStopAtEitherEnd() {
        assertEquals(listOf(jump, stop, remove), definition.withBlockMoved(0, 1, 0).events[0].blocks)
        assertEquals(listOf(stop, remove, jump), definition.withBlockMoved(0, 1, 2).events[0].blocks)
        assertEquals(listOf(jump, stop, remove), definition.withBlockMoved(0, 1, -5).events[0].blocks)
        assertEquals(listOf(stop, jump, remove), definition.withBlockMoved(0, 2, 3).events[0].blocks)
    }

    @Test
    fun collisionEventsFollowARenamedActorType() {
        val collisions = ActorTypeDefinition(
            name = "Ball",
            events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Brick"), EventDefinition(EventType.COLLISION, otherActorType = "Wall")),
        )
        assertEquals(listOf("Block", "Wall"), collisions.withOtherActorTypeRenamed("Brick", "Block").events.map { it.otherActorType })
    }

    @Test
    fun outOfRangeIndicesChangeNothing() {
        assertSame(definition, definition.withEventRemoved(5))
        assertSame(definition, definition.withEventMoved(5, 0))
        assertSame(definition, definition.withEventKey(-1, KeyChoice.A))
        assertSame(definition, definition.withBlockInserted(2, 0, stop))
        assertEquals(definition, definition.withBlockReplaced(0, 9, stop))
        assertEquals(definition, definition.withBlockMoved(0, 9, 1))
        assertEquals(definition, definition.withBlockRemoved(0, 9))
    }

    @Test
    fun renamingAManagerTypeFollowsItsBlocksPlaceholdersAndTexts() {
        val brick = ActorTypeDefinition(
            name = "Brick",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", manager = "Game"), BlockDefinition.Log("{Game.score} {score}"), BlockDefinition.ChangeNumber("score")))),
        )
        val renamed = brick.withManagerRenamed("Game", "Match")
        assertEquals(
            listOf(BlockDefinition.ChangeNumber("score", manager = "Match"), BlockDefinition.Log("{Match.score} {score}"), BlockDefinition.ChangeNumber("score")),
            renamed.events.single().blocks,
        )
        val hud = ManagerTypeDefinition("Hud", texts = listOf(ScreenTextDefinition("{Game.score}")))
        assertEquals("{Match.score}", (hud.withManagerRenamed("Game", "Match") as ManagerTypeDefinition).texts.single().text)
    }

    @Test
    fun renamingAManagersPropertyFollowsOnlyItsOwnReferences() {
        val brick = ActorTypeDefinition(
            name = "Brick",
            properties = listOf(PropertyDefinition.Number("score")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", manager = "Game"), BlockDefinition.ChangeNumber("score"), BlockDefinition.Log("{Game.score} {score}")))),
        )
        assertEquals(
            listOf(BlockDefinition.ChangeNumber("points", manager = "Game"), BlockDefinition.ChangeNumber("score"), BlockDefinition.Log("{Game.points} {score}")),
            brick.withManagerPropertyRenamed("Game", "score", "points").events.single().blocks,
        )
    }

    @Test
    fun renamingAManagersOwnPropertyFollowsItsTexts() {
        val game = ManagerTypeDefinition(
            name = "Game",
            properties = listOf(PropertyDefinition.Number("score")),
            texts = listOf(ScreenTextDefinition("Score: {score}, {Other.score}")),
        )
        val renamed = game.withPropertyReplaced("score", PropertyDefinition.Number("points")) as ManagerTypeDefinition
        assertEquals("Score: {points}, {Other.score}", renamed.texts.single().text)
    }

    @Test
    fun kotlinCodeFollowsRenamedTypes() {
        val hud = ManagerTypeDefinition(
            name = "Hud",
            properties = listOf(PropertyDefinition.Number("match")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("if (actor is Ball) gameManager.score = 0f")))),
            kotlinUi = "BasicText(\"Game: \${gameManager.score}\")\nval x: Game = gameManager",
        )
        val renamed = hud.withManagerRenamed("Game", "Match").withOtherActorTypeRenamed("Ball", "Puck") as ManagerTypeDefinition
        assertEquals(BlockDefinition.Kotlin("if (actor is Puck) matchManager.score = 0f"), renamed.events.single().blocks.single())
        assertEquals("BasicText(\"Game: \${matchManager.score}\")\nval x: Match = matchManager", renamed.kotlinUi)
        val game = ManagerTypeDefinition("Game", kotlinUi = "val game: Game = this")
        assertEquals("val game: Match = this", (game.withManagerRenamed("Game", "Match") as ManagerTypeDefinition).kotlinUi)
    }

    @Test
    fun kotlinCodeFollowsRenamedProperties() {
        val game = ManagerTypeDefinition(
            name = "Game",
            properties = listOf(PropertyDefinition.Number("score")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("score += hudManager.score")))),
            kotlinUi = "BasicText(\"\$score\")",
        )
        val renamed = game.withPropertyReplaced("score", PropertyDefinition.Number("points")).withManagerPropertyRenamed("Game", "score", "points") as ManagerTypeDefinition
        assertEquals(BlockDefinition.Kotlin("points += hudManager.score"), renamed.events.single().blocks.single())
        assertEquals("BasicText(\"\$points\")", renamed.kotlinUi)
        val hud = ManagerTypeDefinition("Hud", properties = listOf(PropertyDefinition.Number("score")), kotlinUi = "BasicText(\"\${gameManager.score} \$score\")")
        assertEquals("BasicText(\"\${gameManager.points} \$score\")", (hud.withManagerPropertyRenamed("Game", "score", "points") as ManagerTypeDefinition).kotlinUi)
    }

    @Test
    fun textsAreInsertedReplacedAndRemoved() {
        val game = ManagerTypeDefinition("Game")
        val withTexts = game.withTextInserted(0, ScreenTextDefinition("B")).withTextInserted(0, ScreenTextDefinition("A"))
        assertEquals(listOf("A", "B"), withTexts.texts.map { it.text })
        assertEquals(listOf("A", "C"), withTexts.withTextReplaced(1, ScreenTextDefinition("C")).texts.map { it.text })
        assertEquals(listOf("B"), withTexts.withTextRemoved(0).texts.map { it.text })
    }

    @Test
    fun kotlinCodeNamingTheOldManagerFieldsNamesTheNewOnes() {
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score")), kotlinUi = "BasicText(\"\${level.number} \$score\")\nval game = this")
        val level = ManagerTypeDefinition("Level", properties = listOf(PropertyDefinition.Number("number")))
        val brick = ActorTypeDefinition(
            name = "Brick",
            properties = listOf(PropertyDefinition.Number("level")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("game.score += level\nlevelManager.number = 1f // game")))),
        )
        val types = listOf(brick, game, level)
        assertEquals("BasicText(\"\${levelManager.number} \$score\")\nval game = this", (game.withManagerFieldsSuffixed(types) as ManagerTypeDefinition).kotlinUi)
        assertEquals(
            BlockDefinition.Kotlin("gameManager.score += level\nlevelManager.number = 1f // game"),
            brick.withManagerFieldsSuffixed(types).events.single().blocks.single(),
        )
        assertEquals(level, level.withManagerFieldsSuffixed(types))
    }

    @Test
    fun renamingAPropertyLeavesKotlinImportsAlone() {
        val code = "import kotlin.math.sin\nkotlin = sin(kotlin)"
        val ball = ActorTypeDefinition(
            name = "Ball",
            properties = listOf(PropertyDefinition.Number("kotlin")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin(code)))),
        )
        assertEquals(
            BlockDefinition.Kotlin("import kotlin.math.sin\nangle = sin(angle)"),
            ball.withPropertyReplaced("kotlin", PropertyDefinition.Number("angle")).events.single().blocks.single(),
        )
        val hud = ManagerTypeDefinition(name = "Hud", properties = listOf(PropertyDefinition.Number("kotlin")), kotlinUi = code)
        assertEquals("import kotlin.math.sin\nangle = sin(angle)", (hud.withPropertyReplaced("kotlin", PropertyDefinition.Number("angle")) as ManagerTypeDefinition).kotlinUi)
    }

    @Test
    fun renamingAResourceFollowsTheSpriteAndTheBlocksPlayingIt() {
        val ball = ActorTypeDefinition(
            name = "Ball",
            sprite = "drawable/ball.png",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.PlaySound("files/hit.wav"), BlockDefinition.PlayMusic("files/hit.wav"), BlockDefinition.PlaySound("files/other.wav")))),
        )
        assertEquals("drawable/orb.png", (ball.withResourceRenamed("drawable/ball.png", "drawable/orb.png") as ActorTypeDefinition).sprite)
        assertEquals(
            listOf(BlockDefinition.PlaySound("files/bounce.wav"), BlockDefinition.PlayMusic("files/bounce.wav"), BlockDefinition.PlaySound("files/other.wav")),
            ball.withResourceRenamed("files/hit.wav", "files/bounce.wav").events.single().blocks,
        )
        assertEquals(ball, ball.withResourceRenamed("drawable/bat.png", "drawable/club.png"))
    }

    @Test
    fun renamingATimerFollowsTheKotlinUsingItsMembersAndOnlyItsTimerEvents() {
        val clock = ManagerTypeDefinition(
            name = "Clock",
            events = listOf(
                EventDefinition(EventType.TIMER_FINISHED, timer = "tick", blocks = listOf(BlockDefinition.Kotlin("tickRemainingInMilliseconds = 0L\nonTickFinished()"))),
                EventDefinition(EventType.EVERY_FRAME, timer = "tick"),
            ),
            kotlinUi = "Text(tickPeriodInMilliseconds.toString())",
        )
        val renamed = clock.withTimerRenamed("tick", "beat") as ManagerTypeDefinition
        assertEquals("beat", renamed.events[0].timer)
        assertEquals(BlockDefinition.Kotlin("beatRemainingInMilliseconds = 0L\nonBeatFinished()"), renamed.events[0].blocks.single())
        assertEquals("tick", renamed.events[1].timer, "A stray field on another kind of Event is no Timer.")
        assertEquals("Text(beatPeriodInMilliseconds.toString())", renamed.kotlinUi)
        assertEquals(listOf("tick"), clock.timerNames)
    }
}
