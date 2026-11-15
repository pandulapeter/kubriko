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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ColorValue
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
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
 * Color Properties and the Blocks reading them as colors ([ColorValue.Property]): what validating, renaming, finding
 * usages, and deleting make of them.
 */
class ColorValueUsagesTest {
    private val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Color("shade", 0xFF102030)))
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Color("tint", 0xFF20C060), PropertyDefinition.Number("hp")),
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(
                    BlockDefinition.SetColor(listOf(ColorValue.Property("tint"), ColorValue.Fixed(0xFF000000))),
                    BlockDefinition.SetColorProperty("tint", ColorValue.Property("shade", manager = "Game")),
                    BlockDefinition.Animate(AnimatedValue.Color(from = ColorValue.Property("tint"), to = ColorValue.Property("shade", manager = "Game"))),
                ),
            ),
        ),
    )

    @Test
    fun aColorValueNeedsAColorPropertyThatExists() = runTest {
        val context = commandContext(enemy, game)
        fun rejection(block: BlockDefinition) = AddBlock.validate(context, AddBlock.Input("Enemy", 0, block))?.reason
        assertEquals(PlacementProblem.COLOR_PROPERTY_OF_OTHER_KIND, rejection(BlockDefinition.SetColor(listOf(ColorValue.Property("hp")))))
        assertEquals(PlacementProblem.COLOR_PROPERTY_MISSING_SIMILAR, rejection(BlockDefinition.SetColorProperty("tnt")))
        assertNull(rejection(BlockDefinition.SetColorProperty("tint", ColorValue.Property())))
        assertNull(rejection(BlockDefinition.SetColor(listOf(ColorValue.Property("shade", manager = "Game")))))
    }

    @Test
    fun renamingAColorPropertyFollowsEveryBlockReadingIt() = runTest {
        val context = commandContext(enemy, game)
        EditProperty.apply(context, EditProperty.Input("Game", "shade", PropertyDefinition.Color("hue", 0xFF102030)))
        EditProperty.apply(context, EditProperty.Input("Enemy", "tint", PropertyDefinition.Color("paint", 0xFF20C060)))
        assertEquals(
            listOf(
                BlockDefinition.SetColor(listOf(ColorValue.Property("paint"), ColorValue.Fixed(0xFF000000))),
                BlockDefinition.SetColorProperty("paint", ColorValue.Property("hue", manager = "Game")),
                BlockDefinition.Animate(AnimatedValue.Color(from = ColorValue.Property("paint"), to = ColorValue.Property("hue", manager = "Game"))),
            ),
            context.blockTypes!!.types.value.single { it.name == "Enemy" }.events.single().blocks,
        )
    }

    @Test
    fun deletingWhatAColorValueReadsLeavesItsStartingColorAndRemovesTheBlockSettingIt() = runTest {
        val context = commandContext(enemy, game)
        assertEquals(
            listOf(RelationKind.READS_VALUE, RelationKind.READS_VALUE) to listOf(UsageEffect.VALUE_TO_FIXED, UsageEffect.VALUE_TO_FIXED),
            context.usagesOf(RelationSubject.Property("Game", "shade")).let { usages -> usages.map { it.kind } to usages.map { it.effect } },
        )
        val afterDeletingGame = context.assertRoundTrip(Invocation(DeleteManagerType, DeleteManagerType.Input("Game"))) { context.blockTypes!!.types.value }
        assertEquals(
            listOf(
                BlockDefinition.SetColor(listOf(ColorValue.Property("tint"), ColorValue.Fixed(0xFF000000))),
                BlockDefinition.SetColorProperty("tint", ColorValue.Fixed(0xFF102030)),
                BlockDefinition.Animate(AnimatedValue.Color(from = ColorValue.Property("tint"), to = ColorValue.Fixed(0xFF102030))),
            ),
            afterDeletingGame.single { it.name == "Enemy" }.events.single().blocks,
        )
        val afterRemovingTint = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Enemy", "tint"))) { context.blockTypes!!.types.value }
        assertEquals(
            listOf(
                BlockDefinition.SetColor(listOf(ColorValue.Fixed(0xFF20C060), ColorValue.Fixed(0xFF000000))),
                BlockDefinition.Animate(AnimatedValue.Color(from = ColorValue.Fixed(0xFF20C060), to = ColorValue.Fixed(0xFF102030))),
            ),
            afterRemovingTint.single { it.name == "Enemy" }.events.single().blocks,
        )
    }
}
