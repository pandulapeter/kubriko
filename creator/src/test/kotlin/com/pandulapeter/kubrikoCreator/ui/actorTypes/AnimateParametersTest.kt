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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.AnimationEasing
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
class AnimateParametersTest {
    private val flash = ActorTypeDefinition(name = "Flash")

    @Test
    fun theValueTheEasingAndTheRemovalAreChosenAndAnotherValueStartsFromItsDefaults() = uiTest(width = 1100, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.Animate())
        showing { Row(block) { block = it } }
        onNodeWithText("Animate").assertExists()
        onNodeWithText("Start").assertExists()
        onNodeWithText("The scale").performClick()
        onNodeWithText("The opacity").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.Animate(AnimatedValue.Opacity()), block)
        onNodeWithText("percent").assertExists()
        onNodeWithText("Ending slowly").performClick()
        onNodeWithText("Starting and ending slowly").performClick()
        waitForIdle()
        onNodeWithText("Remove the Actor when it ends").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.Animate(AnimatedValue.Opacity(), easing = AnimationEasing.EASE_IN_OUT, removesActor = true), block)
        saveSnapshot("animate-opacity")
        onNodeWithText("The opacity").performClick()
        onNodeWithText("The color").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.Animate(AnimatedValue.Color(), easing = AnimationEasing.EASE_IN_OUT, removesActor = true), block)
        onNodeWithText("#E05050").performTextReplacement("#80102030")
        waitForIdle()
        assertEquals(AnimatedValue.Color(), (block as BlockDefinition.Animate).value)
        onNodeWithText("#80102030").performTextReplacement("#102030")
        waitForIdle()
        assertEquals(AnimatedValue.Color(to = 0xFF102030), (block as BlockDefinition.Animate).value)
    }

    @Test
    fun aPositionTakesAnXAndAYFromThisActorOrInTheScene() = uiTest(width = 1100, height = 360) {
        var block by mutableStateOf<BlockDefinition>(BlockDefinition.Animate(AnimatedValue.Position(), seconds = NumberValue.Fixed(1f)))
        showing { Row(block) { block = it } }
        onNodeWithText("From this Actor").assertExists()
        onNodeWithText("From this Actor").performClick()
        waitForIdle()
        assertEquals(BlockDefinition.Animate(AnimatedValue.Position(isRelative = false), seconds = NumberValue.Fixed(1f)), block)
        saveSnapshot("animate-position")
    }

    @Composable
    private fun Row(block: BlockDefinition, onBlockChange: (BlockDefinition) -> Unit) = BlockRow(
        block = block,
        nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
        scope = BlockScope.of(flash, listOf(flash)),
        index = 0,
        dragState = BlockDragState(onDrop = { _, _ -> }),
        onBlockChange = onBlockChange,
        onRemove = {},
    )
}
