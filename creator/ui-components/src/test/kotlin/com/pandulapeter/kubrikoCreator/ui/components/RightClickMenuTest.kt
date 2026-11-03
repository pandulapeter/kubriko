/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RightClickMenuTest {

    @Test
    fun anEntryWithChildrenOpensTheirMenuBesideItAndChoosingThereClosesEveryMenu() = uiTest(width = 480, height = 320) {
        val events = mutableListOf<String>()
        showing {
            val menu = rememberRightClickMenuState()
            val anchor = rememberMenuAnchor(menu)
            TextButton(modifier = anchor.modifier, onClick = anchor::open) { Text("Open") }
            RightClickMenu(
                state = menu,
                actions = listOf(
                    ContextAction(label = "Plain") { events += "plain" },
                    ContextAction(
                        label = "Group",
                        children = listOf(
                            ContextAction(label = "First") { events += "first" },
                            ContextAction(label = "Second", isSeparated = true) { events += "second" },
                        ),
                    ),
                ),
            )
        }
        onNodeWithText("Open").performClick()
        waitForIdle()
        onNodeWithText("First").assertDoesNotExist()
        onNodeWithText("Group").performClick()
        waitForIdle()
        onNodeWithText("Plain").assertExists()
        saveSnapshot("right-click-menu-submenu")
        onNodeWithText("Second").performClick()
        waitForIdle()
        onNodeWithText("Plain").assertDoesNotExist()
        onNodeWithText("Second").assertDoesNotExist()
        assertEquals(listOf("second"), events)
    }
}
