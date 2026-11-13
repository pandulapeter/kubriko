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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Blocks that set the color and the opacity: what validating them accepts and refuses.
 */
class AppearanceBlocksTest {
    private val spark = ActorTypeDefinition(
        name = "Spark",
        properties = listOf(PropertyDefinition.Number("fade")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME)),
    )
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.GAME_STARTED)))

    private fun CommandContext.rejection(block: BlockDefinition, type: String = "Spark") = AddBlock.validate(this, AddBlock.Input(type, 0, block))

    @Test
    fun aColorBlockTakesOneToEightOpaqueColors() = runTest {
        val context = commandContext(spark)
        assertNull(context.rejection(BlockDefinition.SetColor()))
        assertNull(context.rejection(BlockDefinition.SetColor(List(BlockDefinition.SetColor.MAXIMUM_COLORS) { 0xFF000000 + it })))
        assertEquals(Rejection(CommandProblem.COLORS_COUNT, "8"), context.rejection(BlockDefinition.SetColor(emptyList())))
        assertEquals(Rejection(CommandProblem.COLORS_COUNT, "8"), context.rejection(BlockDefinition.SetColor(List(9) { 0xFF000000 + it })))
        assertEquals(Rejection(CommandProblem.COLORS_NOT_OPAQUE), context.rejection(BlockDefinition.SetColor(listOf(0xFFFF0000, 0x80FF0000))))
    }

    @Test
    fun anOpacityIsBetweenZeroAndAHundredPercentAndNoEdge() = runTest {
        val context = commandContext(spark)
        listOf(
            BlockDefinition.SetOpacity(NumberValue.Fixed(0f)),
            BlockDefinition.SetOpacity(NumberValue.Fixed(100f)),
            BlockDefinition.SetOpacity(NumberValue.Random(20f, 80f)),
            BlockDefinition.SetOpacity(NumberValue.Property("fade")),
            BlockDefinition.ChangeOpacity(NumberValue.Fixed(-250f)),
            BlockDefinition.ChangeOpacity(NumberValue.Property("fade")),
        ).forEach { assertNull(context.rejection(it), it.toString()) }
        listOf(
            BlockDefinition.SetOpacity(NumberValue.Fixed(-1f)),
            BlockDefinition.SetOpacity(NumberValue.Fixed(101f)),
            BlockDefinition.SetOpacity(NumberValue.Random(50f, 150f)),
        ).forEach { assertEquals(Rejection(CommandProblem.OPACITY_OUT_OF_RANGE), context.rejection(it), it.toString()) }
        listOf(
            BlockDefinition.SetOpacity(NumberValue.Fixed(Float.NaN)),
            BlockDefinition.ChangeOpacity(NumberValue.Fixed(Float.NaN)),
        ).forEach { assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(it), it.toString()) }
        listOf(
            BlockDefinition.SetOpacity(NumberValue.Edge(SceneEdge.TOP)),
            BlockDefinition.ChangeOpacity(NumberValue.BetweenEdges(SceneEdge.LEFT, to = SceneEdge.RIGHT)),
        ).forEach { assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(it), it.toString()) }
    }

    @Test
    fun aManagerTypeIsntDrawnSoItHasNoAppearanceBlocks() = runTest {
        val context = commandContext(spark, game)
        listOf(BlockDefinition.SetColor(), BlockDefinition.SetOpacity(), BlockDefinition.ChangeOpacity()).forEach { block ->
            assertEquals(Rejection(PlacementProblem.BLOCK_APPEARANCE_IN_MANAGER_TYPE), context.rejection(block, type = "Game"), block.toString())
        }
    }

    @Test
    fun addingAnAppearanceBlockIsUndone() = runTest {
        val context = commandContext(spark)
        val color = BlockDefinition.SetColor(listOf(0xFF20C060, 0xFFFFFFFF))
        val added = context.assertRoundTrip(Invocation(AddBlock, AddBlock.Input("Spark", 0, color))) { context.blockTypes!!.actorType("Spark") }
        assertEquals(listOf<BlockDefinition>(color), added?.events?.single()?.blocks)
    }
}
