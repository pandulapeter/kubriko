/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.git

import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_git_committed
import kubriko.creator.generated.resources.status_git_failed
import kubriko.creator.generated.resources.status_git_repository_created
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.IOException

/**
 * The project's version control: what changed since the last commit, committing it, the history, and discarding
 * changes. Operations run one at a time, off the main thread.
 *
 * @param openRepository The project's repository, or null if it has none.
 * @param createRepository Makes the project a repository, committing everything with the given message.
 * @param generatedPath Where the project's generated code lives, for grouping it with its Actor types.
 */
internal class GitController(
    private val openRepository: () -> GitRepository?,
    private val createRepository: (message: String) -> GitRepository,
    private val generatedPath: String?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val report: (ConsoleEntry) -> Unit,
    private val firstCommitMessage: String = "Start version control with Kubriko Creator.",
) {
    private val _state = MutableStateFlow<GitState>(GitState.Loading)
    val state = _state.asStateFlow()
    private val operations = Mutex()

    init {
        refresh()
    }

    fun refresh() = run { repository -> repository?.let(::readState) ?: GitState.NoRepository }

    fun commit(message: String) {
        if (message.isBlank()) return
        run(onSuccess = { report(ConsoleEntry.Status(Res.string.status_git_committed)) }) { repository ->
            repository?.commitAll(message.trim())
            repository?.let(::readState) ?: GitState.NoRepository
        }
    }

    fun discard(entry: ChangeEntry) = run { repository ->
        repository?.discard(entry.paths)
        repository?.let(::readState) ?: GitState.NoRepository
    }

    fun startVersionControl() = run(onSuccess = { report(ConsoleEntry.Status(Res.string.status_git_repository_created)) }) {
        readState(createRepository(firstCommitMessage))
    }

    private fun readState(repository: GitRepository): GitState {
        val status = repository.status()
        return GitState.Ready(
            branch = status.branch,
            changes = ChangeGrouping.group(status.changes, generatedPath),
            history = repository.history(HISTORY_SIZE),
        )
    }

    private fun run(onSuccess: () -> Unit = {}, operation: (GitRepository?) -> GitState) {
        scope.launch {
            val newState = operations.withLock {
                withContext(ioDispatcher) {
                    try {
                        operation(openRepository())
                    } catch (_: GitAPIException) {
                        null
                    } catch (_: IOException) {
                        null
                    }
                }
            }
            if (newState == null) {
                report(ConsoleEntry.Status(Res.string.status_git_failed, isError = true))
            } else {
                _state.value = newState
                onSuccess()
            }
        }
    }

    private companion object {
        const val HISTORY_SIZE = 50
    }
}
