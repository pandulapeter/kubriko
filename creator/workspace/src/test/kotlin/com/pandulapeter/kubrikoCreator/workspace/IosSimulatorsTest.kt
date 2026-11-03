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
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.failed
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.ok
import kotlin.test.Test
import kotlin.test.assertEquals

class IosSimulatorsTest {
    private val tools = FakeToolProcesses()

    private fun ios(udid: String, name: String) = PlayTarget(id = "ios-simulator:$udid", kind = PlayTarget.Kind.IOS, name = name, isVirtual = true)

    @Test
    fun bootedSimulatorsComeFirstThenTheNewestIosVersions() = blockingTest {
        tools.answer("simctl list devices available --json", ok(SIMULATORS))
        assertEquals(
            listOf(
                ios("C", "iPhone 16 (iOS 18.2)"),
                ios("D", "iPhone 17 (iOS 27.2)"),
                ios("A", "iPhone 16 Pro (iOS 18.2)"),
                ios("B", "iPhone SE (3rd generation) (iOS 18.2)"),
            ),
            IosSimulators(tools).targets(),
        )
    }

    @Test
    fun aSimctlThatFailsOrPrintsSomethingElseListsNothing() = blockingTest {
        tools.answer("simctl list devices available --json", failed("xcrun: error: unable to find utility \"simctl\""))
        assertEquals(emptyList(), IosSimulators(tools).targets())
        assertEquals(emptyList(), IosSimulators.parse("not json"))
        assertEquals(emptyList(), IosSimulators.parse("""{ "devices": [] }"""))
    }

    private companion object {
        val SIMULATORS = """
            {
              "devices" : {
                "com.apple.CoreSimulator.SimRuntime.iOS-18-2" : [
                  { "udid" : "A", "isAvailable" : true, "state" : "Shutdown", "name" : "iPhone 16 Pro" },
                  { "udid" : "B", "isAvailable" : true, "state" : "Shutdown", "name" : "iPhone SE (3rd generation)" },
                  { "udid" : "C", "isAvailable" : true, "state" : "Booted", "name" : "iPhone 16" },
                  { "udid" : "X", "isAvailable" : false, "state" : "Shutdown", "name" : "iPhone 15" }
                ],
                "com.apple.CoreSimulator.SimRuntime.iOS-27-2-iPhone19-4" : [
                  { "udid" : "D", "isAvailable" : true, "state" : "Shutdown", "name" : "iPhone 17" }
                ],
                "com.apple.CoreSimulator.SimRuntime.watchOS-11-2" : [
                  { "udid" : "W", "isAvailable" : true, "state" : "Shutdown", "name" : "Apple Watch Series 10 (46mm)" }
                ]
              }
            }
        """.trimIndent()
    }
}
