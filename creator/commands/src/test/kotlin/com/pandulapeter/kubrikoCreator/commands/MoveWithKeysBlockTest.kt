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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import com.pandulapeter.kubrikoCreator.actors.SteeringKeys
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Move with keys Block: what validating it accepts and refuses.
 */
class MoveWithKeysBlockTest {
    private val hero = ActorTypeDefinition(
        name = "Hero",
        properties = listOf(PropertyDefinition.Number("speed")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME), EventDefinition(EventType.KEY_PRESSED)),
    )
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.EVERY_FRAME)))

    private fun CommandContext.rejection(block: BlockDefinition, type: String = "Hero", event: Int = 0) = AddBlock.validate(this, AddBlock.Input(type, event, block))

    @Test
    fun anActorEveryFrameMovesWithAnyKeysAtAnySpeedButAnEdge() = runTest {
        val context = commandContext(hero)
        listOf(
            BlockDefinition.MoveWithKeys(),
            BlockDefinition.MoveWithKeys(SteeringKeys.WASD, movesDiagonally = false, speed = NumberValue.Property("speed")),
            BlockDefinition.MoveWithKeys(SteeringKeys.ARROWS_AND_WASD, speed = NumberValue.Random(100f, 200f)),
        ).forEach { assertNull(context.rejection(it), it.toString()) }
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(BlockDefinition.MoveWithKeys(speed = NumberValue.Edge(SceneEdge.RIGHT))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(BlockDefinition.MoveWithKeys(speed = NumberValue.Fixed(Float.NaN))))
    }

    @Test
    fun itWorksOnlyInAnActorsEveryFrameEvent() = runTest {
        val context = commandContext(hero, game)
        assertEquals(Rejection(PlacementProblem.BLOCK_MOVE_WITH_KEYS_OUTSIDE_EVERY_FRAME), context.rejection(BlockDefinition.MoveWithKeys(), event = 1))
        assertEquals(Rejection(PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE), context.rejection(BlockDefinition.MoveWithKeys(), type = "Game"))
    }

    @Test
    fun addingItIsUndone() = runTest {
        val context = commandContext(hero)
        val steering = BlockDefinition.MoveWithKeys(SteeringKeys.WASD, movesDiagonally = false)
        val added = context.assertRoundTrip(Invocation(AddBlock, AddBlock.Input("Hero", 0, steering))) { context.blockTypes!!.actorType("Hero") }
        assertEquals(listOf<BlockDefinition>(steering), added?.events?.first()?.blocks)
    }
}
