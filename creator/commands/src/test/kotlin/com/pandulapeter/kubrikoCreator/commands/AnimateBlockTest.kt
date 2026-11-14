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
import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.AnimationEasing
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
 * The Animate Block: what validating it accepts and refuses.
 */
class AnimateBlockTest {
    private val flash = ActorTypeDefinition(
        name = "Flash",
        properties = listOf(PropertyDefinition.Number("grow")),
        events = listOf(EventDefinition(EventType.ADDED_TO_SCENE)),
    )
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.GAME_STARTED)))

    private fun CommandContext.rejection(block: BlockDefinition, type: String = "Flash") = AddBlock.validate(this, AddBlock.Input(type, 0, block))

    @Test
    fun everyValueCanBeAnimatedWithNumbersThatAreRandomOrAPropertysValue() = runTest {
        val context = commandContext(flash)
        listOf(
            BlockDefinition.Animate(),
            BlockDefinition.Animate(AnimatedValue.Scale(NumberValue.Fixed(0f), NumberValue.Property("grow")), NumberValue.Random(0f, 2f), AnimationEasing.LINEAR, removesActor = true),
            BlockDefinition.Animate(AnimatedValue.Opacity(NumberValue.Random(0f, 100f), NumberValue.Property("grow")), NumberValue.Fixed(0f)),
            BlockDefinition.Animate(AnimatedValue.Color(0xFF000000, 0xFFFFFFFF), easing = AnimationEasing.EASE_IN_OUT),
            BlockDefinition.Animate(AnimatedValue.Position(NumberValue.Edge(SceneEdge.RIGHT, -20f), NumberValue.BetweenEdges(SceneEdge.TOP, to = SceneEdge.BOTTOM), isRelative = false)),
        ).forEach { assertNull(context.rejection(it), it.toString()) }
    }

    @Test
    fun anOpacityIsAPercentAndAColorOpaque() = runTest {
        val context = commandContext(flash)
        listOf(
            BlockDefinition.Animate(AnimatedValue.Opacity(NumberValue.Fixed(101f))),
            BlockDefinition.Animate(AnimatedValue.Opacity(to = NumberValue.Random(-10f, 50f))),
        ).forEach { assertEquals(Rejection(CommandProblem.OPACITY_OUT_OF_RANGE), context.rejection(it), it.toString()) }
        listOf(
            BlockDefinition.Animate(AnimatedValue.Color(from = 0x80FFFFFF)),
            BlockDefinition.Animate(AnimatedValue.Color(to = 0x00000000)),
        ).forEach { assertEquals(Rejection(CommandProblem.ANIMATED_COLOR_NOT_OPAQUE), context.rejection(it), it.toString()) }
    }

    @Test
    fun aScaleIsNeverNegative() = runTest {
        val context = commandContext(flash)
        listOf(
            BlockDefinition.Animate(AnimatedValue.Scale(from = NumberValue.Fixed(-1f))),
            BlockDefinition.Animate(AnimatedValue.Scale(to = NumberValue.Random(-0.5f, 2f))),
        ).forEach { assertEquals(Rejection(CommandProblem.SCALE_NEGATIVE), context.rejection(it), it.toString()) }
    }

    @Test
    fun theTimeIsNeverNegativeAndOnlyAPositionTakesEdges() = runTest {
        val context = commandContext(flash)
        listOf(
            BlockDefinition.Animate(seconds = NumberValue.Fixed(-1f)),
            BlockDefinition.Animate(seconds = NumberValue.Random(-1f, 1f)),
            BlockDefinition.Animate(AnimatedValue.Scale(to = NumberValue.Fixed(Float.POSITIVE_INFINITY))),
        ).forEach { assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(it), it.toString()) }
        listOf(
            BlockDefinition.Animate(seconds = NumberValue.Edge(SceneEdge.LEFT)),
            BlockDefinition.Animate(AnimatedValue.Scale(from = NumberValue.Edge(SceneEdge.TOP))),
            BlockDefinition.Animate(AnimatedValue.Opacity(to = NumberValue.BetweenEdges(SceneEdge.LEFT, to = SceneEdge.RIGHT))),
            BlockDefinition.Animate(AnimatedValue.Position(), seconds = NumberValue.Edge(SceneEdge.BOTTOM)),
        ).forEach { assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(it), it.toString()) }
        assertEquals(
            Rejection(CommandProblem.NUMBER_EDGE_ACROSS),
            context.rejection(BlockDefinition.Animate(AnimatedValue.Position(x = NumberValue.BetweenEdges(SceneEdge.TOP, to = SceneEdge.RIGHT)))),
        )
    }

    @Test
    fun aManagerTypeIsntDrawnSoItCantAnimate() = runTest {
        assertEquals(Rejection(PlacementProblem.BLOCK_ANIMATE_IN_MANAGER_TYPE), commandContext(flash, game).rejection(BlockDefinition.Animate(), type = "Game"))
    }

    @Test
    fun addingAnAnimateBlockIsUndone() = runTest {
        val context = commandContext(flash)
        val fade = BlockDefinition.Animate(AnimatedValue.Opacity(), NumberValue.Fixed(2f), AnimationEasing.EASE_IN, removesActor = true)
        val added = context.assertRoundTrip(Invocation(AddBlock, AddBlock.Input("Flash", 0, fade))) { context.blockTypes!!.actorType("Flash") }
        assertEquals(listOf<BlockDefinition>(fade), added?.events?.single()?.blocks)
    }
}
