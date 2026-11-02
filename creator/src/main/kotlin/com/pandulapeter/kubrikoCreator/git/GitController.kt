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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
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
 * changes. Operations run one at a time, off the main thread; refreshes are coalesced, so that at most one runs and one
 * waits however many are requested.
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
    private val refreshes = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in refreshes) {
                try {
                    if (perform { repository -> (repository?.let { readState(it) } ?: GitState.NoRepository) to Unit } == null) {
                        report(ConsoleEntry.Status(Res.string.status_git_failed, isError = true))
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    currentCoroutineContext()[CoroutineExceptionHandler]?.handleException(currentCoroutineContext(), exception)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        refreshes.trySend(Unit)
    }

    /**
     * Commits every change, if the repository's status says there are any and none is in conflict. The status is read
     * after the files were written, inside the operation, since [state] may not have caught up with them yet.
     *
     * @return What happened. Only a commit is reported; the caller reports the rest.
     */
    @EditorMutation
    suspend fun commit(message: String): CommitResult {
        val result = perform { repository ->
            if (repository == null) return@perform GitState.NoRepository to CommitResult.Failed
            val status = repository.status()
            when {
                status.changes.isEmpty() -> readState(repository, status) to CommitResult.NothingToCommit
                status.changes.any { it.kind == ChangeKind.CONFLICTING } -> readState(repository, status) to CommitResult.Conflicts
                else -> {
                    repository.commitAll(message.trim())
                    readState(repository) to CommitResult.Committed
                }
            }
        } ?: CommitResult.Failed
        if (result == CommitResult.Committed) report(ConsoleEntry.Status(Res.string.status_git_committed))
        return result
    }

    /**
     * Restores [paths] to how the last commit has them.
     *
     * @return Whether the changes were discarded.
     */
    @EditorMutation
    suspend fun discard(paths: Collection<String>) = perform { repository ->
        repository?.discard(paths)
        (repository?.let { readState(it) } ?: GitState.NoRepository) to Unit
    } != null

    /**
     * @return Whether the repository was created.
     */
    @EditorMutation
    suspend fun startVersionControl(): Boolean {
        val isCreated = perform { readState(createRepository(firstCommitMessage)) to Unit } != null
        if (isCreated) report(ConsoleEntry.Status(Res.string.status_git_repository_created))
        return isCreated
    }

    private fun readState(repository: GitRepository, status: RepositoryStatus = repository.status()) = GitState.Ready(
        branch = status.branch,
        changes = ChangeGrouping.group(status.changes, generatedPath),
        history = repository.history(HISTORY_SIZE),
    )

    /**
     * Runs [operation] on the repository, one at a time and off the main thread, and takes the state it returns.
     *
     * @return What [operation] returned besides the state, or null if git failed.
     */
    private suspend fun <T : Any> perform(operation: (GitRepository?) -> Pair<GitState, T>): T? {
        val outcome = operations.withLock {
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
        } ?: return null
        _state.value = outcome.first
        return outcome.second
    }

    private companion object {
        const val HISTORY_SIZE = 50
    }
}
