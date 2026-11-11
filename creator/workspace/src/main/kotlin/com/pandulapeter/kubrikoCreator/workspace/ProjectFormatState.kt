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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.project.UnreadableProjectFileException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Whether this Kubriko Creator may change the project, as [projectFormat] finds it.
 *
 * @param onChanged Called with the new status once [refresh] finds it changed.
 */
internal class ProjectFormatState(
    initialStatus: ProjectFormatStatus,
    private val projectFormat: ProjectFormat,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val report: (ConsoleEntry) -> Unit,
    private val onChanged: (ProjectFormatStatus) -> Unit,
) {
    private val _status = MutableStateFlow(initialStatus)
    val status: StateFlow<ProjectFormatStatus> = _status.asStateFlow()
    val isReadOnly get() = _status.value is ProjectFormatStatus.ReadOnly

    /**
     * Checks the project's format version again after `.kubriko/project.json` changed (a git checkout of a branch a newer
     * Kubriko Creator saved, for example), migrating an older one; the files a migration rewrites are then reloaded as
     * any other change on disk.
     */
    fun refresh() {
        scope.launch {
            val status = try {
                withContext(ioDispatcher) { projectFormat.prepare() }
            } catch (exception: UnreadableProjectFileException) {
                report(ConsoleEntry.Status(exception.reason, isError = true, arguments = exception.arguments))
                return@launch
            } catch (exception: IOException) {
                report(ConsoleEntry.Status(ProjectProblem.FORMAT_NOT_UPGRADED, isError = true, arguments = listOf(exception.message ?: exception.toString())))
                return@launch
            }
            val previous = _status.value
            _status.value = status
            if (status != previous) onChanged(status)
        }
    }
}
