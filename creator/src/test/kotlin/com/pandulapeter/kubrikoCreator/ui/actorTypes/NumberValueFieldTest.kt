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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NumberValueFieldTest {
    private val ball = ActorTypeDefinition(name = "Ball", properties = listOf(PropertyDefinition.Number("speed")))
    private val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("level")))
    private val scope = BlockScope.of(ball, listOf(ball, game))

    @Test
    fun theKindMenuTurnsANumberRandomAndThenIntoAProperty() = uiTest(width = 640, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.SetVelocity(x = NumberValue.Fixed(200f)))
        showing {
            BlockRow(
                block = block,
                nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
                scope = scope,
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = { block = it },
                onRemove = {},
            )
        }
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A random number").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetVelocity(x = NumberValue.Random(0f, 200f)), block)
        onNodeWithText("From").assertExists()
        onNodeWithText("To").assertExists()
        saveSnapshot("number-value-random")
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A Property's value").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetVelocity(x = NumberValue.Property()), block)
        onNodeWithText("Choose a Property").performClick()
        onNodeWithText("level of Game").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetVelocity(x = NumberValue.Property("level", manager = "Game")), block)
        saveSnapshot("number-value-property")
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A number").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetVelocity(x = NumberValue.Fixed(0f)), block)
    }

    @Test
    fun aFieldOfWholeNumbersOffersOneRandomKind() = uiTest(width = 640, height = 360) {
        showing {
            Column {
                BlockRow(
                    block = BlockDefinition.Repeat(times = NumberValue.Random(1f, 4f)),
                    nesting = BlockNesting(0, BlockNesting.Role.OPENS),
                    scope = scope,
                    index = 0,
                    dragState = BlockDragState(onDrop = { _, _ -> }),
                    onBlockChange = {},
                    onRemove = {},
                )
            }
        }
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A random number").assertExists()
        onNodeWithText("A random whole number").assertDoesNotExist()
        saveSnapshot("number-value-whole-menu")
    }
}
