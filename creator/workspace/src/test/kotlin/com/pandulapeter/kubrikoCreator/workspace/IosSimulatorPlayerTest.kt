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
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.failed
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.ok
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosSimulatorPlayerTest {
    private val root = testDirectory()
    private val tools = FakeToolProcesses()
    private val console = ConsoleLog()
    private var canBuildIos = true
    private val target = PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS, name = "iPhone 16 (iOS 18.2)")
    private val app = File(root, "${IosSimulatorPlayer.BUILD_PATH}/${IosSimulatorPlayer.PRODUCTS_PATH}/Game.app")
    private val build = "xcodebuild -project ${File(root, IosSimulatorPlayer.PROJECT_PATH).path} -scheme iosApp -configuration Debug -sdk iphonesimulator " +
        "-destination id=U1 -derivedDataPath ${File(root, IosSimulatorPlayer.BUILD_PATH).path} CODE_SIGNING_ALLOWED=NO build"
    private val launch = "simctl launch --console-pty --terminate-running-process U1 com.example.mygame -kubriko.creator.logging YES"

    private val statuses get() = console.entries.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    private fun player() = IosSimulatorPlayer(
        rootDirectory = root,
        simulators = { if (canBuildIos) IosSimulators(tools) else null },
        tools = tools,
        console = console,
        ioDispatcher = Dispatchers.IO,
        bootTimeoutInMilliseconds = 60_000,
    )

    private fun simulator(state: String) = ok("""{ "devices": { "com.apple.CoreSimulator.SimRuntime.iOS-18-2": [{ "udid": "U1", "isAvailable": true, "state": "$state", "name": "iPhone 16" }] } }""")

    private fun builds() {
        tools.stream(build, listOf("** BUILD SUCCEEDED **"), keepsRunning = false)
        app.mkdirs()
        tools.answer("-extract CFBundleIdentifier raw -o - ${File(app, "Info.plist").path}", ok("com.example.mygame\n"))
        tools.answer("simctl install U1 ${app.path}", ok(""))
    }

    @Test
    fun aShutDownSimulatorBootsWhileTheGameBuildsThenItIsInstalledStartedAndShownUntilItEnds() = blockingTest {
        tools.answer("simctl list devices available --json", simulator("Shutdown"), simulator("Booted"))
        tools.answer("simctl bootstatus U1 -b", ok("Device already booted, nothing to do.\n"))
        builds()
        tools.stream("$launch -kubriko.creator.scene levels/it's 1", listOf("2026-11-03 20:01:54.789 Game[4321:5460775] early\r", "com.example.mygame: 4321\r", "[Ball] Hit a brick\r", "plain", "com.example.mygame: 99"), keepsRunning = false)
        var isUp = false
        assertTrue(player().play(target, firstScene = "levels/it's 1") { isUp = true })
        assertTrue(isUp)
        assertTrue(tools.ran("-a Simulator --args -CurrentDeviceUDID U1"))
        assertContains(console.entries.value, ConsoleEntry.Log(message = "Hit a brick", source = "Ball", isImportant = true))
        assertContains(console.entries.value, ConsoleEntry.Log(message = "plain", source = null, isImportant = false))
        assertEquals(
            listOf("2026-11-03 20:01:54.789 Game[4321:5460775] early", "Hit a brick", "plain", "com.example.mygame: 99"),
            console.entries.value.filterIsInstance<ConsoleEntry.Log>().map { it.message },
        )
        assertEquals(setOf(WorkspaceStatus.IOS_SIMULATOR_BOOTING, WorkspaceStatus.BUILDING), statuses.take(2).toSet())
        assertEquals(listOf(WorkspaceStatus.IOS_GAME_STARTED), statuses.drop(2))
    }

    @Test
    fun aBootedSimulatorIsUsedAsItIs() = blockingTest {
        tools.answer("simctl list devices available --json", simulator("Booted"))
        builds()
        tools.stream(launch, listOf("com.example.mygame: 4321"), keepsRunning = false)
        assertTrue(player().play(target, firstScene = null) {})
        assertFalse(tools.ran("simctl bootstatus U1 -b"))
    }

    @Test
    fun stoppingTheGameEndsTheApp() = blockingTest {
        tools.answer("simctl list devices available --json", simulator("Booted"))
        builds()
        tools.stream(launch, listOf("com.example.mygame: 4321"))
        val isUp = CompletableDeferred<Unit>()
        val playing = launch { player().play(target, firstScene = null) { isUp.complete(Unit) } }
        withTimeout(5_000) { isUp.await() }
        playing.cancelAndJoin()
        assertEquals(listOf("xcrun", "simctl", "terminate", "U1", "com.example.mygame"), tools.commands.last())
    }

    @Test
    fun aFailedBuildShowsItsErrorsAndNothingIsInstalled() = blockingTest {
        tools.answer("simctl list devices available --json", simulator("Booted"))
        tools.stream(build, listOf("noise", "/game/Game.kt:3: error: Unresolved reference", "** BUILD FAILED **"), keepsRunning = false, exitCode = 65)
        assertFalse(player().play(target, firstScene = null) {})
        assertEquals(WorkspaceStatus.BUILD_FAILED, statuses.last())
        assertEquals(listOf(ConsoleEntry.BuildOutput("/game/Game.kt:3: error: Unresolved reference")), console.entries.value.filterIsInstance<ConsoleEntry.BuildOutput>())
        assertFalse(tools.commands.any { "install" in it })
    }

    @Test
    fun aSimulatorThatIsGoneOrDoesntBootSaysSoAndStopsTheBuild() = blockingTest {
        val buildCancelled = CompletableDeferred<Unit>()
        tools.answer("simctl list devices available --json", ok("""{ "devices": {} }"""))
        val slowBuild = object : ToolProcesses by tools {
            override suspend fun stream(command: List<String>, onLine: (String) -> Unit): Int? {
                if ("xcodebuild" !in command) return tools.stream(command, onLine)
                try {
                    awaitCancellation()
                } finally {
                    buildCancelled.complete(Unit)
                }
            }
        }
        val player = IosSimulatorPlayer(root, { IosSimulators(slowBuild) }, slowBuild, console, Dispatchers.IO)
        assertFalse(withTimeout(5_000) { player.play(target, firstScene = null) {} })
        assertTrue(buildCancelled.isCompleted)
        tools.answer("simctl list devices available --json", simulator("Shutdown"))
        tools.answer("simctl bootstatus U1 -b", ToolResult(exitCode = null, output = ""), failed("Unable to boot the Simulator.\n"))
        assertFalse(withTimeout(5_000) { player.play(target, firstScene = null) {} })
        assertFalse(withTimeout(5_000) { player.play(target, firstScene = null) {} })
        assertEquals(
            listOf(
                WorkspaceStatus.IOS_SIMULATOR_MISSING,
                WorkspaceStatus.IOS_SIMULATOR_BOOTING,
                WorkspaceStatus.IOS_SIMULATOR_TIMED_OUT,
                WorkspaceStatus.IOS_SIMULATOR_BOOTING,
                WorkspaceStatus.IOS_SIMULATOR_NOT_BOOTED,
            ),
            statuses.filter { it != WorkspaceStatus.BUILDING },
        )
        assertEquals(listOf("iPhone 16 (iOS 18.2)", "Unable to boot the Simulator."), console.entries.value.filterIsInstance<ConsoleEntry.Status>().last().arguments)
    }

    @Test
    fun aMissingAppARefusedInstallOrAnAppThatDoesntStartSaysWhy() = blockingTest {
        tools.answer("simctl list devices available --json", simulator("Booted"))
        tools.stream(build, emptyList(), keepsRunning = false)
        assertFalse(player().play(target, firstScene = null) {})
        builds()
        tools.answer("simctl install U1 ${app.path}", failed("An error was encountered processing the command (code=22):\nFailed to install the app\n"))
        assertFalse(player().play(target, firstScene = null) {})
        tools.answer("simctl install U1 ${app.path}", ok(""))
        tools.stream(launch, listOf("An error was encountered processing the command (code=4):", "The request was denied by service delegate"), keepsRunning = false)
        assertFalse(player().play(target, firstScene = null) {})
        assertEquals(
            listOf(WorkspaceStatus.IOS_APP_MISSING, WorkspaceStatus.IOS_INSTALL_FAILED, WorkspaceStatus.IOS_NOT_STARTED),
            console.entries.value.filterIsInstance<ConsoleEntry.Status>().filter { it.isError }.map { it.text },
        )
        assertEquals("The request was denied by service delegate", console.entries.value.filterIsInstance<ConsoleEntry.Status>().last().arguments.last())
    }

    @Test
    fun aCrashOrASimulatorThatShutsDownEndsTheGameWithAnError() = blockingTest {
        tools.answer("simctl list devices available --json", simulator("Booted"))
        builds()
        tools.stream(launch, listOf("com.example.mygame: 4321", "Uncaught Kotlin exception: kotlin.IllegalStateException: Boom"), keepsRunning = false)
        assertFalse(player().play(target, firstScene = null) {})
        tools.answer("simctl list devices available --json", simulator("Booted"), simulator("Shutdown"))
        tools.stream(launch, listOf("com.example.mygame: 4321"), keepsRunning = false)
        assertFalse(player().play(target, firstScene = null) {})
        assertContains(console.entries.value, ConsoleEntry.BuildOutput("Uncaught Kotlin exception: kotlin.IllegalStateException: Boom"))
        assertEquals(
            listOf(WorkspaceStatus.IOS_GAME_CRASHED, WorkspaceStatus.IOS_SIMULATOR_SHUT_DOWN),
            console.entries.value.filterIsInstance<ConsoleEntry.Status>().filter { it.isError }.map { it.text },
        )
    }

    @Test
    fun withoutXcodeNothingIsPlayed() = blockingTest {
        canBuildIos = false
        assertFalse(player().play(target, firstScene = null) {})
        assertEquals(listOf(WorkspaceStatus.IOS_XCODE_MISSING), statuses)
        assertTrue(tools.commands.isEmpty())
    }
}
