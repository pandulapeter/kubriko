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
import com.pandulapeter.kubrikoCreator.workspace.GradleWorkspaceOpener
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_not_a_gradle_project
import java.io.File

/**
 * Opens and closes projects, deciding which [AppScreen] the window shows.
 */
internal class AppController(
    private val scope: CoroutineScope,
    private val workspaceOpener: WorkspaceOpener = GradleWorkspaceOpener,
) {
    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Welcome())
    val screen = _screen.asStateFlow()
    private var openingJob: Job? = null

    fun openProject(directory: File) {
        closeProject()
        _screen.value = AppScreen.Opening(directory)
        openingJob = scope.launch {
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

    fun closeProject() {
        openingJob?.cancel()
        (_screen.value as? AppScreen.Workspace)?.controller?.close()
        _screen.value = AppScreen.Welcome()
    }
}
