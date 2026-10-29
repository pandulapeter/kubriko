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

import com.pandulapeter.kubrikoCreator.commands.EditorMutation
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
import org.eclipse.jgit.api.errors.JGitInternalException
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

    fun refresh() {
        scope.launch {
            if (!perform { repository -> repository?.let(::readState) ?: GitState.NoRepository }) report(ConsoleEntry.Status(Res.string.status_git_failed, isError = true))
        }
    }

    /**
     * @return Whether the changes were committed. Failures are left for the caller to report.
     */
    @EditorMutation
    suspend fun commit(message: String) = perform(onSuccess = { report(ConsoleEntry.Status(Res.string.status_git_committed)) }) { repository ->
        repository?.commitAll(message.trim())
        repository?.let(::readState) ?: GitState.NoRepository
    }

    /**
     * Restores [paths] to how the last commit has them.
     *
     * @return Whether the changes were discarded.
     */
    @EditorMutation
    suspend fun discard(paths: Collection<String>) = perform { repository ->
        repository?.discard(paths)
        repository?.let(::readState) ?: GitState.NoRepository
    }

    /**
     * @return Whether the repository was created.
     */
    @EditorMutation
    suspend fun startVersionControl() = perform(onSuccess = { report(ConsoleEntry.Status(Res.string.status_git_repository_created)) }) {
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

    private suspend fun perform(onSuccess: () -> Unit = {}, operation: (GitRepository?) -> GitState): Boolean {
        val newState = operations.withLock {
            withContext(ioDispatcher) {
                try {
                    operation(openRepository())
                } catch (_: GitAPIException) {
                    null
                } catch (_: JGitInternalException) {
                    null
                } catch (_: IOException) {
                    null
                }
            }
        }
        if (newState == null) return false
        _state.value = newState
        onSuccess()
        return true
    }

    private companion object {
        const val HISTORY_SIZE = 50
    }
}
