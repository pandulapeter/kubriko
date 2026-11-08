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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.IosDeviceType
import com.pandulapeter.kubrikoCreator.workspace.IosRuntime
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorCreation
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorOptions
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NewIosSimulatorDialogTest {
    private val iPhone = IosDeviceType("com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro", "iPhone 18 Pro")
    private val iPhoneDuo = IosDeviceType("com.apple.CoreSimulator.SimDeviceType.iPhone-Duo", "iPhone Duo")
    private val iPad = IosDeviceType("com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation", "iPad mini (6th generation)")
    private val ios27 = IosRuntime("com.apple.CoreSimulator.SimRuntime.iOS-27-2", "iOS 27.2", "24B8117", setOf(iPhone.identifier, iPad.identifier))
    private val ios27Duo = IosRuntime("com.apple.CoreSimulator.SimRuntime.iOS-27-2-iPhone19-4", "iOS 27.2", "24B91016s", setOf(iPhoneDuo.identifier))
    private val ios18 = IosRuntime("com.apple.CoreSimulator.SimRuntime.iOS-18-2", "iOS 18.2", "22C150", setOf(iPad.identifier))
    private val ready = IosSimulatorOptions.Ready(listOf(iPhoneDuo, iPhone, iPad), listOf(ios27, ios27Duo, ios18), takenNames = setOf("iPhone 18 Pro"))
    private val made = mutableListOf<Triple<String, IosDeviceType, IosRuntime>>()
    private var closings = 0

    private fun ComposeUiTest.showingDialog(
        read: suspend () -> IosSimulatorOptions = { ready },
        creation: suspend () -> IosSimulatorCreation = { IosSimulatorCreation.Created(PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS)) },
    ) = showing {
        NewIosSimulatorDialog(
            state = rememberNewIosSimulatorState(
                read = read,
                create = { name, type, runtime ->
                    made += Triple(name, type, runtime)
                    creation()
                },
                onCreated = { closings++ },
            ),
            onDismiss = { closings++ },
        )
    }

    @Test
    fun theSimulatorStartsAsTheNewestIphoneUnderAFreeNameAndIsMadeOfWhatIsChosen() = uiTest(width = 960, height = 640) {
        showingDialog()
        onNodeWithText("New iOS simulator").assertExists()
        onNodeWithText("iPhone 18 Pro 2").assertExists()
        saveSnapshot("new-ios-simulator")
        onNodeWithText("iPhone 18 Pro").performClick()
        onNodeWithText("iPad mini (6th generation)").performClick()
        onNodeWithText("iOS 27.2").performClick()
        saveSnapshot("new-ios-simulator-versions")
        onNodeWithText("iOS 18.2").performClick()
        onNodeWithText("Create").performClick()
        waitForIdle()
        assertEquals(listOf(Triple("iPad mini (6th generation)", iPad, ios18)), made)
        assertEquals(1, closings)
    }

    @Test
    fun enterInTheNameMakesTheSimulatorAndEscapeLeavesTheDialog() = uiTest(width = 960, height = 640) {
        showingDialog()
        onNode(hasSetTextAction()).performClick()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, closings)
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf(Triple("iPhone 18 Pro 2", iPhone, ios27)), made)
        assertEquals(2, closings)
    }

    @Test
    fun aNarrowWindowShrinksTheDialogRatherThanCuttingItOff() = uiTest(width = 360, height = 640) {
        showingDialog()
        onNodeWithText("iPhone 18 Pro 2").assertIsDisplayed()
        onNodeWithText("iOS version").assertIsDisplayed()
        onNodeWithText("Create").assertIsDisplayed()
        val createEnd = onNodeWithText("Create").fetchSemanticsNode().boundsInWindow.right
        assertTrue(createEnd <= 360f, "Create ends at $createEnd, past the window's edge")
        saveSnapshot("new-ios-simulator-narrow")
    }

    @Test
    fun aTakenNameIsRefusedInTheFieldAndARefusalSaysWhyInXcodesWords() = uiTest(width = 960, height = 640) {
        showingDialog(creation = { IosSimulatorCreation.Refused("Incompatible device") })
        onNode(hasSetTextAction()).performTextReplacement("iphone 18 pro")
        onNodeWithText("Create").performClick()
        onNodeWithText("There's already a simulator called iphone 18 pro.").assertExists()
        saveSnapshot("new-ios-simulator-name-taken")
        onNode(hasSetTextAction()).performTextReplacement("Duo")
        onNodeWithText("There's already a simulator called", substring = true).assertDoesNotExist()
        onNodeWithText("Create").performClick()
        waitForIdle()
        onNodeWithText("Xcode couldn't make the simulator.").assertExists()
        onNodeWithText("Incompatible device").assertExists()
        saveSnapshot("new-ios-simulator-refused")
        assertEquals(0, closings)
    }

    @Test
    fun theDialogCanNotBeLeftWhileTheSimulatorIsBeingMade() = uiTest(width = 960, height = 640) {
        val finish = CompletableDeferred<IosSimulatorCreation>()
        showingDialog(creation = { finish.await() })
        onNodeWithText("Create").performClick()
        onNodeWithText("Creating…").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        saveSnapshot("new-ios-simulator-creating")
        finish.complete(IosSimulatorCreation.TimedOut)
        waitForIdle()
        onNodeWithText("Xcode didn't finish making the simulator within a minute, so it was stopped.").assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closings)
    }

    @Test
    fun aMacWithoutAnIosVersionSaysHowToGetOneAndChecksAgain() = uiTest(width = 960, height = 640) {
        var reads = 0
        showingDialog(read = { if (reads++ == 0) IosSimulatorOptions.NoRuntime else ready })
        onNodeWithText("Xcode → Settings → Components", substring = true).assertExists()
        saveSnapshot("new-ios-simulator-no-runtime")
        onNodeWithText("Check again").performClick()
        waitForIdle()
        onNodeWithText("iPhone 18 Pro 2").assertExists()
    }

    @Test
    fun aListXcodeCanNotGiveSaysWhyAndChecksAgainOnceXcodeWasOpened() = uiTest(width = 960, height = 640) {
        var reads = 0
        showingDialog(read = { if (reads++ == 0) IosSimulatorOptions.Unreadable("xcrun: error: unable to find utility \"simctl\"") else ready })
        onNodeWithText("Xcode couldn't list what it can simulate.", substring = true).assertExists()
        onNodeWithText("xcrun: error: unable to find utility \"simctl\"").assertExists()
        saveSnapshot("new-ios-simulator-unreadable")
        onNodeWithText("Check again").performClick()
        waitForIdle()
        onNodeWithText("iPhone 18 Pro 2").assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closings)
    }
}
