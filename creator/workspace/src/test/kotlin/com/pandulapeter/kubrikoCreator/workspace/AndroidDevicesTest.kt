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
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.failed
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.ok
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidDevicesTest {
    private val sdk = testDirectory()
    private val tools = FakeToolProcesses()
    private val devices = AndroidDevices(sdk, tools, isWindows = false)

    private fun android(id: String, name: String) = PlayTarget(id = id, kind = PlayTarget.Kind.ANDROID, name = name, isVirtual = id.startsWith("android-emulator:"))

    @Test
    fun connectedDevicesComeFirstThenTheEmulatorsThatArentRunning() = blockingTest {
        File(devices.emulator).apply { parentFile.mkdirs() }.writeText("")
        tools.answer("devices -l", ok(DEVICES))
        tools.answer("-s emulator-5554 emu avd name", ok("Medium_Phone_API_36\r\nOK\r\n"))
        tools.answer("-list-avds", ok("INFO    | Storing crashdata in: /tmp/android/emu-crash.db\nTablet\nMedium_Phone_API_36\nAutomotive\n"))
        assertEquals(
            listOf(
                android("android-emulator:Medium_Phone_API_36", "Medium Phone API 36"),
                android("android:R58M12345", "SM G991B"),
                android("android:0A071FDD4003TV", "0A071FDD4003TV"),
                android("android-emulator:Automotive", "Automotive"),
                android("android-emulator:Tablet", "Tablet"),
            ),
            devices.targets(),
        )
    }

    @Test
    fun withoutTheEmulatorOnlyConnectedDevicesAreListed() = blockingTest {
        tools.answer("devices -l", ok("List of devices attached\nR58M12345 device usb:1-1 model:SM_G991B transport_id:2\n"))
        assertEquals(listOf(android("android:R58M12345", "SM G991B")), devices.targets())
        assertEquals(false, tools.ran("-list-avds"))
    }

    @Test
    fun anAdbThatFailsListsNothing() = blockingTest {
        tools.answer("devices -l", failed("cannot connect to daemon"))
        assertEquals(emptyList(), devices.targets())
        assertNull(devices.stateOf("R58M12345"))
    }

    @Test
    fun linesOfTheDeviceListAreReadWithTheirStateAndModel() {
        assertNull(AndroidDevices.parseDevice("List of devices attached"))
        assertNull(AndroidDevices.parseDevice("* daemon started successfully"))
        assertNull(AndroidDevices.parseDevice(""))
        assertEquals(
            AndroidDevices.Connected(serial = "0A071FDD4003TV", state = "unauthorized", model = null, avd = null),
            AndroidDevices.parseDevice("0A071FDD4003TV         unauthorized usb:1-2 transport_id:3"),
        )
        assertEquals(
            AndroidDevices.Connected(serial = "R58M12345", state = "device", model = "SM G991B", avd = null),
            AndroidDevices.parseDevice("R58M12345\tdevice usb:1-1 product:o1s model:SM_G991B device:o1s transport_id:2\r"),
        )
    }

    @Test
    fun theToolsAreThoseOfTheSdkOnEverySystem() {
        assertEquals(File(sdk, "platform-tools/adb").path, AndroidDevices(sdk, tools, isWindows = false).adb)
        assertEquals(File(sdk, "platform-tools/adb.exe").path, AndroidDevices(sdk, tools, isWindows = true).adb)
        assertEquals(File(sdk, "emulator/emulator.exe").path, AndroidDevices(sdk, tools, isWindows = true).emulator)
    }

    private companion object {
        val DEVICES = """
            * daemon not running; starting now at tcp:5037
            * daemon started successfully
            List of devices attached
            emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1
            R58M12345              device usb:1-1 product:o1s model:SM_G991B device:o1s transport_id:2
            0A071FDD4003TV         unauthorized usb:1-2 transport_id:3

        """.trimIndent()
    }
}
