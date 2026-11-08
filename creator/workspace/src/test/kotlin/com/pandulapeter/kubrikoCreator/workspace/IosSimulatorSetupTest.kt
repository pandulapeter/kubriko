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
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IosSimulatorSetupTest {
    private val tools = FakeToolProcesses()
    private val chosen = mutableListOf<PlayTarget>()
    private val setup = IosSimulatorSetup(tools, Dispatchers.IO) { chosen += it }

    private fun type(identifier: String, name: String) = IosDeviceType("com.apple.CoreSimulator.SimDeviceType.$identifier", name)

    private val iPhone18Pro = type("iPhone-18-Pro", "iPhone 18 Pro")
    private val iPhoneDuo = type("iPhone-Duo", "iPhone Duo")
    private val ios272 = "com.apple.CoreSimulator.SimRuntime.iOS-27-2"

    private fun answerLists(devices: String = """{ "devices": {} }""") {
        tools.answer("simctl list devicetypes --json", ok(SimctlRecordings.DEVICE_TYPES))
        tools.answer("simctl list runtimes --json", ok(SimctlRecordings.RUNTIMES))
        tools.answer("simctl list devices available --json", ok(devices))
    }

    private suspend fun ready() = assertIs<IosSimulatorOptions.Ready>(setup.options())

    @Test
    fun onlyIphonesAndIpadsSomeInstalledIosVersionRunsAreOfferedNewestFirst() = blockingTest {
        answerLists()
        val options = ready()
        assertEquals(
            listOf("iPhone Duo", "iPhone 18 Pro", "iPhone 17e", "iPhone 16 Pro", "iPad Pro 13-inch (M5)", "iPad (A16)", "iPad mini (6th generation)"),
            options.deviceTypes.map { it.name },
        )
        assertEquals(
            listOf(ios272, "$ios272-iPhone19-4", "com.apple.CoreSimulator.SimRuntime.iOS-27-1", "com.apple.CoreSimulator.SimRuntime.iOS-18-2"),
            options.runtimes.map { it.identifier },
        )
    }

    @Test
    fun eachDeviceTypeOffersTheVersionsThatRunItAndANewSimulatorStartsAsTheNewestThatRunsMore() = blockingTest {
        answerLists()
        val options = ready()
        assertEquals(iPhone18Pro, options.suggestedDeviceType)
        assertEquals(listOf("iOS 27.2"), options.runtimesFor(iPhone18Pro).map { it.name })
        assertEquals(listOf("$ios272-iPhone19-4", "com.apple.CoreSimulator.SimRuntime.iOS-27-1"), options.runtimesFor(iPhoneDuo).map { it.identifier })
        assertEquals(listOf("iOS 27.2", "iOS 18.2"), options.runtimesFor(type("iPad-mini-6th-generation", "iPad mini (6th generation)")).map { it.name })
    }

    @Test
    fun twoBuildsOfOneVersionAreToldApartByTheirBuild() = blockingTest {
        answerLists()
        val runtimes = ready().runtimes
        assertEquals(listOf("iOS 27.2 (24B8117)", "iOS 27.2 (24B91016s)", "iOS 27.1", "iOS 18.2"), runtimes.map { it.labelAmong(runtimes) })
    }

    @Test
    fun aNameAnySimulatorHasIsTakenWhateverItsCaseAndTheSuggestedOneAvoidsIt() = blockingTest {
        answerLists(
            devices = """{ "devices": { "com.apple.CoreSimulator.SimRuntime.iOS-27-2": [
                { "udid": "U1", "isAvailable": true, "state": "Shutdown", "name": "iPhone 18 Pro" },
                { "udid": "U2", "isAvailable": true, "state": "Shutdown", "name": "iPhone 18 Pro 2" }
            ] } }""",
        )
        val options = ready()
        assertTrue(options.isNameTaken(" iphone 18 PRO "))
        assertFalse(options.isNameTaken("iPhone 18"))
        assertEquals("iPhone 18 Pro 3", options.suggestedName(iPhone18Pro))
        assertEquals("iPhone Duo", options.suggestedName(iPhoneDuo))
    }

    @Test
    fun aMacWithoutAnIosRuntimeHasNoOptions() = blockingTest {
        tools.answer("simctl list devicetypes --json", ok(SimctlRecordings.DEVICE_TYPES))
        tools.answer("simctl list runtimes --json", ok("{\n  \"runtimes\" : [\n\n  ]\n}\n"))
        tools.answer("simctl list devices available --json", ok("""{ "devices": {} }"""))
        assertEquals(IosSimulatorOptions.NoRuntime, setup.options())
    }

    @Test
    fun runtimesThatAreUnavailableOrForAnotherPlatformCountAsNone() = blockingTest {
        val ipad = """{ "name" : "iPad (A16)", "productFamily" : "iPad", "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-A16" }"""
        tools.answer("simctl list devicetypes --json", ok(SimctlRecordings.DEVICE_TYPES))
        tools.answer(
            "simctl list runtimes --json",
            ok(
                """{ "runtimes" : [
                    { "isAvailable" : false, "availabilityError" : "The runtime's disk image is missing.", "version" : "27.2", "buildversion" : "24B8117",
                      "supportedDeviceTypes" : [$ipad], "identifier" : "com.apple.CoreSimulator.SimRuntime.iOS-27-2", "platform" : "iOS", "name" : "iOS 27.2" },
                    { "isAvailable" : true, "version" : "27.2", "buildversion" : "24K100",
                      "supportedDeviceTypes" : [$ipad], "identifier" : "com.apple.CoreSimulator.SimRuntime.tvOS-27-2", "platform" : "tvOS", "name" : "tvOS 27.2" }
                ] }""",
            ),
        )
        tools.answer("simctl list devices available --json", ok("""{ "devices": {} }"""))
        assertEquals(IosSimulatorOptions.NoRuntime, setup.options())
    }

    @Test
    fun anIpodTouchIsNoIphoneEvenThoughItSaysSo() = blockingTest {
        tools.answer("simctl list devicetypes --json", ok(SimctlRecordings.DEVICE_TYPES))
        tools.answer(
            "simctl list runtimes --json",
            ok(
                """{ "runtimes" : [ { "isAvailable" : true, "version" : "15.5", "buildversion" : "19F70", "identifier" : "com.apple.CoreSimulator.SimRuntime.iOS-15-5",
                    "platform" : "iOS", "name" : "iOS 15.5", "supportedDeviceTypes" : [
                      { "name" : "iPod touch (7th generation)", "productFamily" : "iPhone", "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-" },
                      { "name" : "iPad mini (6th generation)", "productFamily" : "iPad", "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation" }
                    ] } ] }""",
            ),
        )
        tools.answer("simctl list devices available --json", ok("""{ "devices": {} }"""))
        assertEquals(listOf("iPad mini (6th generation)"), ready().deviceTypes.map { it.name })
    }

    @Test
    fun aListSimctlCanNotGiveSaysWhyAndOneItCanNotReadSaysNothing() = blockingTest {
        tools.answer("simctl list devicetypes --json", failed("xcrun: error: unable to find utility \"simctl\", not a developer tool or in PATH\n"))
        assertEquals(IosSimulatorOptions.Unreadable("xcrun: error: unable to find utility \"simctl\", not a developer tool or in PATH"), setup.options())
        answerLists()
        tools.answer("simctl list runtimes --json", ok("Unable to locate CoreSimulator.framework"))
        assertEquals(IosSimulatorOptions.Unreadable(reason = null), setup.options())
    }

    @Test
    fun aCreatedSimulatorIsChosenUnderItsTrimmedNameAndVersion() = blockingTest {
        answerLists()
        val runtime = ready().runtimes.first()
        tools.answer("simctl create My Phone com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro $ios272", ok(SimctlRecordings.CREATED))
        val created = assertIs<IosSimulatorCreation.Created>(setup.create("  My Phone ", iPhone18Pro, runtime))
        val target = PlayTarget(id = "ios-simulator:6F1E2C3A-9B8D-4E5F-A1B2-C3D4E5F60718", kind = PlayTarget.Kind.IOS, name = "My Phone (iOS 27.2)", isVirtual = true)
        assertEquals(target, created.target)
        assertEquals(listOf(target), chosen)
        assertEquals(listOf("xcrun", "simctl", "create", "My Phone", iPhone18Pro.identifier, ios272), tools.commands.last())
    }

    @Test
    fun aSimulatorIsFoundByItsUdidWhateverWarningsFollowIt() = blockingTest {
        answerLists()
        val runtime = ready().runtimes.first()
        tools.answer(
            "simctl create My Phone com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro $ios272",
            ok(SimctlRecordings.CREATED + "objc[4321]: Class SimServiceContext is implemented in both CoreSimulator and SimulatorKit.\n"),
        )
        val created = assertIs<IosSimulatorCreation.Created>(setup.create("My Phone", iPhone18Pro, runtime))
        assertEquals("ios-simulator:6F1E2C3A-9B8D-4E5F-A1B2-C3D4E5F60718", created.target.id)
    }

    @Test
    fun aRefusedCreationSaysWhyInSimctlsWordsAndChoosesNothing() = blockingTest {
        answerLists()
        val runtime = ready().runtimes.first()
        tools.answer("simctl create Duo com.apple.CoreSimulator.SimDeviceType.iPhone-Duo $ios272", failed(SimctlRecordings.INCOMPATIBLE))
        assertEquals(IosSimulatorCreation.Refused("Incompatible device"), setup.create("Duo", iPhoneDuo, runtime))
        tools.answer("simctl create Duo com.apple.CoreSimulator.SimDeviceType.iPhone-Duo $ios272", ok("\n"))
        assertIs<IosSimulatorCreation.Refused>(setup.create("Duo", iPhoneDuo, runtime))
        tools.answer("simctl create Duo com.apple.CoreSimulator.SimDeviceType.iPhone-Duo $ios272", ToolResult(exitCode = null, output = "", hasTimedOut = true))
        assertEquals(IosSimulatorCreation.TimedOut, setup.create("Duo", iPhoneDuo, runtime))
        assertTrue(chosen.isEmpty())
    }
}
