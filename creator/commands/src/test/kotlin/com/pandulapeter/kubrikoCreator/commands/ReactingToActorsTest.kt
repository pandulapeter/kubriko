/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * A Manager type reacting to Actors of a type being added and removed, and counting them: where the Events and the
 * condition may go, and how renaming and deleting the Actor type they name follow them.
 */
class ReactingToActorsTest {
    private val brick = ActorTypeDefinition(name = "Brick", events = listOf(EventDefinition(EventType.EVERY_FRAME)))
    private val ball = ActorTypeDefinition(name = "Ball")
    private val allGone = listOf(
        BlockDefinition.If(ConditionDefinition.ActorCount("Brick")),
        BlockDefinition.GoToScene("win"),
        BlockDefinition.End,
        BlockDefinition.Log("Still playing"),
    )
    private val game = ManagerTypeDefinition(
        name = "Game",
        events = listOf(
            EventDefinition(EventType.ACTOR_REMOVED, otherActorType = "Brick", blocks = allGone),
            EventDefinition(EventType.ACTOR_ADDED, otherActorType = "Ball"),
        ),
    )

    @Test
    fun onlyAManagerTypeReactsToActorsOfAKnownType() = runTest {
        val context = commandContext(brick, ball, game)
        assertNull(AddEvent.validate(context, AddEvent.Input("Game", EventType.ACTOR_ADDED, otherActorType = "Brick")))
        assertNull(AddEvent.validate(context, AddEvent.Input("Game", EventType.ACTOR_REMOVED)))
        assertEquals(Rejection(PlacementProblem.EVENT_ACTOR_ADDED_IN_ACTOR_TYPE), AddEvent.validate(context, AddEvent.Input("Brick", EventType.ACTOR_ADDED, otherActorType = "Ball")))
        assertEquals(Rejection(PlacementProblem.EVENT_ACTOR_REMOVED_IN_ACTOR_TYPE), AddEvent.validate(context, AddEvent.Input("Brick", EventType.ACTOR_REMOVED)))
        assertNotNull(AddEvent.validate(context, AddEvent.Input("Game", EventType.ACTOR_ADDED, otherActorType = "Paddle")))
        assertEquals(Rejection(PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED), AddEvent.validate(context, AddEvent.Input("Game", EventType.EVERY_FRAME, otherActorType = "Brick")))
    }

    @Test
    fun theActorTypeOfTheEventIsChangedAndUndone() = runTest {
        val context = commandContext(brick, ball, game)
        val changed = context.assertRoundTrip(Invocation(SetEventOtherActorType, SetEventOtherActorType.Input("Game", 1, "Brick"))) { context.blockTypes!!.managerType("Game") }
        assertEquals("Brick", changed?.events?.get(1)?.otherActorType)
    }

    @Test
    fun bothKindsOfTypesCountActorsOfAKnownType() = runTest {
        val context = commandContext(brick, ball, game)
        val counting = BlockDefinition.If(ConditionDefinition.ActorCount("Ball", Comparison.GREATER, NumberValue.Fixed(2f)))
        assertNull(AddBlock.validate(context, AddBlock.Input("Game", 0, counting)))
        assertNull(AddBlock.validate(context, AddBlock.Input("Brick", 0, counting)))
        assertNull(AddBlock.validate(context, AddBlock.Input("Brick", 0, BlockDefinition.If(ConditionDefinition.ActorCount()))))
        assertNotNull(AddBlock.validate(context, AddBlock.Input("Game", 0, BlockDefinition.If(ConditionDefinition.ActorCount("Paddle")))))
    }

    @Test
    fun renamingTheActorTypeFollowsTheEventAndTheCount() = runTest {
        val context = commandContext(brick, ball, game)
        val renamed = context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Brick", "Tile"))) { context.blockTypes!!.managerType("Game") }
        assertEquals("Tile", renamed?.events?.first()?.otherActorType)
        assertEquals(BlockDefinition.If(ConditionDefinition.ActorCount("Tile")), renamed?.events?.first()?.blocks?.first())
    }

    @Test
    fun deletingTheActorTypeRemovesTheEventAndTheCountingSectionAndIsUndone() = runTest {
        val counter = ManagerTypeDefinition(
            name = "Counter",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = allGone + BlockDefinition.If(ConditionDefinition.ActorCount("Ball")) + BlockDefinition.End)),
        )
        val context = commandContext(brick, ball, game, counter)
        val after = context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Brick"))) { context.blockTypes!!.types.value }
        val gameAfter = after.first { it.name == "Game" }
        assertEquals(listOf(EventType.ACTOR_ADDED), gameAfter.events.map { it.type })
        val counterAfter = after.first { it.name == "Counter" }
        assertEquals(listOf(BlockDefinition.Log("Still playing"), BlockDefinition.If(ConditionDefinition.ActorCount("Ball")), BlockDefinition.End), counterAfter.events.single().blocks)
    }

    @Test
    fun theAiReadsThemInWords() {
        assertEquals("When an Actor of Brick is removed by a Block", BlockNotation.of(game.events.first()))
        assertEquals("When an Actor of (none) is added", BlockNotation.of(EventDefinition(EventType.ACTOR_ADDED)))
        assertEquals("the number of Actors of Brick is 0", BlockNotation.of(ConditionDefinition.ActorCount("Brick")))
    }
}
