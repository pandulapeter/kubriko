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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_actor_type_not_saved
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_session_ended
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import kubriko.creator.generated.resources.status_unsupported_protocol
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceControllerTest {
    private val rootDirectory = File("/projects/game")
    private val scene = ProjectResource(
        group = ResourceGroup.SCENES,
        file = File(rootDirectory, "src/commonMain/composeResources/files/scenes/level.json"),
        relativePath = "src/commonMain/composeResources/files/scenes/level.json",
    )
    private val build = FakeProjectBuild()
    private val channel = FakeSessionChannel()

    private fun TestScope.createController(
        desktopModulePath: String = ":",
        scanResources: (File) -> Map<ResourceGroup, List<ProjectResource>> = { emptyMap() },
        actorTypeRepository: FakeActorTypeRepository? = null,
    ): WorkspaceController {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkspaceController(
            project = CreatorProject(rootDirectory = rootDirectory, name = "game", desktopModulePath = desktopModulePath),
            build = build,
            sessionChannel = channel,
            scope = CoroutineScope(backgroundScope.coroutineContext + dispatcher),
            ioDispatcher = dispatcher,
            scanResources = scanResources,
            actorTypeRepository = actorTypeRepository,
        )
    }

    private val WorkspaceController.statuses get() = console.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    @Test
    fun resourcesAreScannedFromTheProjectRootWhenOpened() = runTest {
        val scannedDirectories = mutableListOf<File>()
        val controller = createController(scanResources = { scannedDirectories += it; mapOf(ResourceGroup.SCENES to listOf(scene)) })
        assertEquals(listOf(rootDirectory), scannedDirectories)
        assertEquals(mapOf(ResourceGroup.SCENES to listOf(scene)), controller.resources.value)
    }

    @Test
    fun refreshingRescansTheResources() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { scanCount++; emptyMap() })
        controller.refreshResources()
        assertEquals(2, scanCount)
    }

    @Test
    fun playRunsTheSessionTaskOfTheRootModuleInPlayMode() = runTest {
        val controller = createController()
        controller.play()
        assertEquals(":${SessionInitScript.TASK_NAME}", build.currentRun.taskPath)
        val arguments = build.currentRun.arguments
        assertEquals(SessionInitScript.file.absolutePath, arguments[arguments.indexOf("--init-script") + 1])
        assertTrue(File(arguments[arguments.indexOf("--init-script") + 1]).isFile)
        assertContains(arguments, "--no-configuration-cache")
        assertContains(arguments, "-PkubrikoCreatorPort=${channel.port}")
        assertContains(arguments, "-PkubrikoCreatorMode=play")
        assertEquals(SessionState.Starting(SessionMode.PLAY), controller.sessionState.value)
        assertEquals(listOf(Res.string.status_building), controller.statuses)
    }

    @Test
    fun theSessionTaskOfANestedModuleIsAddressedByItsPath() = runTest {
        createController(desktopModulePath = ":app:desktop").play()
        assertEquals(":app:desktop:${SessionInitScript.TASK_NAME}", build.currentRun.taskPath)
    }

    @Test
    fun editingASceneRunsTheSessionTaskWithTheScenesFolderAndFile() = runTest {
        val controller = createController()
        controller.editScene(scene)
        val arguments = build.currentRun.arguments
        assertContains(arguments, "-PkubrikoCreatorMode=edit")
        assertContains(arguments, "-PkubrikoCreatorSceneFolder=${scene.file.parentFile.absolutePath}")
        assertContains(arguments, "-PkubrikoCreatorSceneFile=level.json")
        assertEquals(SessionState.Starting(SessionMode.EDIT), controller.sessionState.value)
    }

    @Test
    fun aHelloInTheCurrentProtocolMarksTheSessionAsRunning() = runTest {
        val controller = createController()
        controller.editScene(scene)
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.EDIT))
        assertEquals(SessionState.Running(SessionMode.EDIT), controller.sessionState.value)
        assertEquals(Res.string.status_session_started, controller.statuses.last())
    }

    @Test
    fun aHelloInAnUnknownProtocolStopsTheSession() = runTest {
        val controller = createController()
        controller.play()
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION + 1, mode = SessionMode.PLAY))
        assertTrue(build.currentRun.isCancelled)
        assertEquals(SessionState.Idle, controller.sessionState.value)
        assertContains(controller.console.value, ConsoleEntry.Status(Res.string.status_unsupported_protocol, isError = true))
    }

    @Test
    fun logsOfTheSessionAppearInTheConsole() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Log(id = "1", message = "Started", details = null, source = "Game", timestamp = 0L, importance = "HIGH"))
        channel.receive(SessionMessage.Log(id = "2", message = "Ticking", details = null, source = null, timestamp = 0L, importance = "LOW"))
        assertEquals(
            listOf(
                ConsoleEntry.Log(message = "Started", source = "Game", isImportant = true),
                ConsoleEntry.Log(message = "Ticking", source = null, isImportant = false),
            ),
            controller.console.value,
        )
    }

    @Test
    fun aSessionFailureAppearsInTheConsoleInTheSessionsWords() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Failure(message = "No entry point."))
        assertEquals(listOf(ConsoleEntry.SessionFailure("No entry point.")), controller.console.value)
    }

    @Test
    fun problemsAppearWithTheirLocationRelativeToTheProject() = runTest {
        val controller = createController()
        controller.play()
        build.currentRun.onEvent(GradleBuildEvent.Problem("Unresolved reference", File(rootDirectory, "src/Ball.kt"), line = 3, column = 5))
        build.currentRun.onEvent(GradleBuildEvent.Problem("Somewhere else", file = null, line = null, column = null))
        assertEquals(
            listOf(
                ConsoleEntry.Problem("Unresolved reference", "src/Ball.kt:3:5"),
                ConsoleEntry.Problem("Somewhere else", null),
            ),
            controller.console.value.filterIsInstance<ConsoleEntry.Problem>(),
        )
    }

    @Test
    fun aFailedBuildWithoutProblemsShowsTheTailOfItsOutput() = runTest {
        val controller = createController()
        controller.play()
        repeat(40) { build.currentRun.onEvent(GradleBuildEvent.Output("line $it", isError = it % 2 == 0)) }
        build.currentRun.finish(isSuccessful = false)
        assertEquals(Res.string.status_build_failed, controller.statuses.last())
        assertEquals((10 until 40).map { "line $it" }, controller.console.value.filterIsInstance<ConsoleEntry.BuildOutput>().map { it.line })
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun aFailedBuildWithProblemsShowsOnlyTheProblems() = runTest {
        val controller = createController()
        controller.play()
        build.currentRun.onEvent(GradleBuildEvent.Output("e: noise", isError = true))
        build.currentRun.onEvent(GradleBuildEvent.Problem("Unresolved reference", file = null, line = null, column = null))
        build.currentRun.finish(isSuccessful = false)
        assertTrue(controller.console.value.none { it is ConsoleEntry.BuildOutput })
    }

    @Test
    fun aSessionThatExitsOnItsOwnEndsQuietly() = runTest {
        val controller = createController()
        controller.play()
        build.currentRun.finish(isSuccessful = true)
        assertEquals(listOf(Res.string.status_building, Res.string.status_session_ended), controller.statuses)
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun stoppingCancelsTheRunningSession() = runTest {
        val controller = createController()
        controller.play()
        controller.stopSession()
        assertTrue(build.currentRun.isCancelled)
        assertEquals(Res.string.status_session_stopped, controller.statuses.last())
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun stoppingWithoutASessionDoesNothing() = runTest {
        val controller = createController()
        controller.stopSession()
        assertTrue(controller.console.value.isEmpty())
    }

    @Test
    fun startingASessionReplacesTheRunningOne() = runTest {
        val controller = createController()
        controller.play()
        controller.editScene(scene)
        assertTrue(build.runs.first().isCancelled)
        assertFalse(build.currentRun.isCancelled)
        assertEquals(SessionState.Starting(SessionMode.EDIT), controller.sessionState.value)
    }

    @Test
    fun theConsoleKeepsOnlyTheMostRecentEntries() = runTest {
        val controller = createController()
        repeat(2_100) { channel.receive(SessionMessage.Failure(message = "$it")) }
        assertEquals(2_000, controller.console.value.size)
        assertEquals(ConsoleEntry.SessionFailure("2099"), controller.console.value.last())
    }

    @Test
    fun clearingEmptiesTheConsole() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Failure(message = "x"))
        controller.clearConsole()
        assertTrue(controller.console.value.isEmpty())
    }

    @Test
    fun closingStopsTheSessionAndReleasesTheBuildAndTheChannel() = runTest {
        val controller = createController()
        controller.play()
        controller.close()
        assertTrue(build.currentRun.isCancelled)
        assertTrue(build.isClosed)
        assertTrue(channel.isClosed)
    }

    @Test
    fun actorTypesAreOnlyAvailableToProjectsThatOptedIn() = runTest {
        assertNull(createController().actorTypes)
        assertNotNull(createController(actorTypeRepository = FakeActorTypeRepository()).actorTypes)
    }

    @Test
    fun savingAnActorTypeRescansTheResourcesSoItsGeneratedCodeShowsUp() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { scanCount++; emptyMap() }, actorTypeRepository = FakeActorTypeRepository())
        assertEquals(2, scanCount)
        controller.actorTypes!!.create("Player")
        assertEquals(3, scanCount)
    }

    @Test
    fun aFailedActorTypeSaveIsReportedInTheConsole() = runTest {
        val controller = createController(actorTypeRepository = FakeActorTypeRepository().apply { isFailing = true })
        controller.clearConsole()
        controller.actorTypes!!.create("Player")
        assertEquals(listOf(ConsoleEntry.Status(Res.string.status_actor_type_not_saved, isError = true)), controller.console.value)
    }
}
