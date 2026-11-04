/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.GameModules
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GameModuleMenuTest {

    @Test
    fun theMenuListsEveryModuleAndChoosesTheGame() = uiTest(width = 480, height = 320) {
        val chosen = mutableListOf<String>()
        showing {
            GameModuleMenu(
                modules = GameModules(listOf(":app:desktop", ":editor:model", ":editor:region"), ":app:desktop"),
                onChosen = { chosen += it },
            )
        }
        onNodeWithText(":app:desktop").performClick()
        waitForIdle()
        onNodeWithText(":editor:region").assertExists()
        saveSnapshot("game-module-menu")
        onNodeWithText(":editor:model").performClick()
        waitForIdle()
        onNodeWithText(":editor:region").assertDoesNotExist()
        assertEquals(listOf(":editor:model"), chosen)
    }
}
