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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ComposableSectionTest {

    @Test
    fun aComposableIsMadeOpenedAndNoLongerShown() = uiTest(width = 360, height = 200) {
        var composable by mutableStateOf<String?>(null)
        val events = mutableListOf<String>()
        showing {
            ComposableSection(
                composable = composable,
                onCreate = { events += "create" },
                onRemove = { events += "remove" },
                open = { name, onFailed ->
                    events += "open $name"
                    onFailed()
                },
            )
        }
        onNodeWithText("Your own Compose code", substring = true).assertExists()
        onNodeWithContentDescription("Make a Composable").performClick()
        composable = "com.example.game.ui.HudUi"
        waitForIdle()
        saveSnapshot("composable-section")
        onNodeWithText("com.example.game.ui.HudUi").assertExists()
        onNodeWithContentDescription("Open in your editor").performClick()
        onNodeWithText("No file of the game declares it", substring = true).assertExists()
        onNodeWithContentDescription("Stop showing it (its file stays)").performClick()
        assertEquals(listOf("create", "open com.example.game.ui.HudUi", "remove"), events)
    }
}
