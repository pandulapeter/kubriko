/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.failed
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.ok
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceSetupTest {
    private val tools = FakeToolProcesses()
    private val console = ConsoleLog()
    private val opened = mutableListOf<URI>()
    private var canOpenBrowser = true

    private val statuses get() = console.entries.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    private fun setup() = DeviceSetup(
        openBrowser = { address ->
            opened += address
            canOpenBrowser
        },
        tools = tools,
        console = console,
    )

    @Test
    fun androidsEmulatorsAreSetUpFromItsGuideWhoseAddressTheConsoleGivesWithoutABrowser() = blockingTest {
        setup().open(PlayTarget.Kind.ANDROID)
        canOpenBrowser = false
        setup().open(PlayTarget.Kind.ANDROID)
        assertEquals(listOf(DeviceSetup.ANDROID_EMULATORS_GUIDE, DeviceSetup.ANDROID_EMULATORS_GUIDE), opened)
        assertEquals(listOf(WorkspaceStatus.BROWSER_NOT_OPENED), statuses)
    }

    @Test
    fun iosSimulatorsAreSetUpInDeviceHubOrElseTheSimulatorApp() = blockingTest {
        tools.answer("-b com.apple.dt.Devices", ok(""))
        setup().open(PlayTarget.Kind.IOS)
        assertTrue(statuses.isEmpty())
        tools.answer("-b com.apple.dt.Devices", failed("Unable to find application with bundle identifier com.apple.dt.Devices"))
        tools.answer("-a Simulator", ok(""))
        setup().open(PlayTarget.Kind.IOS)
        assertTrue(statuses.isEmpty())
        tools.answer("-a Simulator", failed("Unable to find application named 'Simulator'"))
        setup().open(PlayTarget.Kind.IOS)
        assertEquals(listOf(WorkspaceStatus.IOS_SIMULATOR_APPS_MISSING), statuses)
        assertEquals(5, tools.commands.size)
    }
}
