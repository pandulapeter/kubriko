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

import com.pandulapeter.kubrikoCreator.foundation.unexpectedProblemHandler
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.recent.RecentProject
import com.pandulapeter.kubrikoCreator.recent.RecentProjectsStore
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectCreationException
import com.pandulapeter.kubrikoCreator.template.ProjectCreator
import com.pandulapeter.kubrikoCreator.ui.resource
import com.pandulapeter.kubrikoCreator.workspace.CLOSE_GAME_TIMEOUT_IN_MILLISECONDS
import com.pandulapeter.kubrikoCreator.workspace.CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_project_not_opened
import kubriko.creator.generated.resources.status_unexpected_problem
import java.io.File

/**
 * Creates, opens, and closes projects, deciding which [AppScreen] the window shows.
 */
internal class AppController(
    parentScope: CoroutineScope,
    private val workspaceOpener: WorkspaceOpener,
    private val projectCreator: ProjectCreator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val recentProjectsStore: RecentProjectsStore = RecentProjectsStore.default(),
) {
    /**
     * Where everything app-wide runs: what one of its coroutines throws unexpectedly is reported
     * ([reportUnexpectedProblem]), and the others keep running.
     */
    val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + unexpectedProblemHandler(::reportUnexpectedProblem))
    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Welcome())
    val screen = _screen.asStateFlow()
    private val _recentProjects = MutableStateFlow(emptyList<RecentProject>())

    /**
     * The projects opened recently, newest first, those whose folders are gone too, which the welcome screen shows greyed.
     */
    val recentProjects = _recentProjects.asStateFlow()

    /**
     * The commands of the open project, which the AI connection runs tool calls with.
     */
    val openProjectCommands get() = (_screen.value as? AppScreen.Workspace)?.controller?.commands

    /**
     * Says that something failed that nothing expected to: in the open project's console, or on the welcome screen. On the
     * other screens it is only printed.
     */
    fun reportUnexpectedProblem(exception: Throwable) {
        when (val screen = _screen.value) {
            is AppScreen.Workspace -> screen.controller.reportUnexpectedProblem(exception)
            is AppScreen.Welcome -> _screen.value = AppScreen.Welcome(error = Res.string.status_unexpected_problem, errorArguments = listOf(exception.toString()))
            else -> Unit
        }
    }

    init {
        scope.launch {
            _recentProjects.value = withContext(ioDispatcher) { recentProjectsStore.load() }
        }
    }

    fun forgetRecentProject(project: RecentProject) {
        _recentProjects.value = _recentProjects.value - project
        scope.launch(ioDispatcher) { recentProjectsStore.remove(project.path) }
    }
    private var job: Job? = null
    private var closing: Job? = null

    fun showNewProject() {
        closeProject()
        _screen.value = AppScreen.NewProject()
    }

    fun createProject(request: NewProjectRequest) {
        closeProject()
        _screen.value = AppScreen.CreatingProject(request.directory)
        launchOperation {
            awaitClosing()
            val directory = try {
                withContext(ioDispatcher) { projectCreator.create(request) }
            } catch (exception: ProjectCreationException) {
                _screen.value = AppScreen.NewProject(error = exception.reason.resource)
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
            awaitClosing()
            _screen.value = try {
                val workspace = workspaceOpener.open(directory, scope)
                try {
                    _recentProjects.value = withContext(ioDispatcher) { recentProjectsStore.add(directory, workspace.project.name) }
                } catch (exception: CancellationException) {
                    workspace.close()
                    throw exception
                }
                AppScreen.Workspace(workspace)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ProjectInspectionException) {
                AppScreen.Welcome(error = exception.reason.resource)
            } catch (exception: Exception) {
                exception.printStackTrace()
                AppScreen.Welcome(error = Res.string.error_project_not_opened, errorArguments = listOf(exception.message ?: exception.toString()))
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

    /**
     * Waits until the project closed last is closed, so that a project opened again doesn't load its types while the old
     * workspace still writes them.
     */
    private suspend fun awaitClosing() {
        withTimeoutOrNull(CLOSING_TIMEOUT_IN_MILLISECONDS) { closing?.join() }
    }

    /**
     * Closes the open project, if any.
     *
     * @return The job that finishes once the project closed last (this one, or one closed earlier and still closing) has
     *   its game gone and its changes written, which an app that is quitting waits for.
     */
    fun closeProject(): Job? {
        job?.cancel()
        (_screen.value as? AppScreen.Workspace)?.controller?.close()?.let { closing = it }
        _screen.value = AppScreen.Welcome()
        return closing
    }

    companion object {
        /**
         * How long closing a project may take after its waits for the game ([CLOSE_GAME_TIMEOUT_IN_MILLISECONDS]) and for
         * a failed write ([CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS]) ran out: stopping its work and closing its Gradle connection.
         */
        private const val CLOSE_WORK_MARGIN_IN_MILLISECONDS = 5_000L

        /**
         * How long quitting, and opening a project, wait for the open project to close, so that a write that never ends
         * can't keep the window open. It outlasts every bounded step of the project's own close, so the project is always
         * closed first.
         */
        const val CLOSING_TIMEOUT_IN_MILLISECONDS =
            CLOSE_GAME_TIMEOUT_IN_MILLISECONDS + CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS + CLOSE_WORK_MARGIN_IN_MILLISECONDS
    }
}
