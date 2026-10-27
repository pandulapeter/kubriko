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
    fun textsAreInsertedReplacedAndRemoved() {
        val game = ManagerTypeDefinition("Game")
        val withTexts = game.withTextInserted(0, ScreenTextDefinition("B")).withTextInserted(0, ScreenTextDefinition("A"))
        assertEquals(listOf("A", "B"), withTexts.texts.map { it.text })
        assertEquals(listOf("A", "C"), withTexts.withTextReplaced(1, ScreenTextDefinition("C")).texts.map { it.text })
        assertEquals(listOf("B"), withTexts.withTextRemoved(0).texts.map { it.text })
    }
}
