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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
class AppearanceBlockParametersTest {
    private val spark = ActorTypeDefinition(name = "Spark")

    @Test
    fun aColorBlockAddsAndRemovesColorsToPickFromAndTakesOnlyOpaqueOnes() = uiTest(width = 900, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.SetColor())
        showing { Row(block) { block = it } }
        onNodeWithText("Set the color").assertExists()
        onNodeWithContentDescription("Add a color, so one is picked at random").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetColor(listOf(0xFFE05050, 0xFFE05050)), block)
        onAllNodesWithText("#E05050")[1].performTextReplacement("#80102030")
        waitForIdle()
        assertEquals(BlockDefinition.SetColor(listOf(0xFFE05050, 0xFFE05050)), block)
        onNodeWithText("#80102030").performTextReplacement("#102030")
        waitForIdle()
        assertEquals(BlockDefinition.SetColor(listOf(0xFFE05050, 0xFF102030)), block)
        saveSnapshot("set-color")
        onAllNodesWithContentDescription("Remove this color")[0].performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetColor(listOf(0xFF102030)), block)
        onNodeWithContentDescription("Remove this color").assertDoesNotExist()
    }

    @Test
    fun theOpacityBlocksTakeAPercentThatCanBeRandom() = uiTest(width = 900, height = 360) {
        var setOpacity by mutableStateOf<BlockDefinition>(BlockDefinition.SetOpacity())
        var changeOpacity by mutableStateOf<BlockDefinition>(BlockDefinition.ChangeOpacity())
        showing {
            Column {
                Row(setOpacity) { setOpacity = it }
                Row(changeOpacity) { changeOpacity = it }
            }
        }
        onNodeWithText("Set the opacity").assertExists()
        onNodeWithText("Change the opacity").assertExists()
        assertEquals(2, onAllNodesWithText("percent").fetchSemanticsNodes().size)
        onAllNodesWithContentDescription("Kind of number")[0].performClick()
        onNodeWithText("A random number").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetOpacity(NumberValue.Random(0f, 50f)), setOpacity)
        saveSnapshot("opacity")
    }

    @androidx.compose.runtime.Composable
    private fun Row(block: BlockDefinition, onBlockChange: (BlockDefinition) -> Unit) = BlockRow(
        block = block,
        nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
        scope = BlockScope.of(spark, listOf(spark)),
        index = 0,
        dragState = BlockDragState(onDrop = { _, _ -> }),
        onBlockChange = onBlockChange,
        onRemove = {},
    )
}
