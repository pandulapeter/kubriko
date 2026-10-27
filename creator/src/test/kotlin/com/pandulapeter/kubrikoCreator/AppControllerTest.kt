/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectCreationException
import com.pandulapeter.kubrikoCreator.template.ProjectCreator
import com.pandulapeter.kubrikoCreator.workspace.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.workspace.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceOpener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_no_desktop_module
import kubriko.creator.generated.resources.error_project_folder_not_empty
import kubriko.creator.generated.resources.error_not_a_gradle_project
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {
    private val directory = File("/projects/game")
    private val builds = mutableListOf<FakeProjectBuild>()

    private val request = NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = File("/projects"))
    private val openedDirectories = mutableListOf<File>()

    private fun TestScope.createController(
        creator: ProjectCreator = ProjectCreator { it.directory },
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
        opener: WorkspaceOpener = WorkspaceOpener { directory, scope -> openedDirectories += directory; workspace(scope) },
    ) = AppController(
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        workspaceOpener = opener,
        projectCreator = creator,
        ioDispatcher = ioDispatcher,
    )

    private fun workspace(scope: CoroutineScope) = WorkspaceController(
        project = CreatorProject(rootDirectory = directory, name = "game", desktopModulePath = ":"),
        build = FakeProjectBuild().also(builds::add),
        sessionChannel = FakeSessionChannel(),
        scope = scope,
        scanResources = { emptyMap() },
    )

    @Test
    fun theWelcomeScreenComesFirst() = runTest {
        assertEquals(AppScreen.Welcome(), createController { _, scope -> workspace(scope) }.screen.value)
    }

    @Test
    fun aProjectShowsTheOpeningScreenUntilItsWorkspaceIsReady() = runTest {
        val opened = CompletableDeferred<Unit>()
        val controller = createController { _, scope -> opened.await(); workspace(scope) }
        controller.openProject(directory)
        assertEquals(AppScreen.Opening(directory), controller.screen.value)
        opened.complete(Unit)
        assertIs<AppScreen.Workspace>(controller.screen.value)
    }

    @Test
    fun theOpenerReceivesTheChosenFolder() = runTest {
        val directories = mutableListOf<File>()
        createController { directory, scope -> directories += directory; workspace(scope) }.openProject(directory)
        assertEquals(listOf(directory), directories)
    }

    @Test
    fun aProjectThatCantBeUsedReturnsToWelcomeWithItsReason() = runTest {
        val controller = createController { _, _ -> throw ProjectInspectionException(Res.string.error_no_desktop_module) }
        controller.openProject(directory)
        assertEquals(AppScreen.Welcome(error = Res.string.error_no_desktop_module), controller.screen.value)
    }

    @Test
    fun aFolderGradleCantOpenReturnsToWelcomeWithAGenericReason() = runTest {
        val controller = createController { _, _ -> throw IOException("Not a build.") }
        controller.openProject(directory)
        assertEquals(AppScreen.Welcome(error = Res.string.error_not_a_gradle_project), controller.screen.value)
    }

    @Test
    fun closingAProjectReleasesItsWorkspace() = runTest {
        val controller = createController { _, scope -> workspace(scope) }
        controller.openProject(directory)
        controller.closeProject()
        assertEquals(AppScreen.Welcome(), controller.screen.value)
        assertTrue(builds.single().isClosed)
    }

    @Test
    fun openingAnotherProjectReleasesTheCurrentOne() = runTest {
        val controller = createController { _, scope -> workspace(scope) }
        controller.openProject(directory)
        val first = (controller.screen.value as AppScreen.Workspace).controller
        controller.openProject(File("/projects/other"))
        assertTrue(builds.first().isClosed)
        val second = (controller.screen.value as AppScreen.Workspace).controller
        assertNotSame(first, second)
        assertFalse(builds.last().isClosed)
    }

    @Test
    fun closingWhileOpeningAbandonsTheOpening() = runTest {
        val opened = CompletableDeferred<Unit>()
        val controller = createController { _, scope -> opened.await(); workspace(scope) }
        controller.openProject(directory)
        controller.closeProject()
        opened.complete(Unit)
        assertEquals(AppScreen.Welcome(), controller.screen.value)
        assertTrue(builds.isEmpty())
    }

    @Test
    fun newProjectShowsTheNewProjectScreen() = runTest {
        val controller = createController()
        controller.showNewProject()
        assertEquals(AppScreen.NewProject(), controller.screen.value)
    }

    @Test
    fun cancellingANewProjectReturnsToWelcome() = runTest {
        val controller = createController()
        controller.showNewProject()
        controller.closeProject()
        assertEquals(AppScreen.Welcome(), controller.screen.value)
    }

    @Test
    fun aCreatedProjectIsOpenedRightAway() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val created = mutableListOf<NewProjectRequest>()
        val controller = createController(creator = { created += it; File("/projects/my-game") }, ioDispatcher = dispatcher)
        controller.createProject(request)
        assertEquals(AppScreen.CreatingProject(request.directory), controller.screen.value)
        runCurrent()
        assertEquals(listOf(request), created)
        assertEquals(listOf(File("/projects/my-game")), openedDirectories)
        assertIs<AppScreen.Workspace>(controller.screen.value)
    }

    @Test
    fun aProjectThatCantBeCreatedReturnsToTheFormWithItsReason() = runTest {
        val controller = createController(creator = { throw ProjectCreationException(Res.string.error_project_folder_not_empty) })
        controller.createProject(request)
        assertEquals(AppScreen.NewProject(error = Res.string.error_project_folder_not_empty), controller.screen.value)
        assertTrue(openedDirectories.isEmpty())
    }

    @Test
    fun closingWhileCreatingNeverOpensTheProject() = runTest {
        lateinit var controller: AppController
        var createdCount = 0
        controller = createController(creator = { createdCount++; controller.closeProject(); it.directory })
        controller.createProject(request)
        assertEquals(1, createdCount)
        assertEquals(AppScreen.Welcome(), controller.screen.value)
        assertTrue(openedDirectories.isEmpty())
    }

    @Test
    fun closingBeforeCreationStartsNeverCreatesTheProject() = runTest {
        var createdCount = 0
        val controller = createController(creator = { createdCount++; it.directory }, ioDispatcher = StandardTestDispatcher(testScheduler))
        controller.createProject(request)
        controller.closeProject()
        runCurrent()
        assertEquals(0, createdCount)
        assertEquals(AppScreen.Welcome(), controller.screen.value)
    }
}
