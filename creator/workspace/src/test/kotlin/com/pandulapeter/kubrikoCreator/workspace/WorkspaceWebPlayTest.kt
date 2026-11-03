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
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.testing.testAndroidVersions
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.File
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkspaceWebPlayTest {
    private val directory = ProjectGenerator(testConfiguration(), wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = testDirectory()))
    private val targets = GameTargets(directory, testAndroidVersions)
    private val build = FakeProjectBuild()
    private val opened = mutableListOf<URI>()
    private val scope = CoroutineScope(Dispatchers.Default + Job())

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
        openBrowser = { address ->
            opened += address
            true
        },
    )

    @Test
    fun aProjectBuiltForTheWebPlaysInTheBrowserUntilItIsStopped() = blockingTest {
        File(directory, "${WebPlayer.DISTRIBUTION_PATH}/index.html").apply { parentFile.mkdirs() }.writeText("<body></body>")
        val controller = controller()
        val available = controller.playTargets.targets.first { PlayTarget.WEB in it.available }
        assertEquals(listOf(PlayTarget.DESKTOP, PlayTarget.WEB), available.available)
        controller.play(firstScene = null, target = PlayTarget.WEB)
        while (build.runs.isEmpty()) delay(10)
        assertEquals(":${WebPlayer.DISTRIBUTION_TASK_NAME}", build.currentRun.taskPath)
        build.currentRun.finish(isSuccessful = true)
        assertEquals(SessionState.Running(SessionMode.PLAY, PlayTarget.WEB), controller.sessionState.first { it is SessionState.Running })
        assertTrue(opened.single().toString().startsWith("http://127.0.0.1:"))
        controller.stopSession()
        controller.sessionState.first { it == SessionState.Idle }
    }

    @Test
    fun theBrowserIsListedOnlyWhileTheProjectIsBuiltForTheWeb() = blockingTest {
        val change = assertIs<TargetChange.Proposed>(targets.change(GameTarget.WEB, isOn = false))
        assertTrue(targets.apply(change))
        assertEquals(listOf(PlayTarget.DESKTOP), controller().playTargets.refresh().available)
    }
}
