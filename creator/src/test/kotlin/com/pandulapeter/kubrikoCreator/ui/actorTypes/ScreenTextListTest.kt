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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScreenTextListTest {

    @Test
    fun removingATextAboveTheOneBeingTypedIntoKeepsEachRowsText() = uiTest {
        var texts by mutableStateOf(listOf(ScreenTextDefinition(text = "A"), ScreenTextDefinition(text = "B"), ScreenTextDefinition(text = "C")))
        showing {
            ScreenTextList(
                texts = texts,
                onAdded = {},
                onChanged = { index, text -> texts = texts.toMutableList().also { it[index] = text } },
                onRemoved = { index -> texts = texts.filterIndexed { other, _ -> other != index } },
            )
        }
        val field = onNode(hasSetTextAction() and hasText("B"))
        field.requestFocus()
        field.performTextReplacement("B2")
        waitForIdle()
        texts = texts.drop(1)
        waitForIdle()
        val typedField = onNode(hasSetTextAction() and hasText("B2"))
        typedField.assertIsFocused()
        onAllNodes(hasSetTextAction() and hasText("B2")).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("C")).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("A")).assertCountEquals(0)
        typedField.performTextReplacement("B3")
        waitForIdle()
        assertEquals(listOf("B3", "C"), texts.map { it.text })
    }
}
