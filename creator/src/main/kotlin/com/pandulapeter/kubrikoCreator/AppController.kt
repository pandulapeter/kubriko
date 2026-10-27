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

import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectCreationException
import com.pandulapeter.kubrikoCreator.template.ProjectCreator
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import com.pandulapeter.kubrikoCreator.workspace.GradleWorkspaceOpener
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_not_a_gradle_project
import java.io.File

/**
 * Creates, opens, and closes projects, deciding which [AppScreen] the window shows.
 */
internal class AppController(
    private val scope: CoroutineScope,
    private val workspaceOpener: WorkspaceOpener = GradleWorkspaceOpener,
    private val projectCreator: ProjectCreator = ProjectGenerator(CreatorConfiguration.load()),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Welcome())
    val screen = _screen.asStateFlow()
    private var job: Job? = null

    fun showNewProject() {
        closeProject()
        _screen.value = AppScreen.NewProject()
    }

    fun createProject(request: NewProjectRequest) {
        closeProject()
        _screen.value = AppScreen.CreatingProject(request.directory)
        launchOperation {
            val directory = try {
                withContext(ioDispatcher) { projectCreator.create(request) }
            } catch (exception: ProjectCreationException) {
                _screen.value = AppScreen.NewProject(error = exception.reason)
                return@launchOperation
            }
            ensureActive()
            openProject(directory)
        }
    }

    fun openProject(directory: File) {
        closeProject()
        _screen.value = AppScreen.Opening(directory)
        launchOperation {
            _screen.value = try {
                AppScreen.Workspace(workspaceOpener.open(directory, scope))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ProjectInspectionException) {
                AppScreen.Welcome(error = exception.reason)
            } catch (_: Exception) {
                AppScreen.Welcome(error = Res.string.error_not_a_gradle_project)
            }
        }
    }

    /**
     * Starts [operation] as the current job only once it's assigned, so a close coming from inside the operation can
     * cancel it however the scope dispatches.
     */
    private fun launchOperation(operation: suspend CoroutineScope.() -> Unit) {
        val operationJob = scope.launch(start = CoroutineStart.LAZY, block = operation)
        job = operationJob
        operationJob.start()
    }

    fun closeProject() {
        job?.cancel()
        (_screen.value as? AppScreen.Workspace)?.controller?.close()
        _screen.value = AppScreen.Welcome()
    }
}
