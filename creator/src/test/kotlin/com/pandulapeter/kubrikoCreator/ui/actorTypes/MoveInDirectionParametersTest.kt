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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MoveInDirectionParametersTest {
    private val debris = ActorTypeDefinition(name = "Debris")

    @Test
    fun theDirectionAndTheSpeedAreFieldsWithTheirUnitsAndEitherCanBeRandom() = uiTest(width = 900, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.MoveInDirection(direction = NumberValue.Fixed(360f)))
        showing {
            BlockRow(
                block = block,
                nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
                scope = BlockScope.of(debris, listOf(debris)),
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = { block = it },
                onRemove = {},
            )
        }
        onNodeWithText("Move in a direction").assertExists()
        onNodeWithText("degrees, clockwise from the right").assertExists()
        onNodeWithText("units per second").assertExists()
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A random number").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.MoveInDirection(direction = NumberValue.Random(0f, 360f)), block)
        onAllNodesWithContentDescription("Kind of number")[1].performClick()
        onNodeWithText("A random number").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.MoveInDirection(direction = NumberValue.Random(0f, 360f), speed = NumberValue.Random(0f, 200f)), block)
        saveSnapshot("move-in-direction")
    }
}
