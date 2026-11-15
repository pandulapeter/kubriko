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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The distance condition, through the commands: where it is allowed, what it may compare with, and what deleting the
 * type it measures the distance to does.
 */
class DistanceConditionTest {
    private val dog = ActorTypeDefinition(name = "Dog")
    private val fleeing = listOf(BlockDefinition.If(ConditionDefinition.Distance(ActorTypeValue.Fixed("Dog"))), BlockDefinition.Stop, BlockDefinition.End)
    private val sheep = ActorTypeDefinition(name = "Sheep", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = fleeing)))
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.EVERY_FRAME)))

    @Test
    fun onlyActorTypesMeasureAndOnlyToATypeThatExistsWithANumberThatIsNoEdge() = runTest {
        val context = commandContext(dog, sheep, game)
        fun rejection(type: String, condition: ConditionDefinition) = AddBlock.validate(context, AddBlock.Input(type, 0, BlockDefinition.If(condition)))?.reason
        assertNull(rejection("Sheep", ConditionDefinition.Distance(ActorTypeValue.Fixed("Dog"))))
        assertEquals(PlacementProblem.CONDITION_NOT_ALLOWED, rejection("Game", ConditionDefinition.Distance(ActorTypeValue.Fixed("Dog"))))
        assertEquals(LookupProblem.ACTOR_TYPE_MISSING, rejection("Sheep", ConditionDefinition.Distance(ActorTypeValue.Fixed("Wolf"))))
        assertEquals(CommandProblem.NUMBER_EDGE_NOT_TAKEN, rejection("Sheep", ConditionDefinition.Distance(ActorTypeValue.Fixed("Dog"), value = NumberValue.Edge(SceneEdge.LEFT))))
    }

    @Test
    fun deletingTheTypeItMeasuresTheDistanceToRemovesItsSection() = runTest {
        val context = commandContext(dog, sheep)
        assertEquals(
            listOf(RelationKind.MEASURES to UsageEffect.SECTION_REMOVED),
            context.usagesOf(RelationSubject.Type("Dog")).map { it.kind to it.effect },
        )
        val after = context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Dog"))) { context.blockTypes!!.types.value }
        assertEquals(emptyList<BlockDefinition>(), after.single { it.name == "Sheep" }.events.single().blocks)
    }

    @Test
    fun itReadsAsASentence() {
        assertEquals("If the distance to the nearest Actor of Dog is less than 100", BlockNotation.of(fleeing.first()))
    }
}
