/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplaceBlocksTest {
    private val stop = BlockDefinition.Stop
    private val jump = BlockDefinition.JumpTo(NumberValue.Fixed(1f), NumberValue.Fixed(2f))
    private val log = BlockDefinition.Log("hit")
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(stop, jump, log)),
            EventDefinition(EventType.EVERY_FRAME),
        ),
    )

    private fun CommandContext.blocks(eventIndex: Int = 0) = blockTypes!!["Ball"]!!.events[eventIndex].blocks

    @Test
    fun severalBlocksAreAddedRemovedOrRewrittenInOneUndoStep() = runTest {
        val context = commandContext(ball)
        val velocity = BlockDefinition.SetVelocity(NumberValue.Fixed(3f), NumberValue.Fixed(4f))
        assertEquals(listOf(stop, velocity, log, jump, log), context.assertRoundTrip(Invocation(ReplaceBlocks, ReplaceBlocks.Input("Ball", 0, 1, blocks = listOf(velocity, log)))) { context.blocks() })
        assertEquals(listOf(stop, jump, log), context.assertRoundTrip(Invocation(ReplaceBlocks, ReplaceBlocks.Input("Ball", 0, 1, count = 2))) { context.blocks() })
        assertEquals(listOf(velocity, log), context.assertRoundTrip(Invocation(ReplaceBlocks, ReplaceBlocks.Input("Ball", 0, 0, count = 3, blocks = listOf(velocity, log)))) { context.blocks() })
        assertEquals(listOf(log), context.assertRoundTrip(Invocation(ReplaceBlocks, ReplaceBlocks.Input("Ball", 1, 0, blocks = listOf(log)))) { context.blocks(1) })
    }

    @Test
    fun aRangeOutsideTheEventOrNothingToDoIsRejected() = runTest {
        val context = commandContext(ball)
        assertEquals(Rejection(CommandProblem.BLOCKS_NOTHING_TO_REPLACE), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 0)))
        assertEquals(Rejection(CommandProblem.BLOCKS_NOTHING_TO_REPLACE), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 0, count = -1, blocks = listOf(log))))
        assertEquals(Rejection(LookupProblem.BLOCK_INSERTION_INDEX_INVALID, "Ball", "0", "3"), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 4, blocks = listOf(log))))
        assertEquals(Rejection(LookupProblem.BLOCKS_COUNT_TOO_LARGE, "Ball", "0", "2", "2", "1"), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 2, count = 2)))
        assertEquals(Rejection(LookupProblem.BLOCKS_COUNT_TOO_LARGE, "Ball", "0", "2", "1", "2"), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 1, count = Int.MAX_VALUE)))
        assertEquals(Rejection(LookupProblem.BLOCKS_NONE_TO_REMOVE, "Ball", "0", "3"), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 3, count = 1)))
        assertEquals(Rejection(LookupProblem.EVENT_MISSING, "Ball", "2", "1"), ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 2, 0, blocks = listOf(log))))
        assertNull(ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 3, blocks = listOf(log))))
    }

    @Test
    fun everyNewBlockMustBeAllowedInTheEvent() = runTest {
        val context = commandContext(ball)
        assertEquals(
            Rejection(PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION),
            ReplaceBlocks.validate(context, ReplaceBlocks.Input("Ball", 0, 0, count = 1, blocks = listOf(log, BlockDefinition.RemoveOther))),
        )
        assertNull(ReplaceBlocks.validateRestoring(context, ReplaceBlocks.Input("Ball", 0, 0, count = 1, blocks = listOf(BlockDefinition.RemoveOther))))
    }

    @Test
    fun theTargetIsWhereTheBlocksGo() = runTest {
        assertEquals(CommandTarget.BlockType("Ball", 0, 2), ReplaceBlocks.target(commandContext(ball), ReplaceBlocks.Input("Ball", 0, 2, count = 1)))
    }
}
