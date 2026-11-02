/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.FileChange
import com.pandulapeter.kubrikoCreator.git.GitRepository
import com.pandulapeter.kubrikoCreator.git.RepositoryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * What every [GitRepository] does, run against the real one and every fake of it.
 */
internal abstract class GitRepositoryContract {

    /**
     * A new repository on `main` holding one commit, `"Created."`, of `kept.txt`, `changed.txt`, and `deleted.txt`.
     */
    protected abstract fun repository(): GitRepository

    /**
     * Makes [path] show as [kind] in the status of [repository].
     */
    protected abstract fun change(repository: GitRepository, path: String, kind: ChangeKind)

    private fun GitRepository.changeEveryKind() {
        change(this, "new.txt", ChangeKind.ADDED)
        change(this, "changed.txt", ChangeKind.MODIFIED)
        change(this, "deleted.txt", ChangeKind.DELETED)
    }

    @Test
    fun aNewRepositoryIsOnMainWithNothingChanged() {
        val repository = repository()
        assertEquals(RepositoryStatus(branch = "main", changes = emptyList()), repository.status())
        assertEquals(listOf("Created."), repository.history(10).map { it.message })
    }

    @Test
    fun committingRecordsEveryChangeAndHistoryIsNewestFirst() {
        val repository = repository()
        repository.changeEveryKind()
        repository.commitAll("Second")
        assertEquals(emptyList(), repository.status().changes)
        assertEquals(listOf("Second", "Created."), repository.history(10).map { it.message })
        assertEquals(1, repository.history(1).size)
    }

    @Test
    fun commitIdsAreDistinctAndNotBlank() {
        val repository = repository()
        change(repository, "changed.txt", ChangeKind.MODIFIED)
        repository.commitAll("Second")
        val (newest, oldest) = repository.history(10).map { it.id }
        assertTrue(newest.isNotBlank() && oldest.isNotBlank())
        assertNotEquals(newest, oldest)
    }

    @Test
    fun discardingRemovesOnlyTheGivenPathsFromTheChanges() {
        val repository = repository()
        repository.changeEveryKind()
        repository.discard(listOf("new.txt", "deleted.txt"))
        assertEquals(listOf(FileChange("changed.txt", ChangeKind.MODIFIED)), repository.status().changes)
    }

    @Test
    fun committingWithoutChangesStillRecordsACommit() {
        val repository = repository()
        repository.commitAll("Empty")
        assertEquals("Empty", repository.history(10).first().message)
    }
}
