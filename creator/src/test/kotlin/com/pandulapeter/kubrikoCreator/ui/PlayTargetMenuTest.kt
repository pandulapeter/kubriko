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
                targets = PlayTargets(listOf(PlayTarget.DESKTOP, other, PlayTarget.WEB), chosen = PlayTarget.DESKTOP),
                onOpened = { events += "opened" },
                onChosen = { events += "chose ${it.id}" },
                onConfigurePlatforms = { events += "more" },
                onSetUpMore = { events += "set up $it" },
            )
        }
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("Configure platforms…").assertExists()
        onNodeWithText("Other window").performClick()
        waitForIdle()
        assertEquals(listOf("opened", "chose other"), events)
    }

    @Test
    fun androidAndIosDevicesAreInMenusOfTheirOwnEndingInSettingUpMore() = uiTest(width = 640, height = 480) {
        val phone = PlayTarget(id = "android:serial", kind = PlayTarget.Kind.ANDROID, name = "Pixel 7")
        val emulator = PlayTarget(id = "android-emulator:Medium_Phone", kind = PlayTarget.Kind.ANDROID, name = "Medium Phone", isVirtual = true)
        val simulator = PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS, name = "iPhone 16 (iOS 18.2)", isVirtual = true)
        val events = mutableListOf<String>()
        showing {
            PlayTargetMenu(
                targets = PlayTargets(listOf(PlayTarget.DESKTOP, PlayTarget.WEB, emulator, phone, simulator), chosen = PlayTarget.DESKTOP),
                onOpened = {},
                onChosen = { events += "chose ${it.id}" },
                onConfigurePlatforms = {},
                onSetUpMore = { events += "set up $it" },
            )
        }
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("Pixel 7").assertDoesNotExist()
        onNodeWithText("Android").performClick()
        waitForIdle()
        onNodeWithText("Pixel 7").assertExists()
        onNodeWithText("Set up more emulators…").assertExists()
        saveSnapshot("play-target-menu-android")
        onNodeWithText("Medium Phone").performClick()
        waitForIdle()
        onNodeWithText("Pixel 7").assertDoesNotExist()
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("iOS").performClick()
        waitForIdle()
        onNodeWithText("New simulator…").performClick()
        waitForIdle()
        assertEquals(listOf("chose android-emulator:Medium_Phone", "set up IOS"), events)
    }

    @Test
    fun aConfiguredGroupWithNothingFoundOffersOnlyTheWayToSetOneUp() = uiTest(width = 640, height = 480) {
        val events = mutableListOf<String>()
        showing {
            PlayTargetMenu(
                targets = PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP, setOf(PlayTarget.Kind.DESKTOP, PlayTarget.Kind.ANDROID)),
                onOpened = {},
                onChosen = {},
                onConfigurePlatforms = {},
                onSetUpMore = { events += "set up $it" },
            )
        }
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("iOS").assertDoesNotExist()
        onNodeWithText("Android").performClick()
        waitForIdle()
        saveSnapshot("play-target-menu-android-empty")
        onNodeWithText("Set up more emulators…").performClick()
        waitForIdle()
        assertEquals(listOf("set up ANDROID"), events)
    }

    @Test
    fun moreTargetsLeadToProjectSettings() = uiTest {
        val events = mutableListOf<String>()
        showing {
            PlayTargetMenu(
                targets = PlayTargets(listOf(PlayTarget.DESKTOP), chosen = PlayTarget.DESKTOP),
                onOpened = {},
                onChosen = { events += "chose ${it.id}" },
                onConfigurePlatforms = { events += "more" },
                onSetUpMore = {},
            )
        }
        onNodeWithText("Desktop").performClick()
        waitForIdle()
        onNodeWithText("Configure platforms…").performClick()
        waitForIdle()
        assertEquals(listOf("more"), events)
    }
}
