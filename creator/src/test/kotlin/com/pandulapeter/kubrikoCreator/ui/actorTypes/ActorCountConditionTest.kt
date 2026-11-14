/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ActorCountConditionTest {
    private val game = ManagerTypeDefinition(name = "Game")
    private val types = listOf(ActorTypeDefinition(name = "Ball"), ActorTypeDefinition(name = "Brick"), game)

    @Test
    fun anIfCountsTheActorsOfTheChosenType() = uiTest(width = 900, height = 300) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.If())
        showing {
            BlockRow(
                block = block,
                nesting = BlockNesting(0, BlockNesting.Role.OPENS),
                scope = BlockScope.of(game, types),
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = { block = it },
                onRemove = {},
            )
        }
        onNodeWithText("By chance").performClick()
        onNodeWithText("The number of Actors of").performClick()
        waitForIdle()
        onNodeWithText("Choose an Actor type").performClick()
        onNodeWithText("Brick").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.If(ConditionDefinition.ActorCount("Brick")), block)
        saveSnapshot("actor-count-condition")
        onNodeWithContentDescription("Kind of number").performClick()
        onNodeWithText("A random number").assertExists()
        onNodeWithText("An edge of the Scene").assertDoesNotExist()
    }
}
