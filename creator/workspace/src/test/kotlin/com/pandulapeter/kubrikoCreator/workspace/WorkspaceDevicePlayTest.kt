/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.template.TargetHost
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.testing.testAndroidVersions
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.workspace.FakeToolProcesses.Companion.ok
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkspaceDevicePlayTest {
    private val directory = ProjectGenerator(testConfiguration(), wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = testDirectory()))
    private val sdk = testDirectory().also { File(it, "platforms").mkdirs() }
    private val targets = GameTargets(
        directory,
        testAndroidVersions,
        object : TargetHost {
            override fun androidSdk(rootDirectory: File) = sdk
            override val canBuildIos = true
        },
    )
    private val build = FakeProjectBuild()
    private val tools = FakeToolProcesses()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val phone = PlayTarget(id = "android:R58M12345", kind = PlayTarget.Kind.ANDROID, name = "Pixel 7")

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    private fun controller() = workspaceController(
        project = CreatorProject(rootDirectory = directory, name = "My Game"),
        parentScope = scope,
        ioDispatcher = Dispatchers.IO,
        build = build,
        targets = targets,
        tools = tools,
    )

    private fun turnOn(target: GameTarget) = assertTrue(targets.apply(assertIs<TargetChange.Proposed>(targets.change(target, isOn = true))))

    @Test
    fun aProjectBuiltForAndroidListsItsDevicesAndPlaysOnOneUntilItIsStopped() = blockingTest {
        turnOn(GameTarget.ANDROID)
        tools.answer("devices -l", ok("List of devices attached\nR58M12345 device usb:1-1 model:Pixel_7\n"))
        val apk = File(directory, "${AndroidApp.OUTPUT_PATH}/androidApp-debug.apk").apply { parentFile.mkdirs() }.also { it.writeText("apk") }
        File(apk.parentFile, "output-metadata.json").writeText("""{ "applicationId": "com.example.mygame", "elements": [{ "outputFile": "androidApp-debug.apk" }] }""")
        tools.answer("-s R58M12345 install -r ${apk.path}", ok("Success\n"))
        tools.answer("-s R58M12345 shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER com.example.mygame", ok("com.example.mygame/.MainActivity\n"))
        tools.answer("-s R58M12345 shell am start -W -n com.example.mygame/.MainActivity --ez kubriko.creator.logging true", ok("Status: ok\n"))
        tools.answer("-s R58M12345 shell pidof com.example.mygame", ok("4321\n"))
        val controller = controller()
        assertEquals(listOf(PlayTarget.DESKTOP, PlayTarget.WEB, phone), controller.playTargets.refresh().available)
        controller.play(firstScene = null, target = phone)
        while (build.runs.isEmpty()) delay(10)
        assertEquals(AndroidPlayer.BUILD_TASK_PATH, build.currentRun.taskPath)
        build.currentRun.finish(isSuccessful = true)
        assertEquals(SessionState.Running(SessionMode.PLAY, phone), controller.sessionState.first { it is SessionState.Running })
        controller.stopSession()
        controller.sessionState.first { it == SessionState.Idle }
        assertTrue(tools.commands.count { it.drop(1).joinToString(" ") == "-s R58M12345 shell am force-stop com.example.mygame" } == 2)
    }

    @Test
    fun androidsDevicesAreListedOnlyWhileTheProjectIsBuiltForAndroid() = blockingTest {
        tools.answer("devices -l", ok("List of devices attached\nR58M12345 device usb:1-1 model:Pixel_7\n"))
        assertEquals(listOf(PlayTarget.DESKTOP, PlayTarget.WEB), controller().playTargets.refresh().available)
        assertTrue(tools.commands.isEmpty())
    }

    @Test
    fun iosSimulatorsAreListedOnlyWhileTheProjectIsBuiltForIos() = blockingTest {
        tools.answer(
            "simctl list devices available --json",
            ok("""{ "devices": { "com.apple.CoreSimulator.SimRuntime.iOS-18-2": [{ "udid": "U1", "isAvailable": true, "state": "Shutdown", "name": "iPhone 16" }] } }"""),
        )
        val controller = controller()
        assertEquals(listOf(PlayTarget.DESKTOP, PlayTarget.WEB), controller.playTargets.refresh().available)
        turnOn(GameTarget.IOS)
        assertEquals(
            listOf(PlayTarget.DESKTOP, PlayTarget.WEB, PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS, name = "iPhone 16 (iOS 18.2)")),
            controller.playTargets.refresh().available,
        )
    }
}
