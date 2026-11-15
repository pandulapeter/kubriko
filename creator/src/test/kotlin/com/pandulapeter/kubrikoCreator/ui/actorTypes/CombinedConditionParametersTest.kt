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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.TextValue
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CombinedConditionParametersTest {
    private val scope = BlockScope("Ball", BlockTypeKind.ACTOR, listOf(PropertyDefinition.Toggle("isFast"), PropertyDefinition.Number("speed")))

    @Test
    fun combiningKeepsTheConditionThereIsAndConditionsAreAddedAndRemovedOneByOne() = uiTest(width = 900, height = 400) {
        var condition by mutableStateOf<ConditionDefinition>(ConditionDefinition.Number("speed"))
        showing { ConditionParameters(condition, scope) { condition = it } }
        onNodeWithText("A number Property").performClick()
        onNodeWithText("Several conditions").performClick()
        waitForIdle()
        assertEquals(ConditionDefinition.Combined(listOf(ConditionDefinition.Number("speed"), ConditionDefinition.Toggle())), condition)
        onNodeWithText("all of these hold").performClick()
        onNodeWithText("any of these holds").performClick()
        onNodeWithContentDescription("Add a condition").performClick()
        waitForIdle()
        assertEquals(ConditionDefinition.Combined(listOf(ConditionDefinition.Number("speed"), ConditionDefinition.Toggle(), ConditionDefinition.Toggle()), isAll = false), condition)
        saveSnapshot("combined-condition")
        onAllNodesWithContentDescription("Remove this condition")[0].performClick()
        waitForIdle()
        assertEquals(ConditionDefinition.Combined(listOf(ConditionDefinition.Toggle(), ConditionDefinition.Toggle()), isAll = false), condition)
    }

    @Test
    fun theFieldBeingTypedInGoesWithItsConditionRatherThanTypingIntoTheNextOne() = uiTest(width = 900, height = 400) {
        val texts = BlockScope("Ball", BlockTypeKind.ACTOR, listOf(PropertyDefinition.Text("title")))
        var condition by mutableStateOf<ConditionDefinition>(
            ConditionDefinition.Combined(listOf(ConditionDefinition.Text("title", value = TextValue.Fixed("first")), ConditionDefinition.Text("title", value = TextValue.Fixed("second")))),
        )
        showing { ConditionParameters(condition, texts) { condition = it } }
        onNodeWithText("first").performTextReplacement("firstly")
        waitForIdle()
        onNodeWithText("firstly").assertIsFocused()
        onAllNodesWithContentDescription("Remove this condition")[0].performClick()
        waitForIdle()
        assertEquals(ConditionDefinition.Combined(listOf(ConditionDefinition.Text("title", value = TextValue.Fixed("second")))), condition)
        onNodeWithText("second").assertIsNotFocused()
    }
}
