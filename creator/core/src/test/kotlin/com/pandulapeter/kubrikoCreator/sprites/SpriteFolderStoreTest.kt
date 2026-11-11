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

    @Test
    fun aSheetIsKeptBesideTheFolderAndGoesWithItsLastFile() {
        val sheet = SpriteSheet(frameWidth = 16, frameHeight = 16, frameCount = 4, framesPerRow = 2, framesPerSecond = 8f)
        store.setSheet("bat", sheet)
        assertEquals(mapOf("bat" to sheet), store.state.value.sheets)
        assertEquals(mapOf("bat" to sheet), SpriteSheetFiles().read(root))
        store.createFolder("enemies")
        store.move("bat", "enemies")
        assertEquals(sheet, store.state.value.sheets["bat"])
        store.move("bat", "")
        store.setSheet("bat", null)
        assertEquals(emptyList(), store.filesOf("bat"))
        assertEquals(SpriteFolderStore.State(setOf("enemies")), store.state.value)
    }

    @Test
    fun aSheetsFileChangedWithinTheSameMomentAndLengthIsReadAgain() {
        val sheetFiles = SpriteSheetFiles()
        val sheet = SpriteSheet(frameWidth = 16, frameHeight = 16, frameCount = 4, framesPerRow = 2)
        store.setSheet("bat", sheet)
        val file = store.filesOf("bat").single()
        assertEquals(mapOf("bat" to sheet), sheetFiles.read(root))
        val stamp = file.lastModified()
        file.writeText(file.readText().replace("\"frameCount\": 4", "\"frameCount\": 2"))
        file.setLastModified(stamp)
        assertEquals(mapOf("bat" to sheet.copy(frameCount = 2)), sheetFiles.read(root))
        file.delete()
        assertEquals(emptyMap(), sheetFiles.read(root))
    }

    @Test
    fun removingASheetKeepsTheFolderAndWhatElseTheFileHolds() {
        File(root, ".kubriko/sprites/enemies").mkdirs()
        File(root, ".kubriko/sprites/enemies/bat.json").writeText("{\"later\":1}\n")
        store.refresh()
        store.setSheet("bat", SpriteSheet(frameWidth = 8, frameHeight = 8, frameCount = 2, framesPerRow = 2))
        store.setSheet("bat", null)
        assertEquals("{\n    \"later\": 1\n}\n", File(root, ".kubriko/sprites/enemies/bat.json").readText())
        assertEquals("enemies", store.state.value.folderOf["bat"])
    }

    @Test
    fun aSheetThatCanNotBePlayedOrReadIsLeftOut() {
        File(root, ".kubriko/sprites").mkdirs()
        File(root, ".kubriko/sprites/bat.json").writeText("{\"sheet\":{\"frameWidth\":0,\"frameHeight\":8,\"frameCount\":2,\"framesPerRow\":2}}")
        File(root, ".kubriko/sprites/owl.json").writeText("{\"sheet\":{\"frameWidth\":\"wide\"}}")
        File(root, ".kubriko/sprites/cat.json").writeText("not json")
        store.refresh()
        assertEquals(emptyMap(), store.state.value.sheets)
        assertEquals(setOf("bat", "owl", "cat"), store.state.value.folderOf.keys)
    }
}
