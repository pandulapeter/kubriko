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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.SteeringKeys
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MoveWithKeysParametersTest {
    private val hero = ActorTypeDefinition(name = "Hero")

    @Test
    fun theKeysAndTheDiagonalsAreChosen() = uiTest(width = 900, height = 300) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.MoveWithKeys())
        showing {
            BlockRow(
                block = block,
                nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
                scope = BlockScope.of(hero, listOf(hero)),
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = { block = it },
                onRemove = {},
            )
        }
        onNodeWithText("Move with keys").assertExists()
        onNodeWithText("units per second").assertExists()
        onNodeWithText("The arrow keys").performClick()
        onNodeWithText("The arrow keys or WASD").performClick()
        waitForIdle()
        onNodeWithText("Diagonally too").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.MoveWithKeys(SteeringKeys.ARROWS_AND_WASD, movesDiagonally = false), block)
        saveSnapshot("move-with-keys")
    }
}
