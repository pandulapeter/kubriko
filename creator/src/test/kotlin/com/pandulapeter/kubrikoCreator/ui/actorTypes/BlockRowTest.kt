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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BlockRowTest {
    private val ball = ActorTypeDefinition(name = "Ball")

    @Test
    fun clickingASwitchLabelTogglesIt() = uiTest(width = 520) {
        var block by mutableStateOf(BlockDefinition.StartTimer())
        showing {
            BlockRow(
                block = block,
                nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
                scope = BlockScope.of(ball, listOf(ball)),
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = { block = it as BlockDefinition.StartTimer },
                onRemove = {},
            )
        }
        onNodeWithText("Repeating").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).performClick()
        waitForIdle()
        assertEquals(true, block.isRepeating)
        saveSnapshot("block-row-repeating")
        onNodeWithText("Repeating").performClick()
        waitForIdle()
        assertEquals(false, block.isRepeating)
    }
}
