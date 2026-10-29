/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.git

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.gitController
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.FakeGitRepository
import com.pandulapeter.kubrikoCreator.git.FileChange
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.git.RepositoryStatus
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_already_under_version_control
import kubriko.creator.generated.resources.error_command_change_missing
import kubriko.creator.generated.resources.error_command_commit_conflicts
import kubriko.creator.generated.resources.error_command_commit_message_empty
import kubriko.creator.generated.resources.error_command_no_repository
import kubriko.creator.generated.resources.error_command_nothing_to_commit
import kubriko.creator.generated.resources.status_git_failed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitCommandsTest {
    private fun changed(vararg paths: String) = FakeGitRepository(RepositoryStatus("main", paths.map { FileChange(it, ChangeKind.MODIFIED) }))

    @Test
    fun versionControlIsStartedOnlyOnceAndCantBeUndone() = runTest {
        val context = commandContext(git = gitController(repository = null))
        assertNull(StartVersionControl.validate(context, NoInput))
        assertNull(StartVersionControl.apply(context, NoInput).inverse)
        assertIs<GitState.Ready>(context.git.state.value)
        val underVersionControl = commandContext(git = gitController(changed()))
        assertEquals(Rejection(Res.string.error_command_already_under_version_control), StartVersionControl.validate(underVersionControl, NoInput))
    }

    @Test
    fun committingNeedsARepositoryAMessageAndChanges() = runTest {
        assertEquals(Rejection(Res.string.error_command_no_repository), Commit.validate(commandContext(), Commit.Input("Hello.")))
        val clean = commandContext(git = gitController(changed()))
        assertEquals(Rejection(Res.string.error_command_nothing_to_commit), Commit.validate(clean, Commit.Input("Hello.")))
        val repository = changed("README.md")
        val context = commandContext(git = gitController(repository))
        assertEquals(Rejection(Res.string.error_command_commit_message_empty), Commit.validate(context, Commit.Input("  ")))
        assertNull(Commit.validate(context, Commit.Input("Write the README.")))
        assertNull(Commit.apply(context, Commit.Input("Write the README.")).inverse)
        assertEquals(listOf("Write the README."), repository.commits)
    }

    @Test
    fun committingIsRejectedWhileAMergeLeftConflicts() = runTest {
        val repository = FakeGitRepository(RepositoryStatus("main", listOf(FileChange("README.md", ChangeKind.CONFLICTING))))
        val context = commandContext(git = gitController(repository))
        assertEquals(Rejection(Res.string.error_command_commit_conflicts), Commit.validate(context, Commit.Input("Merge.")))
    }

    @Test
    fun aFailedCommitIsRejectedWithItsReasonAndNotRecorded() = runTest {
        val repository = changed("README.md")
        val context = commandContext(git = gitController(repository))
        val rejections = mutableListOf<Rejection>()
        val executor = CommandExecutor(context, testScope(), onRejected = { rejections += it })
        repository.isFailing = true
        executor.dispatch(Invocation(Commit, Commit.Input("Write the README.")))
        assertEquals(listOf(Rejection(Res.string.status_git_failed)), rejections)
        assertNull(executor.history.value.nextUndoTitle)
    }

    @Test
    fun onlyChangedFilesCanBeDiscarded() = runTest {
        val repository = changed("README.md", "build.gradle.kts")
        val context = commandContext(git = gitController(repository))
        assertEquals(Rejection(Res.string.error_command_change_missing, "Game.kt"), DiscardChanges.validate(context, DiscardChanges.Input(listOf("README.md", "Game.kt"))))
        val outcome = CommandExecutor(context, testScope()).execute(Invocation(DiscardChanges, DiscardChanges.Input(listOf("README.md"))))
        assertIs<CommandOutcome.Success>(outcome)
        assertEquals(listOf<Collection<String>>(listOf("README.md")), repository.discarded)
        assertTrue(assertIs<GitState.Ready>(context.git.state.value).changes.none { it.title == "README.md" })
    }
}
