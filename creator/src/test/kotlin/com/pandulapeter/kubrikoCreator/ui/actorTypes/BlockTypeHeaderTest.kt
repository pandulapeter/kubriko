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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BlockTypeHeaderTest {

    @Test
    fun theChildrenPastTheFirstFewAreChosenFromASearchableList() = uiTest(width = 900, height = 500) {
        val children = (1..30).map { "Enemy$it" } + "Vampire"
        var selected: String? = null
        showing {
            BlockTypeHeader(
                definition = ActorTypeDefinition(name = "Enemy"),
                inheritance = Inheritance(children = children),
                flashKey = null,
                onRename = { null },
                onParentChange = {},
                onTypeSelected = { selected = it },
                onDelete = {},
            )
        }
        onNodeWithText("28 more").performClick()
        onNode(hasSetTextAction()).performTextInput("vamp")
        onNodeWithText("Enemy4").assertDoesNotExist()
        saveSnapshot("type-header-children-search")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertEquals("Vampire", selected)
    }
}
