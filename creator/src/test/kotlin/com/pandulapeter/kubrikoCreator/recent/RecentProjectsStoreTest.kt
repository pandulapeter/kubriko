/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.recent

import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentProjectsStoreTest {
    private val root = createTempDirectory().toFile()
    private val file = File(root, "settings/recent.json")
    private var time = 0L
    private val store = RecentProjectsStore(file, currentTimeMillis = { ++time })

    private fun project(name: String) = File(root, name).apply { mkdirs() }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun nothingIsRememberedAtFirst() {
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun projectsAreRememberedNewestFirstAndSurviveARestart() {
        store.add(project("first"), "First")
        store.add(project("second"), "Second")
        assertEquals(listOf("Second", "First"), RecentProjectsStore(file).load().map { it.name })
    }

    @Test
    fun openingAProjectAgainMovesItToTheTopWithoutDuplicating() {
        val first = project("first")
        store.add(first, "First")
        store.add(project("second"), "Second")
        store.add(File(first, "../first"), "First")
        assertEquals(listOf("First", "Second"), store.load().map { it.name })
    }

    @Test
    fun projectsWhoseFolderIsGoneAreKeptForADriveThatIsntMounted() {
        val gone = project("gone")
        store.add(gone, "Gone")
        store.add(project("kept"), "Kept")
        gone.deleteRecursively()
        assertEquals(listOf("Kept", "Gone"), store.load().map { it.name })
        assertTrue("Gone" in file.readText())
    }

    @Test
    fun changesFromSeveralThreadsAtOnceAreAllKept() {
        val projects = (0 until RecentProjectsStore.MAXIMUM_COUNT).map { project("game$it") }
        projects.map { directory -> Thread { store.add(directory, directory.name) }.apply { start() } }.forEach(Thread::join)
        assertEquals(projects.map { it.name }.toSet(), store.load().map { it.name }.toSet())
    }

    @Test
    fun onlyTheMostRecentProjectsAreKept() {
        repeat(RecentProjectsStore.MAXIMUM_COUNT + 3) { store.add(project("project$it"), "Project $it") }
        val projects = store.load()
        assertEquals(RecentProjectsStore.MAXIMUM_COUNT, projects.size)
        assertEquals("Project ${RecentProjectsStore.MAXIMUM_COUNT + 2}", projects.first().name)
    }

    @Test
    fun projectsCanBeForgotten() {
        store.add(project("first"), "First")
        val kept = store.add(project("second"), "Second")
        store.remove(kept.last().path)
        assertEquals(listOf("Second"), store.load().map { it.name })
    }

    @Test
    fun aCorruptFileCountsAsNoProjects() {
        file.parentFile.mkdirs()
        file.writeText("{ not a list")
        assertTrue(store.load().isEmpty())
        store.add(project("first"), "First")
        assertEquals(listOf("First"), store.load().map { it.name })
    }

    @Test
    fun anUnreadableListIsntLost() {
        file.parentFile.mkdirs()
        file.writeText("{ not a list")
        store.add(project("first"), "First")
        assertEquals("{ not a list", File(file.parentFile, "recent.json.bak").readText())
        assertEquals(listOf("First"), store.load().map { it.name })
    }

    @Test
    fun aListThatCantBeReadForAMomentIsntReplaced() {
        store.add(project("first"), "First")
        store.add(project("second"), "Second")
        RecentProjectsStore(file, currentTimeMillis = { ++time }, readText = lockedOnce()).add(project("third"), "Third")
        assertEquals(listOf("Second", "First"), store.load().map { it.name })
        RecentProjectsStore(file, currentTimeMillis = { ++time }, readText = lockedOnce()).remove(store.load().first().path)
        assertEquals(listOf("Second", "First"), store.load().map { it.name })
    }

    /**
     * Reads the file, failing the first time as if another program held it for a moment.
     */
    private fun lockedOnce(): (File) -> String {
        var isLocked = true
        return { file -> if (isLocked) throw IOException("Locked.").also { isLocked = false } else file.readText() }
    }
}
