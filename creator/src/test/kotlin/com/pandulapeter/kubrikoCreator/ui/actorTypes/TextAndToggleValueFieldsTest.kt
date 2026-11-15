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

import androidx.compose.runtime.Composable
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
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.TextValue
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TextAndToggleValueFieldsTest {
    private val hero = ActorTypeDefinition(
        name = "Hero",
        properties = listOf(PropertyDefinition.Text("name"), PropertyDefinition.Text("nickname"), PropertyDefinition.Toggle("isHappy"), PropertyDefinition.Toggle("isSunny")),
    )

    @Test
    fun aTextBlockCopiesAnotherTextProperty() = uiTest(width = 900, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.SetText("name"))
        showing { Row(block) { block = it } }
        onNodeWithContentDescription("Kind of text").performClick()
        onNodeWithText("A text Property's value").performClick()
        waitForIdle()
        onNodeWithText("Choose a Property").performClick()
        onNodeWithText("nickname").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetText("name", TextValue.Property("nickname")), block)
        saveSnapshot("set-text-property-value")
        onNodeWithContentDescription("Kind of text").performClick()
        onNodeWithText("A text").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.SetText("name"), block)
    }

    @Test
    fun aToggleConditionComparesWithAnotherToggleProperty() = uiTest(width = 900, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.If(ConditionDefinition.Toggle("isHappy")))
        showing { Row(block) { block = it } }
        onNodeWithContentDescription("Kind of on or off").performClick()
        onNodeWithText("A toggle Property's value").performClick()
        waitForIdle()
        onNodeWithText("Choose a Property").performClick()
        onNodeWithText("isSunny").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.If(ConditionDefinition.Toggle("isHappy", ToggleValue.Property("isSunny"))), block)
        onNodeWithText("is as").assertExists()
        saveSnapshot("toggle-condition-property-value")
    }

    @Composable
    private fun Row(block: BlockDefinition, onBlockChange: (BlockDefinition) -> Unit) = BlockRow(
        block = block,
        nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
        scope = BlockScope.of(hero, listOf(hero)),
        index = 0,
        dragState = BlockDragState(onDrop = { _, _ -> }),
        onBlockChange = onBlockChange,
        onRemove = {},
    )
}
