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
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlayTargetMenuTest {

    @Test
    fun theMenuLooksAgainWhenItOpensAndChoosesWhereToPlay() = uiTest {
        val other = PlayTarget(id = "other", kind = PlayTarget.Kind.DESKTOP, name = "Other window")
        val events = mutableListOf<String>()
        showing {
            PlayTargetMenu(
                targets = PlayTargets(listOf(PlayTarget.DESKTOP, other), chosen = PlayTarget.DESKTOP),
                onOpened = { events += "opened" },
                onChosen = { events += "chose ${it.id}" },
                onMoreTargets = { events += "more" },
            )
        }
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("Build for more platforms…").assertExists()
        saveSnapshot("play-target-menu")
        onNodeWithText("Other window").performClick()
        waitForIdle()
        assertEquals(listOf("opened", "chose other"), events)
    }

    @Test
    fun moreTargetsLeadToProjectSettings() = uiTest {
        val events = mutableListOf<String>()
        showing {
            PlayTargetMenu(
                targets = PlayTargets(listOf(PlayTarget.DESKTOP), chosen = PlayTarget.DESKTOP),
                onOpened = {},
                onChosen = { events += "chose ${it.id}" },
                onMoreTargets = { events += "more" },
            )
        }
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("Build for more platforms…").performClick()
        waitForIdle()
        assertEquals(listOf("more"), events)
    }
}
