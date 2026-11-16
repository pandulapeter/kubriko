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
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SizeBlockParametersTest {
    private val balloon = ActorTypeDefinition(name = "Balloon", properties = listOf(PropertyDefinition.Number("air")))

    @Test
    fun theSizeAndScaleBlocksTakeTheirNumbers() = uiTest(width = 900, height = 360) {
        var setSize by mutableStateOf<BlockDefinition>(BlockDefinition.SetSize())
        var setScale by mutableStateOf<BlockDefinition>(BlockDefinition.SetScale())
        var changeScale by mutableStateOf<BlockDefinition>(BlockDefinition.ChangeScale())
        showing {
            Column {
                Row(setSize) { setSize = it }
                Row(setScale) { setScale = it }
                Row(changeScale) { changeScale = it }
            }
        }
        onNodeWithText("Set the size").assertExists()
        onNodeWithText("Set the scale").assertExists()
        onNodeWithText("Change the scale").assertExists()
        onNodeWithText("Width").assertExists()
        onNodeWithText("Height").assertExists()
        onNodeWithText("2").performTextReplacement("1.5")
        waitForIdle()
        assertEquals(BlockDefinition.SetScale(NumberValue.Fixed(1.5f)), setScale)
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A Property's value").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetSize(width = NumberValue.Property()), setSize)
        saveSnapshot("size-and-scale")
    }

    @androidx.compose.runtime.Composable
    private fun Row(block: BlockDefinition, onBlockChange: (BlockDefinition) -> Unit) = BlockRow(
        block = block,
        nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
        scope = BlockScope.of(balloon, listOf(balloon)),
        index = 0,
        dragState = BlockDragState(onDrop = { _, _ -> }),
        onBlockChange = onBlockChange,
        onRemove = {},
    )
}
