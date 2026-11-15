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
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.TextValue
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Text and toggle values reading another Property ([TextValue.Property], [ToggleValue.Property]): what validating, renaming,
 * finding usages, deleting, and reading them as sentences make of them.
 */
class TextAndToggleValueUsagesTest {
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Text("motto", "Go"), PropertyDefinition.Toggle("isHard", true)),
        texts = listOf(ScreenTextDefinition("Hard", shownWhen = ConditionDefinition.Toggle("isHard", ToggleValue.Property("isHard", manager = "Game")))),
    )
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Text("cry"), PropertyDefinition.Toggle("isAngry"), PropertyDefinition.Number("hp")),
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(
                    BlockDefinition.SetText("cry", TextValue.Property("motto", manager = "Game")),
                    BlockDefinition.If(ConditionDefinition.Toggle("isAngry", ToggleValue.Property("isHard", manager = "Game"))),
                    BlockDefinition.SetToggle("isAngry", ToggleValue.Property("isHard", manager = "Game")),
                    BlockDefinition.End,
                ),
            ),
        ),
    )

    @Test
    fun aValueNeedsAPropertyOfItsKindThatExists() = runTest {
        val context = commandContext(enemy, game)
        fun rejection(block: BlockDefinition) = AddBlock.validate(context, AddBlock.Input("Enemy", 0, block))?.reason
        assertEquals(PlacementProblem.TEXT_PROPERTY_OF_OTHER_KIND, rejection(BlockDefinition.SetText("cry", TextValue.Property("hp"))))
        assertEquals(PlacementProblem.TOGGLE_PROPERTY_MISSING_SIMILAR, rejection(BlockDefinition.If(ConditionDefinition.Toggle("isAngry", ToggleValue.Property("isAngy")))))
        assertNull(rejection(BlockDefinition.SetText("cry", TextValue.Property())))
        assertNull(rejection(BlockDefinition.If(ConditionDefinition.Text("cry", TextValue.Property("motto", manager = "Game")))))
    }

    @Test
    fun renamingAPropertyFollowsEveryValueReadingIt() = runTest {
        val context = commandContext(enemy, game)
        EditProperty.apply(context, EditProperty.Input("Game", "isHard", PropertyDefinition.Toggle("isTough", true)))
        EditProperty.apply(context, EditProperty.Input("Game", "motto", PropertyDefinition.Text("slogan", "Go")))
        val types = context.blockTypes!!.types.value
        assertEquals(
            listOf(
                BlockDefinition.SetText("cry", TextValue.Property("slogan", manager = "Game")),
                BlockDefinition.If(ConditionDefinition.Toggle("isAngry", ToggleValue.Property("isTough", manager = "Game"))),
                BlockDefinition.SetToggle("isAngry", ToggleValue.Property("isTough", manager = "Game")),
                BlockDefinition.End,
            ),
            types.single { it.name == "Enemy" }.events.single().blocks,
        )
        assertEquals(ConditionDefinition.Toggle("isTough", ToggleValue.Property("isTough", manager = "Game")), (types.single { it.name == "Game" } as ManagerTypeDefinition).texts.single().shownWhen)
    }

    @Test
    fun deletingWhatAValueReadsLeavesItsStartingValue() = runTest {
        val context = commandContext(enemy, game)
        assertEquals(
            setOf(RelationKind.READS_VALUE) to setOf(UsageEffect.VALUE_TO_FIXED),
            context.usagesOf(RelationSubject.Property("Game", "motto")).let { usages -> usages.mapTo(HashSet()) { it.kind } to usages.mapTo(HashSet()) { it.effect } },
        )
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Game", "motto"))) { context.blockTypes!!.types.value }
        assertEquals(BlockDefinition.SetText("cry", TextValue.Fixed("Go")), after.single { it.name == "Enemy" }.events.single().blocks.first())
        val withoutHard = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Enemy", "isAngry"))) { context.blockTypes!!.types.value }
        assertEquals(listOf(BlockDefinition.SetText("cry", TextValue.Fixed("Go"))), withoutHard.single { it.name == "Enemy" }.events.single().blocks)
    }

    @Test
    fun theValuesReadAsSentences() {
        assertEquals("Set cry to motto of Game", BlockNotation.of(enemy.events.single().blocks[0]))
        assertEquals("If isAngry is on or off as isHard of Game is", BlockNotation.of(enemy.events.single().blocks[1]))
        assertEquals("Turn isAngry on or off as isHard of Game is", BlockNotation.of(enemy.events.single().blocks[2]))
    }
}
