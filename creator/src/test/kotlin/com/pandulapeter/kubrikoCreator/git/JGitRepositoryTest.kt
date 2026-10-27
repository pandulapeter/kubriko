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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JGitRepositoryTest {
    private val root = createTempDirectory().toFile()

    private fun write(path: String, text: String) = File(root, path).apply { parentFile.mkdirs(); writeText(text) }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aFolderWithoutARepositoryHasNone() {
        assertNull(JGitRepository.open(root))
    }

    @Test
    fun aCreatedRepositoryStartsOnMainWithEverythingCommitted() {
        write("README.md", "Hello")
        write("build/out.txt", "ignored")
        write(".gitignore", "build/\n")
        val repository = JGitRepository.create(root, "Created with Kubriko Creator.")
        assertNotNull(JGitRepository.open(root))
        assertEquals(RepositoryStatus(branch = "main", changes = emptyList()), repository.status())
        assertEquals(listOf("Created with Kubriko Creator."), repository.history(10).map { it.message })
    }

    @Test
    fun newChangedAndDeletedFilesAreListed() {
        write("kept.txt", "a")
        write("changed.txt", "a")
        write("deleted.txt", "a")
        val repository = JGitRepository.create(root, "First")
        write("changed.txt", "b")
        File(root, "deleted.txt").delete()
        write("new/file.txt", "c")
        assertEquals(
            listOf(
                FileChange("changed.txt", ChangeKind.MODIFIED),
                FileChange("deleted.txt", ChangeKind.DELETED),
                FileChange("new/file.txt", ChangeKind.ADDED),
            ),
            repository.status().changes,
        )
    }

    @Test
    fun committingRecordsEveryKindOfChange() {
        write("changed.txt", "a")
        write("deleted.txt", "a")
        val repository = JGitRepository.create(root, "First")
        write("changed.txt", "b")
        File(root, "deleted.txt").delete()
        write("new.txt", "c")
        repository.commitAll("Second")
        assertTrue(repository.status().changes.isEmpty())
        val history = repository.history(10)
        assertEquals(listOf("Second", "First"), history.map { it.message })
        assertEquals(7, history.first().id.length)
        assertTrue(history.first().time >= history.last().time)
    }

    @Test
    fun theHistoryIsLimitedToTheMostRecentCommits() {
        write("file.txt", "0")
        val repository = JGitRepository.create(root, "Commit 0")
        (1..5).forEach {
            write("file.txt", "$it")
            repository.commitAll("Commit $it")
        }
        assertEquals(listOf("Commit 5", "Commit 4"), repository.history(2).map { it.message })
    }

    @Test
    fun discardingRestoresChangedAndDeletedFilesAndRemovesNewOnes() {
        write("changed.txt", "original")
        write("deleted.txt", "original")
        val repository = JGitRepository.create(root, "First")
        write("changed.txt", "edited")
        File(root, "deleted.txt").delete()
        write("new.txt", "new")
        write("kept.txt", "keep me")
        repository.discard(listOf("changed.txt", "deleted.txt", "new.txt"))
        assertEquals("original", File(root, "changed.txt").readText())
        assertEquals("original", File(root, "deleted.txt").readText())
        assertFalse(File(root, "new.txt").exists())
        assertEquals(listOf(FileChange("kept.txt", ChangeKind.ADDED)), repository.status().changes)
    }

    @Test
    fun discardingAFileThatWasAddedToTheIndexRemovesItToo() {
        write("first.txt", "a")
        val repository = JGitRepository.create(root, "First")
        write("staged.txt", "b")
        org.eclipse.jgit.api.Git.open(root).use { it.add().addFilepattern("staged.txt").call() }
        repository.discard(listOf("staged.txt"))
        assertFalse(File(root, "staged.txt").exists())
        assertTrue(repository.status().changes.isEmpty())
    }
}
