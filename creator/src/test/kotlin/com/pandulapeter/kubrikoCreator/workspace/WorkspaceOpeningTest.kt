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

import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceOpeningTest {
    private val build = FakeProjectBuild()
    private val channel = FakeSessionChannel()
    private var isWatcherClosed = false
    private lateinit var workspaceJob: Job

    private fun TestScope.parentScope() = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

    private suspend fun TestScope.open(afterCreating: suspend (WorkspaceOpening) -> Unit) = openWorkspace(parentScope(), UnconfinedTestDispatcher(testScheduler)) { opening ->
        workspaceJob = opening.scope.launch { awaitCancellation() }
        WorkspaceController(
            project = CreatorProject(rootDirectory = testDirectory(), name = "game"),
            build = opening.track(build),
            sessionChannel = opening.track(channel),
            parentScope = parentScope(),
            workspaceScope = opening.scope,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            scanResources = { emptyMap() },
            blockTypeRepository = FakeBlockTypeRepository(),
            startWatching = { opening.track(AutoCloseable { isWatcherClosed = true }) },
            openGitRepository = { null },
        ).also { afterCreating(opening) }
    }

    @Test
    fun aFailedOpeningClosesWhatItStarted() = runTest {
        val failure = IOException("Broken.")
        val thrown = assertFailsWith<IOException> { open { throw failure } }
        assertSame(failure, thrown)
        assertTrue(workspaceJob.isCancelled, "the workspace's coroutines are cancelled")
        assertTrue(channel.isClosed, "the session channel is closed")
        assertTrue(isWatcherClosed, "the watcher is closed")
        assertTrue(build.isClosed, "the Gradle connection is closed")
    }

    @Test
    fun aCancelledOpeningClosesWhatItStarted() = runTest {
        assertFailsWith<CancellationException> { open { throw CancellationException("Closed.") } }
        assertTrue(workspaceJob.isCancelled)
        assertTrue(channel.isClosed)
        assertTrue(isWatcherClosed)
        assertTrue(build.isClosed)
    }

    @Test
    fun whatFailsToCloseDoesNotKeepTheRestOpenAndIsKeptWithTheReason() = runTest {
        val thrown = assertFailsWith<IOException> {
            open { opening ->
                opening.track(AutoCloseable { throw IllegalStateException("Stuck.") })
                throw IOException("Broken.")
            }
        }
        assertEquals(listOf("Stuck."), thrown.suppressed.map { it.message })
        assertTrue(channel.isClosed)
        assertTrue(isWatcherClosed)
        assertTrue(build.isClosed)
    }

    @Test
    fun aSuccessfulOpeningKeepsWhatItStartedRunning() = runTest {
        open {}
        assertFalse(workspaceJob.isCancelled)
        assertFalse(channel.isClosed)
        assertFalse(isWatcherClosed)
        assertFalse(build.isClosed)
    }
}
