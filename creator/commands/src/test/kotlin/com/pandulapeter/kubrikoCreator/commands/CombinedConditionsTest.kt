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
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import com.pandulapeter.kubrikoCreator.context.HealthChecks
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.ProjectRelations
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Combined conditions: every rule a single condition is held to reaches each condition one combines, however deep.
 */
class CombinedConditionsTest {
    private val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Toggle("isOver"), PropertyDefinition.Number("lives")))
    private val ball = ActorTypeDefinition(
        name = "Ball",
        properties = listOf(PropertyDefinition.Number("speed")),
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(
                    BlockDefinition.If(
                        ConditionDefinition.Combined(
                            listOf(ConditionDefinition.Toggle("isOver", manager = "Game"), ConditionDefinition.Combined(listOf(ConditionDefinition.Number("speed")), isAll = false)),
                        ),
                    ),
                    BlockDefinition.Stop,
                    BlockDefinition.End,
                ),
            ),
        ),
    )

    @Test
    fun eachCombinedConditionIsValidatedAsASingleOneWouldBe() = runTest {
        val context = commandContext(ball, game)
        fun rejection(condition: ConditionDefinition, type: String = "Ball") = AddBlock.validate(context, AddBlock.Input(type, 0, BlockDefinition.If(condition)))?.reason
        fun combined(vararg conditions: ConditionDefinition) = ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver", manager = "Game")) + conditions)
        assertNull(rejection(combined(ConditionDefinition.Number("speed"))))
        assertEquals(PlacementProblem.NUMBER_PROPERTY_MISSING, rejection(combined(ConditionDefinition.Combined(listOf(ConditionDefinition.Number("hits"))))))
        assertEquals(LookupProblem.GAME_KEY_UNKNOWN, rejection(combined(ConditionDefinition.KeyHeld(KeyChoice("NoSuchKey")))))
        assertEquals(CommandProblem.NUMBER_INVALID, rejection(combined(ConditionDefinition.Chance(com.pandulapeter.kubrikoCreator.actors.NumberValue.Fixed(150f)))))
        assertEquals(LookupProblem.ACTOR_TYPE_MISSING, rejection(combined(ConditionDefinition.ActorCount("Ghost"))))
        val countedToAnEdge = ConditionDefinition.ActorCount("Ball", value = com.pandulapeter.kubrikoCreator.actors.NumberValue.Edge(com.pandulapeter.kubrikoCreator.actors.SceneEdge.LEFT))
        assertEquals(CommandProblem.NUMBER_EDGE_NOT_TAKEN, rejection(combined(countedToAnEdge)))
    }

    @Test
    fun aVelocityCombinedInAManagerTypeIsRefused() = runTest {
        val context = commandContext(ball, game.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME))))
        val input = AddBlock.Input("Game", 0, BlockDefinition.If(ConditionDefinition.Combined(listOf(ConditionDefinition.Velocity()))))
        assertEquals(PlacementProblem.CONDITION_NOT_ALLOWED, AddBlock.validate(context, input)?.reason)
    }

    @Test
    fun whatACombinedConditionReadsIsRelatedRenamedAndCleanedUp() = runTest {
        val read: Set<RelationSubject> = ProjectRelations(listOf(ball, game), emptyMap()).all.filter { it.kind == RelationKind.READS }.mapTo(HashSet()) { it.subject }
        assertEquals(setOf<RelationSubject>(RelationSubject.Property("Game", "isOver"), RelationSubject.Property("Ball", "speed")), read)
        val context = commandContext(ball, game)
        EditProperty.apply(context, EditProperty.Input("Ball", "speed", PropertyDefinition.Number("pace")))
        val renamed = (context.blockTypes!!.types.value.single { it.name == "Ball" }.events.single().blocks.first() as BlockDefinition.If).condition
        assertEquals(ConditionDefinition.Number("pace"), (renamed as ConditionDefinition.Combined).conditions.filterIsInstance<ConditionDefinition.Combined>().single().conditions.single())
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Game", "isOver"))) { context.blockTypes!!.types.value }
        assertEquals(emptyList<BlockDefinition>(), after.single { it.name == "Ball" }.events.single().blocks)
    }

    @Test
    fun aCombinedConditionReadsAsASentenceAndATextBehindOneThatIsNeverTrueIsPointedOut() {
        assertEquals(
            "If (isOver of Game is on) and ((speed is more than 0) or (the game is paused))",
            BlockNotation.of(BlockDefinition.If(ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver", manager = "Game"), ConditionDefinition.Combined(listOf(ConditionDefinition.Number("speed"), ConditionDefinition.Paused()), isAll = false))))),
        )
        val hud = ManagerTypeDefinition(
            name = "Hud",
            properties = listOf(PropertyDefinition.Toggle("isOver")),
            texts = listOf(
                ScreenTextDefinition("Never", shownWhen = ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver"), ConditionDefinition.Toggle()))),
                ScreenTextDefinition("Sometimes", shownWhen = ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver"), ConditionDefinition.Toggle()), isAll = false)),
                ScreenTextDefinition("Always", shownWhen = ConditionDefinition.Combined(emptyList())),
                ScreenTextDefinition("Never either", shownWhen = ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver"), ConditionDefinition.Combined(emptyList(), isAll = false)))),
                ScreenTextDefinition("Compared with nothing", shownWhen = ConditionDefinition.Toggle("isOver", ToggleValue.Property())),
            ),
        )
        assertEquals(
            listOf(HealthFinding(HealthFinding.Kind.TEXT_NEVER_SHOWN, RelationSubject.Type("Hud"), "1"), HealthFinding(HealthFinding.Kind.TEXT_NEVER_SHOWN, RelationSubject.Type("Hud"), "4"),
                HealthFinding(HealthFinding.Kind.TEXT_NEVER_SHOWN, RelationSubject.Type("Hud"), "5"),
            ),
            HealthChecks.of(listOf(hud), ProjectRelations(listOf(hud), emptyMap()), emptyList()).filter { it.kind == HealthFinding.Kind.TEXT_NEVER_SHOWN },
        )
    }

    @Test
    fun deletingWhatOnePartOfAnOrChecksDropsThatPartAndKeepsTheSection() = runTest {
        val or = ConditionDefinition.Combined(listOf(ConditionDefinition.KeyHeld(), ConditionDefinition.Number("lives", manager = "Game")), isAll = false)
        val hud = ManagerTypeDefinition(
            name = "Hud",
            properties = listOf(PropertyDefinition.Toggle("isOver")),
            texts = listOf(ScreenTextDefinition("Careful", shownWhen = ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver"), ConditionDefinition.Number("lives", manager = "Game")), isAll = false))),
        )
        val paddle = ActorTypeDefinition(name = "Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(or), BlockDefinition.Stop, BlockDefinition.End))))
        val context = commandContext(paddle, game, hud)
        val usages = context.usagesOf(RelationSubject.Property("Game", "lives"))
        assertEquals(setOf(UsageEffect.CONDITION_DROPPED), usages.mapTo(HashSet()) { it.effect })
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Game", "lives"))) { context.blockTypes!!.types.value }
        assertEquals(
            listOf(BlockDefinition.If(ConditionDefinition.Combined(listOf(ConditionDefinition.KeyHeld()), isAll = false)), BlockDefinition.Stop, BlockDefinition.End),
            after.single { it.name == "Paddle" }.events.single().blocks,
        )
        assertEquals(ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver")), isAll = false), (after.single { it.name == "Hud" } as ManagerTypeDefinition).texts.single().shownWhen)
    }

    @Test
    fun deletingEverythingAnOrChecksRemovesTheSection() = runTest {
        val or = ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle("isOver", manager = "Game"), ConditionDefinition.Number("lives", manager = "Game")), isAll = false)
        val paddle = ActorTypeDefinition(name = "Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(or), BlockDefinition.Stop, BlockDefinition.End))))
        val context = commandContext(paddle, game)
        val after = context.assertRoundTrip(Invocation(DeleteManagerType, DeleteManagerType.Input("Game"))) { context.blockTypes!!.types.value }
        assertEquals(emptyList<BlockDefinition>(), after.single { it.name == "Paddle" }.events.single().blocks)
    }
}
