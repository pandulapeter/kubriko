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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidPlayerTest {
    private val root = testDirectory()
    private val sdk = testDirectory()
    private val tools = FakeToolProcesses()
    private val console = ConsoleLog()
    private val builds = mutableListOf<String>()
    private var isBuildSuccessful = true
    private var hasSdk = true
    private val phone = PlayTarget(id = "android:R58M12345", kind = PlayTarget.Kind.ANDROID, name = "Pixel 7")
    private val emulator = AndroidDevices.emulatorTarget("Medium_Phone")
    private val apk = File(root, "${AndroidApp.OUTPUT_PATH}/androidApp-debug.apk")

    private val statuses get() = console.entries.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    private fun player(runTask: suspend (String) -> Boolean = { taskPath -> builds += taskPath; isBuildSuccessful }) = AndroidPlayer(
        rootDirectory = root,
        devices = { if (hasSdk) AndroidDevices(sdk, tools, isWindows = false) else null },
        runTask = runTask,
        tools = tools,
        console = console,
        ioDispatcher = Dispatchers.IO,
        pollIntervalInMilliseconds = 1,
        bootTimeoutInMilliseconds = 2_000,
    )

    private fun built() {
        apk.apply { parentFile.mkdirs() }.writeText("apk")
        File(apk.parentFile, "output-metadata.json").writeText("""{ "applicationId": "com.example.mygame", "elements": [{ "outputFile": "androidApp-debug.apk" }] }""")
    }

    private fun startsOn(serial: String, scene: String = "") {
        tools.answer("-s $serial shell am start -W -n com.example.mygame/.MainActivity --ez kubriko.creator.logging true$scene", ok("Starting: Intent { cmp=com.example.mygame/.MainActivity }\nStatus: ok\n"))
        tools.answer("-s $serial install -r ${apk.path}", ok("Performing Streamed Install\nSuccess\n"))
        tools.answer("-s $serial shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER com.example.mygame", ok("priority=0 preferredOrder=0\ncom.example.mygame/.MainActivity\n"))
        tools.answer("-s $serial shell pidof com.example.mygame", ok("4321\n"), ok("4321\n"), failed(""))
        tools.answer("-s $serial get-state", ok("device\n"))
    }

    private fun connected(vararg lines: String) = tools.answer("devices -l", ok((listOf("List of devices attached") + lines).joinToString("\n")))

    @Test
    fun theGameIsBuiltInstalledStartedAndLoggedOnADeviceUntilItEnds() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345", scene = " --es kubriko.creator.scene 'levels/it'\\''s 1'")
        tools.stream("-s R58M12345 logcat -v tag --pid=4321", listOf("I/System.out: [Ball] Hit a brick", "W/HWUI: noise"))
        var isUp = false
        assertTrue(player().play(phone, firstScene = "levels/it's 1") { isUp = true })
        assertTrue(isUp)
        assertEquals(listOf(AndroidPlayer.BUILD_TASK_PATH), builds)
        assertTrue(tools.ran("-s R58M12345 shell am force-stop com.example.mygame"))
        assertTrue(
            tools.ran("-s R58M12345 shell am start -W -n com.example.mygame/.MainActivity --ez kubriko.creator.logging true --es kubriko.creator.scene 'levels/it'\\''s 1'"),
            tools.commands.joinToString("\n"),
        )
        assertContains(console.entries.value, ConsoleEntry.Log(message = "Hit a brick", source = "Ball", isImportant = true))
        assertEquals(listOf(WorkspaceStatus.ANDROID_INSTALLING, WorkspaceStatus.ANDROID_GAME_STARTED), statuses)
    }

    @Test
    fun stoppingTheGameStopsTheApp() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.answer("-s R58M12345 shell pidof com.example.mygame", ok("4321\n"))
        val isUp = CompletableDeferred<Unit>()
        val playing = launch { player().play(phone, firstScene = null) { isUp.complete(Unit) } }
        withTimeout(5_000) { isUp.await() }
        val stopsBefore = tools.commands.count { it.drop(1).joinToString(" ") == "-s R58M12345 shell am force-stop com.example.mygame" }
        playing.cancelAndJoin()
        assertEquals(stopsBefore + 1, tools.commands.count { it.drop(1).joinToString(" ") == "-s R58M12345 shell am force-stop com.example.mygame" })
        assertFalse(tools.commands.any { "--es" in it })
    }

    @Test
    fun aCheckThatFailsForAMomentLeavesTheGamePlaying() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.answer(
            "-s R58M12345 shell pidof com.example.mygame",
            ok("4321\n"),
            ok("4321\n"),
            ToolResult(exitCode = null, output = ""),
            failed("error: closed"),
            ok("4321\n"),
            failed(""),
        )
        assertTrue(player().play(phone, firstScene = null) {})
        assertEquals(6, tools.commands.count { it.drop(1).joinToString(" ") == "-s R58M12345 shell pidof com.example.mygame" })
        assertEquals(listOf(WorkspaceStatus.ANDROID_INSTALLING, WorkspaceStatus.ANDROID_GAME_STARTED), statuses)
    }

    @Test
    fun stoppingTheGameWhileItStartsStopsTheApp() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        val isStarting = CompletableDeferred<Unit>()
        tools.onRun = { command ->
            if ("start" in command) {
                isStarting.complete(Unit)
                awaitCancellation()
            }
        }
        val playing = launch { player().play(phone, firstScene = null) {} }
        withTimeout(5_000) { isStarting.await() }
        playing.cancelAndJoin()
        assertEquals("force-stop", tools.commands.last()[tools.commands.last().size - 2])
    }

    @Test
    fun anAppThatEndsBeforeItsProcessIsFoundSaysSo() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.answer("-s R58M12345 shell pidof com.example.mygame", failed(""))
        assertFalse(player().play(phone, firstScene = null) {})
        assertEquals(WorkspaceStatus.ANDROID_GAME_ENDED_AT_ONCE, statuses.last())
    }

    @Test
    fun aDeviceThatIsntThereStopsTheBuild() = blockingTest {
        connected()
        val buildCancelled = CompletableDeferred<Unit>()
        val result = withTimeout(5_000) {
            player(
                runTask = {
                    try {
                        awaitCancellation()
                    } finally {
                        buildCancelled.complete(Unit)
                    }
                },
            ).play(phone, firstScene = null) {}
        }
        assertFalse(result)
        assertTrue(buildCancelled.isCompleted)
        assertEquals(listOf(WorkspaceStatus.ANDROID_DEVICE_NOT_CONNECTED), statuses)
    }

    @Test
    fun aDeviceThatLeavesEndsTheGameWithTheReason() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.answer("-s R58M12345 get-state", failed("error: device 'R58M12345' not found"))
        assertFalse(player().play(phone, firstScene = null) {})
        assertEquals(WorkspaceStatus.ANDROID_DEVICE_LEFT, statuses.last())
    }

    @Test
    fun aCrashEndsTheGameWithAnError() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.stream("-s R58M12345 logcat -v tag --pid=4321", listOf("E/AndroidRuntime: FATAL EXCEPTION: main", "E/AndroidRuntime: java.lang.IllegalStateException: Boom"))
        assertFalse(player().play(phone, firstScene = null) {})
        assertContains(console.entries.value, ConsoleEntry.BuildOutput("java.lang.IllegalStateException: Boom"))
        assertEquals(WorkspaceStatus.ANDROID_GAME_CRASHED, statuses.last())
    }

    @Test
    fun aDeviceThatIsntReadySaysWhatToDoAndNothingIsInstalled() = blockingTest {
        built()
        connected("R58M12345 unauthorized usb:1-1 transport_id:2", "0A071FDD4003TV offline transport_id:3")
        assertFalse(player().play(phone, firstScene = null) {})
        assertFalse(player().play(phone.copy(id = "android:0A071FDD4003TV"), firstScene = null) {})
        assertFalse(player().play(phone.copy(id = "android:gone"), firstScene = null) {})
        assertEquals(
            listOf(WorkspaceStatus.ANDROID_DEVICE_UNAUTHORIZED, WorkspaceStatus.ANDROID_DEVICE_OFFLINE, WorkspaceStatus.ANDROID_DEVICE_NOT_CONNECTED),
            statuses,
        )
        assertFalse(tools.commands.any { "install" in it })
    }

    @Test
    fun anEmulatorThatIsntRunningIsStartedAndWaitedForWhileTheGameBuilds() = blockingTest {
        built()
        connected()
        tools.onDetached = {
            connected("emulator-5554 offline transport_id:1")
            tools.answer("-s emulator-5554 emu avd name", ok("Medium_Phone\nOK\n"))
            tools.answer("-s emulator-5554 shell getprop sys.boot_completed", ok("\n"), ok("1\n"))
            launch {
                delay(20)
                connected("emulator-5554 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 transport_id:1")
            }
        }
        startsOn("emulator-5554")
        val buildStarted = CompletableDeferred<Unit>()
        val finishBuild = CompletableDeferred<Boolean>()
        val playing = async {
            player(runTask = { buildStarted.complete(Unit); finishBuild.await() }).play(emulator, firstScene = null) {}
        }
        withTimeout(5_000) { buildStarted.await() }
        withTimeout(5_000) { while (!tools.ran("-avd Medium_Phone")) delay(1) }
        finishBuild.complete(true)
        assertTrue(playing.await())
        assertTrue(tools.ran("-s emulator-5554 install -r ${apk.path}"))
        assertEquals(WorkspaceStatus.ANDROID_EMULATOR_STARTING, statuses.first())
    }

    @Test
    fun aRunningEmulatorIsUsedAsItIs() = blockingTest {
        built()
        connected("emulator-5556 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 transport_id:1")
        tools.answer("-s emulator-5556 emu avd name", ok("Medium_Phone\nOK\n"))
        tools.answer("-s emulator-5556 shell getprop sys.boot_completed", ok("1\n"))
        startsOn("emulator-5556")
        assertTrue(player().play(emulator, firstScene = null) {})
        assertFalse(tools.commands.any { "-avd" in it })
    }

    @Test
    fun anEmulatorThatEndsOrNeverBootsSaysSo() = blockingTest {
        built()
        connected()
        tools.detached = FakeToolProcesses.FakeDetachedTool(exitCode = 1, output = listOf("FATAL | No AVD named Medium_Phone"))
        assertFalse(player().play(emulator, firstScene = null) {})
        assertContains(console.entries.value, ConsoleEntry.BuildOutput("FATAL | No AVD named Medium_Phone"))
        tools.detached = FakeToolProcesses.FakeDetachedTool()
        assertFalse(player().play(emulator, firstScene = null) {})
        assertEquals(
            listOf(WorkspaceStatus.ANDROID_EMULATOR_STARTING, WorkspaceStatus.ANDROID_EMULATOR_FAILED, WorkspaceStatus.ANDROID_EMULATOR_STARTING, WorkspaceStatus.ANDROID_EMULATOR_TIMED_OUT),
            statuses,
        )
    }

    @Test
    fun aRefusedInstallSaysWhy() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.answer("-s R58M12345 install -r ${apk.path}", failed("Performing Streamed Install\nadb: failed to install: Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]\n"))
        assertFalse(player().play(phone, firstScene = null) {})
        tools.answer("-s R58M12345 install -r ${apk.path}", failed("Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package signatures do not match]"))
        assertFalse(player().play(phone, firstScene = null) {})
        val failures = console.entries.value.filterIsInstance<ConsoleEntry.Status>().filter { it.isError }
        assertEquals(listOf(WorkspaceStatus.ANDROID_INSTALL_FAILED, WorkspaceStatus.ANDROID_INSTALL_INCOMPATIBLE), failures.map { it.text })
        assertEquals("adb: failed to install: Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]", failures.first().arguments.last())
    }

    @Test
    fun anAppThatDoesntStartSaysWhy() = blockingTest {
        built()
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        startsOn("R58M12345")
        tools.answer(
            "-s R58M12345 shell am start -W -n com.example.mygame/.MainActivity --ez kubriko.creator.logging true",
            ok("Starting: Intent { cmp=com.example.mygame/.MainActivity }\nError type 3\nError: Activity class does not exist.\n"),
        )
        assertFalse(player().play(phone, firstScene = null) {})
        tools.answer("-s R58M12345 shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER com.example.mygame", ok("No activity found\n"))
        assertFalse(player().play(phone, firstScene = null) {})
        assertEquals(listOf(WorkspaceStatus.ANDROID_NOT_STARTED), statuses.filter { it != WorkspaceStatus.ANDROID_INSTALLING }.distinct())
    }

    @Test
    fun nothingIsPlayedWithoutTheSdkABuildOrAnApp() = blockingTest {
        hasSdk = false
        assertFalse(player().play(phone, firstScene = null) {})
        hasSdk = true
        connected("R58M12345 device usb:1-1 model:Pixel_7")
        isBuildSuccessful = false
        assertFalse(player().play(phone, firstScene = null) {})
        isBuildSuccessful = true
        assertFalse(player().play(phone, firstScene = null) {})
        assertEquals(listOf(WorkspaceStatus.ANDROID_SDK_MISSING, WorkspaceStatus.ANDROID_APP_MISSING), statuses)
        assertFalse(tools.commands.any { "install" in it })
    }

    @Test
    fun aFailedBuildStopsWaitingForTheEmulator() = blockingTest {
        built()
        connected()
        isBuildSuccessful = false
        val result = withTimeout(1_000) { player().play(emulator, firstScene = null) {} }
        assertFalse(result)
    }
}
