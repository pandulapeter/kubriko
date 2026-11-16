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
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Set the size, Set the scale, and Change the scale Blocks: what validating them accepts and refuses, and how the AI
 * reads them.
 */
class SizeBlocksTest {
    private val hero = ActorTypeDefinition(
        name = "Hero",
        properties = listOf(PropertyDefinition.Number("growth")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME)),
    )
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.GAME_STARTED)))

    private fun CommandContext.rejection(block: BlockDefinition, type: String = "Hero") = AddBlock.validate(this, AddBlock.Input(type, 0, block))

    @Test
    fun anActorIsResizedAndScaledWithNumbersThatAreRandomOrAPropertysValue() = runTest {
        val context = commandContext(hero, game)
        listOf(
            BlockDefinition.SetSize(),
            BlockDefinition.SetSize(NumberValue.Fixed(0f), NumberValue.Random(10f, 30f)),
            BlockDefinition.SetSize(NumberValue.Property("growth"), NumberValue.Property("growth")),
            BlockDefinition.SetScale(NumberValue.Fixed(0f)),
            BlockDefinition.SetScale(NumberValue.Property("growth")),
            BlockDefinition.ChangeScale(NumberValue.Fixed(-0.5f)),
            BlockDefinition.ChangeScale(NumberValue.Random(-0.1f, 0.1f)),
        ).forEach { assertNull(context.rejection(it), it.toString()) }
    }

    @Test
    fun aManagerTypeHasNoSizeOrScale() = runTest {
        val context = commandContext(hero, game)
        listOf(BlockDefinition.SetSize(), BlockDefinition.SetScale(), BlockDefinition.ChangeScale()).forEach { block ->
            assertEquals(Rejection(PlacementProblem.BLOCK_SIZE_IN_MANAGER_TYPE), context.rejection(block, type = "Game"), block.toString())
        }
    }

    @Test
    fun aSizeAndAScaleAreNeverNegative() = runTest {
        val context = commandContext(hero)
        assertEquals(Rejection(CommandProblem.SIZE_NEGATIVE), context.rejection(BlockDefinition.SetSize(width = NumberValue.Fixed(-1f))))
        assertEquals(Rejection(CommandProblem.SIZE_NEGATIVE), context.rejection(BlockDefinition.SetSize(height = NumberValue.Random(-5f, 5f))))
        assertEquals(Rejection(CommandProblem.SCALE_NEGATIVE), context.rejection(BlockDefinition.SetScale(NumberValue.Fixed(-0.5f))))
        assertEquals(Rejection(CommandProblem.SCALE_NEGATIVE), context.rejection(BlockDefinition.SetScale(NumberValue.Random(-1f, 1f))))
    }

    @Test
    fun noneOfTheirNumbersIsAnEdge() = runTest {
        val context = commandContext(hero)
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(BlockDefinition.SetSize(width = NumberValue.Edge(SceneEdge.LEFT))))
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(BlockDefinition.SetScale(NumberValue.Edge(SceneEdge.TOP))))
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(BlockDefinition.ChangeScale(NumberValue.BetweenEdges())))
        assertContains(CommandProblem.NUMBER_EDGE_NOT_TAKEN.english, "a volume, a size, a scale")
    }

    @Test
    fun theAiReadsWhatEachSizeBlockDoes() {
        assertEquals("Set the size to 80 by 80", BlockNotation.of(BlockDefinition.SetSize()))
        assertEquals("Set the scale to 2", BlockNotation.of(BlockDefinition.SetScale()))
        assertEquals("Change the scale by -0.25", BlockNotation.of(BlockDefinition.ChangeScale(NumberValue.Fixed(-0.25f))))
    }
}
