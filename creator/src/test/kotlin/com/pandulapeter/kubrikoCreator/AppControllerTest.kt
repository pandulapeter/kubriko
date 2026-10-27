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
import com.pandulapeter.kubrikoCreator.workspace.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.workspace.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceOpener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_no_desktop_module
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

    private fun TestScope.createController(opener: WorkspaceOpener) = AppController(
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        workspaceOpener = opener,
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
}
