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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_git_committed
import kubriko.creator.generated.resources.status_git_failed
import kubriko.creator.generated.resources.status_git_repository_created
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, EditorMutation::class)
class GitControllerTest {
    private val reports = mutableListOf<ConsoleEntry>()
    private val generated = "src/game/generated"

    private fun TestScope.createController(
        repository: FakeGitRepository?,
        created: FakeGitRepository = FakeGitRepository(),
    ): GitController {
        var current: GitRepository? = repository
        return GitController(
            openRepository = { current },
            createRepository = { message -> created.also { it.commitAll(message); current = it } },
            generatedPath = generated,
            scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            report = { reports += it },
        )
    }

    @Test
    fun aProjectWithoutARepositorySaysSo() = runTest {
        assertEquals(GitState.NoRepository, createController(repository = null).state.value)
    }

    @Test
    fun theBranchGroupedChangesAndHistoryAreShown() = runTest {
        val repository = FakeGitRepository(
            RepositoryStatus(
                branch = "main",
                changes = listOf(FileChange(".kubriko/actors/Ball.json", ChangeKind.MODIFIED), FileChange("$generated/Ball.kt", ChangeKind.MODIFIED)),
            ),
        ).apply { commits += "First" }
        val state = assertIs<GitState.Ready>(createController(repository).state.value)
        assertEquals("main", state.branch)
        assertEquals(listOf("Ball"), state.changes.map { it.title })
        assertEquals(listOf("First"), state.history.map { it.message })
    }

    @Test
    fun committingRecordsTheTrimmedMessageAndRefreshes() = runTest {
        val repository = FakeGitRepository(RepositoryStatus("main", listOf(FileChange("README.md", ChangeKind.ADDED))))
        val controller = createController(repository)
        controller.commit("  Make the Ball faster  ")
        assertEquals(listOf("Make the Ball faster"), repository.commits)
        val state = assertIs<GitState.Ready>(controller.state.value)
        assertTrue(state.changes.isEmpty())
        assertEquals(listOf("Make the Ball faster"), state.history.map { it.message })
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.status_git_committed)), reports)
    }

    @Test
    fun discardingAnActorTypeDiscardsAllOfItsFiles() = runTest {
        val repository = FakeGitRepository(
            RepositoryStatus("main", listOf(FileChange(".kubriko/actors/Ball.json", ChangeKind.MODIFIED), FileChange("$generated/Ball.kt", ChangeKind.MODIFIED))),
        )
        val controller = createController(repository)
        assertTrue(controller.discard(assertIs<GitState.Ready>(controller.state.value).changes.single().paths))
        assertEquals(listOf<Collection<String>>(listOf(".kubriko/actors/Ball.json", "$generated/Ball.kt")), repository.discarded)
        assertTrue(assertIs<GitState.Ready>(controller.state.value).changes.isEmpty())
    }

    @Test
    fun startingVersionControlCreatesARepositoryWithAFirstCommit() = runTest {
        val created = FakeGitRepository()
        val controller = createController(repository = null, created = created)
        controller.startVersionControl()
        assertEquals(1, created.commits.size)
        assertIs<GitState.Ready>(controller.state.value)
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.status_git_repository_created)), reports)
    }

    @Test
    fun aFailedOperationIsReportedAndKeepsTheLastState() = runTest {
        val repository = FakeGitRepository(RepositoryStatus("main", listOf(FileChange("README.md", ChangeKind.ADDED))))
        val controller = createController(repository)
        val before = controller.state.value
        repository.isFailing = true
        assertFalse(controller.commit("Broken"))
        assertEquals(before, controller.state.value)
        assertTrue(reports.isEmpty())
    }

    @Test
    fun aFailedRefreshIsReported() = runTest {
        val repository = FakeGitRepository()
        val controller = createController(repository)
        repository.isFailing = true
        repository.status = repository.status.copy(branch = "other")
        controller.refresh()
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.status_git_failed, isError = true)), reports)
    }
}
