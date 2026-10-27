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
    fun projectsWhoseFolderIsGoneAreLeftOutAndForgotten() {
        val gone = project("gone")
        store.add(gone, "Gone")
        store.add(project("kept"), "Kept")
        gone.deleteRecursively()
        assertEquals(listOf("Kept"), store.load().map { it.name })
        assertTrue("Gone" !in file.readText())
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
}
