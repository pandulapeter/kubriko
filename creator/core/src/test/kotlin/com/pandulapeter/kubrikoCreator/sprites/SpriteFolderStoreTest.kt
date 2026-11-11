/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import org.junit.Assume.assumeFalse
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(EditorMutation::class)
class SpriteFolderStoreTest {
    private val root = testDirectory()
    private val store = SpriteFolderStore(root, OwnWrites())

    @Test
    fun aSpritesFileSaysWhichFolderItIsIn() {
        File(root, ".kubriko/sprites/enemies/flying").mkdirs()
        File(root, ".kubriko/sprites/enemies/bat.json").writeText(SpriteFolderStore.DEFAULT_TEXT)
        store.refresh()
        assertEquals(SpriteFolderStore.State(setOf("enemies", "enemies/flying"), mapOf("bat" to "enemies")), store.state.value)
    }

    @Test
    fun movingKeepsWhatTheFileHoldsAndLeavesNoFileForNothing() {
        store.createFolder("enemies")
        store.move("bat", "enemies")
        assertEquals("enemies", store.state.value.folderOf["bat"])
        store.move("bat", "")
        assertNull(store.state.value.folderOf["bat"])
        assertEquals(emptyList(), store.filesOf("bat"))
        File(root, ".kubriko/sprites/enemies/owl.json").writeText("{\"frames\":4}\n")
        store.refresh()
        store.move("owl", "")
        assertEquals("{\"frames\":4}\n", File(root, ".kubriko/sprites/owl.json").readText())
    }

    @Test
    fun aFolderMovesWithEverythingInItAndOneWithASpriteIsntDeleted() {
        store.createFolder("enemies/flying")
        store.move("bat", "enemies/flying")
        assertTrue(store.moveFolder("enemies", "foes"))
        assertEquals(SpriteFolderStore.State(setOf("foes", "foes/flying"), mapOf("bat" to "foes/flying")), store.state.value)
        store.deleteFolder("foes")
        assertTrue(File(root, ".kubriko/sprites/foes").isDirectory)
        store.rename("bat", "owl")
        assertEquals(mapOf("owl" to "foes/flying"), store.state.value.folderOf)
        assertFalse(File(root, ".kubriko/sprites/foes/flying/bat.json").exists())
    }

    @Test
    fun aSpriteInTwoFoldersIsInTheFirstAndMovesAndRenamesWithAllOfThem() {
        File(root, ".kubriko/sprites/enemies").mkdirs()
        File(root, ".kubriko/sprites/foes").mkdirs()
        File(root, ".kubriko/sprites/foes/bat.json").writeText(SpriteFolderStore.DEFAULT_TEXT)
        File(root, ".kubriko/sprites/enemies/bat.json").writeText(SpriteFolderStore.DEFAULT_TEXT)
        store.refresh()
        assertEquals("enemies", store.state.value.folderOf["bat"])
        store.rename("bat", "owl")
        assertEquals(2, store.filesOf("owl").size)
        store.move("owl", "foes")
        assertEquals(listOf(File(root, ".kubriko/sprites/foes/owl.json")), store.filesOf("owl"))
        assertFalse(File(root, ".kubriko/sprites/enemies/owl.json").exists())
    }

    @Test
    fun aMoveThatCanNotBeWrittenLeavesTheSpriteWhereItWas() {
        store.createFolder("enemies")
        store.move("bat", "enemies")
        val locked = File(root, ".kubriko/sprites/locked").apply { mkdirs() }
        store.refresh()
        locked.setWritable(false)
        try {
            assumeFalse("the folder still takes new files", runCatching { File(locked, "probe").createNewFile() }.getOrDefault(false))
            assertFailsWith<IOException> { store.move("bat", "locked") }
        } finally {
            locked.setWritable(true)
        }
        store.refresh()
        assertEquals("enemies", store.state.value.folderOf["bat"])
    }
}
