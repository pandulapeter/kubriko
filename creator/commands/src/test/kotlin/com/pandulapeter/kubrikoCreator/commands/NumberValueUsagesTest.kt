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
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Properties a Block reads as one of its numbers ([NumberValue.Property]): what validating, finding usages, and deleting
 * make of them.
 */
class NumberValueUsagesTest {
    private fun <I> CommandContext.rejection(command: WriteCommand<I>, input: I) = command.validate(this, input)

    private val speed = NumberValue.Property("speed")
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Number("speed", initialValue = 120f), PropertyDefinition.Number("hp"), PropertyDefinition.Text("title")),
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(
                    BlockDefinition.SetVelocity(x = speed, y = NumberValue.Random(-1f, 1f)),
                    BlockDefinition.If(ConditionDefinition.Number("hp", Comparison.GREATER, speed)),
                    BlockDefinition.Stop,
                    BlockDefinition.End,
                    BlockDefinition.If(ConditionDefinition.Number("speed")),
                    BlockDefinition.Stop,
                    BlockDefinition.End,
                ),
            ),
        ),
    )

    @Test
    fun aValueNamesAPropertyThatExistsAndIsANumber() = runTest {
        val context = commandContext(enemy)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.SetVelocity(x = NumberValue.Property()))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.SetVelocity(x = speed))))
        assertEquals(
            Rejection(PlacementProblem.NUMBER_PROPERTY_MISSING, "lives", "Enemy"),
            context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.SetVelocity(x = NumberValue.Property("lives")))),
        )
        assertEquals(
            Rejection(PlacementProblem.NUMBER_PROPERTY_OF_OTHER_KIND, "title", "Enemy"),
            context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.SetNumber("hp", NumberValue.Property("title")))),
        )
    }

    @Test
    fun aRandomNumberStaysWithinWhatItsFieldTakes() = runTest {
        val context = commandContext(enemy)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.If(ConditionDefinition.Chance(NumberValue.Random(0f, 100f))))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.If(ConditionDefinition.Chance(NumberValue.Random(50f, 150f))))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.StartTimer(seconds = NumberValue.Random(-1f, 1f)))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.SetVelocity(x = NumberValue.Random(0f, Float.NaN)))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.Repeat(NumberValue.Fixed(2.5f)))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.Repeat(NumberValue.Random(0.5f, 2.5f)))))
    }

    @Test
    fun anXTakesOnlyHorizontalEdgesAndAYOnlyVerticalOnes() = runTest {
        val context = commandContext(enemy)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.AddActor("Enemy", NumberValue.Edge(SceneEdge.RIGHT, 50f), NumberValue.BetweenEdges(SceneEdge.TOP, to = SceneEdge.BOTTOM)))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.SetNumber("hp", NumberValue.BetweenEdges(SceneEdge.BOTTOM, to = SceneEdge.TOP)))))
        listOf(
            BlockDefinition.JumpTo(x = NumberValue.Edge(SceneEdge.TOP)),
            BlockDefinition.AddActor("Enemy", y = NumberValue.BetweenEdges(SceneEdge.LEFT, to = SceneEdge.RIGHT)),
            BlockDefinition.SetVelocity(y = NumberValue.Edge(SceneEdge.LEFT)),
            BlockDefinition.SetNumber("hp", NumberValue.BetweenEdges(SceneEdge.LEFT, to = SceneEdge.BOTTOM)),
        ).forEach { block ->
            assertEquals(Rejection(CommandProblem.NUMBER_EDGE_ACROSS), context.rejection(AddBlock, AddBlock.Input("Enemy", 0, block)), block.toString())
        }
    }

    @Test
    fun aDirectionOrASpeedTakesNoEdge() = runTest {
        val context = commandContext(enemy)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Enemy", 0, BlockDefinition.MoveInDirection(NumberValue.Random(0f, 360f), NumberValue.Property("hp")))))
        listOf(
            BlockDefinition.MoveInDirection(direction = NumberValue.Edge(SceneEdge.RIGHT)),
            BlockDefinition.MoveInDirection(speed = NumberValue.BetweenEdges(SceneEdge.TOP, to = SceneEdge.BOTTOM)),
            BlockDefinition.MoveTowardsNearest("Enemy", NumberValue.Edge(SceneEdge.LEFT)),
        ).forEach { block ->
            assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(AddBlock, AddBlock.Input("Enemy", 0, block)), block.toString())
        }
    }

    @Test
    fun aPropertyReadAsANumberIsAUsageThatBecomesItsStartingValue() = runTest {
        val context = commandContext(enemy)
        val usages = context.usagesOf(RelationSubject.Property("Enemy", "speed"))
        assertEquals(listOf(RelationKind.READS_AS_NUMBER, RelationKind.READS_AS_NUMBER, RelationKind.READS), usages.map { it.kind })
        assertEquals(listOf(UsageEffect.VALUE_TO_NUMBER, UsageEffect.VALUE_TO_NUMBER, UsageEffect.SECTION_REMOVED), usages.map { it.effect })
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Enemy", "speed"))) { context.blockTypes!!.types.value }
        assertEquals(
            listOf(
                BlockDefinition.SetVelocity(x = NumberValue.Fixed(120f), y = NumberValue.Random(-1f, 1f)),
                BlockDefinition.If(ConditionDefinition.Number("hp", Comparison.GREATER, NumberValue.Fixed(120f))),
                BlockDefinition.Stop,
                BlockDefinition.End,
            ),
            after.single().events.single().blocks,
        )
    }

    @Test
    fun deletingAManagerTypeTurnsTheValuesReadingItIntoNumbers() = runTest {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("level", initialValue = 3f)))
        val ball = ActorTypeDefinition(
            name = "Ball",
            events = listOf(
                EventDefinition(
                    EventType.EVERY_FRAME,
                    blocks = listOf(
                        BlockDefinition.SetVelocity(x = NumberValue.Property("level", manager = "Game"), y = NumberValue.Property(manager = "Game")),
                        BlockDefinition.Stop,
                    ),
                ),
            ),
        )
        val context = commandContext(game, ball)
        assertEquals(listOf(UsageEffect.VALUE_TO_NUMBER, UsageEffect.VALUE_TO_NUMBER), context.usagesOf(RelationSubject.Type("Game")).map { it.effect })
        val after = context.assertRoundTrip(Invocation(DeleteManagerType, DeleteManagerType.Input("Game"))) { context.blockTypes!!.types.value }
        assertEquals(
            listOf(BlockDefinition.SetVelocity(x = NumberValue.Fixed(3f), y = NumberValue.Fixed(0f)), BlockDefinition.Stop),
            after.single().events.single().blocks,
        )
    }
}
