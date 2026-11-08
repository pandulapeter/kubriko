/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(EditorMutation::class)
class ScreenplayStoreTest {
    private val root = testDirectory()
    private val store = ScreenplayStore(root)

    @Test
    fun pagesAreListedTheFirstPageFirstAndFollowWritesMovesAndDeletes() {
        store.write("zebra", "# Z")
        store.write("levels/cave", "# Cave")
        store.write("README", "# The game")
        assertEquals(listOf("README", "levels/cave", "zebra"), store.names())
        assertEquals(setOf("levels"), store.folders())
        assertTrue(store.move("levels/cave", "cave"))
        assertEquals(listOf("README", "cave", "zebra"), store.current.value)
        assertEquals(setOf("levels"), store.folders())
        store.deleteFolder("levels")
        assertEquals(emptySet(), store.folders())
        store.delete("zebra")
        assertEquals(listOf("README", "cave"), store.names())
        assertNull(store.read("zebra"))
    }

    @Test
    fun aPageMadeOutsideIsKnownOnceRefreshedAndTakenIgnoringCase() {
        store.refresh()
        File(root, "screenplay/Hero.md").apply { parentFile.mkdirs() }.writeText("# Hero")
        assertNull(store.existingName("hero"))
        assertEquals("Hero", store.scanExistingName("hero"))
        val revision = store.revision.value
        store.refresh()
        assertEquals("Hero", store.existingName("hero"))
        assertTrue(store.revision.value > revision)
    }

    @Test
    fun writingKeepsTheLineBreaksTheFileHas() {
        File(root, "screenplay/README.md").apply { parentFile.mkdirs() }.writeText("# A\r\n\r\nOld\r\n")
        store.write("README", "# A\n\nNew\n")
        assertEquals("# A\r\n\r\nNew\r\n", File(root, "screenplay/README.md").readText())
    }

    @Test
    fun namesAreValidatedPartByPart() {
        listOf("hero", "levels/cave", "Level 1-2", "a_b").forEach { assertTrue(PageNaming.isValid(it), it) }
        listOf("", "/hero", "levels/", "../x", "a\\b", "hero.md", " lead", "trail ", "x".repeat(121)).forEach { assertFalse(PageNaming.isValid(it), it) }
    }

    @Test
    fun creatorsOwnWritesOfImagesAmongThePagesAndOfTheSettingsAreHeardOf() {
        val ownWrites = OwnWrites()
        val followed = ScreenplayStore(root, ownWrites)
        val start = followed.revision.value
        ownWrites.record(File(root, "screenplay/levels/cave-1.png"))
        ownWrites.record(File(root, ScreenplaySettings.FILE))
        assertEquals(start + 2, followed.revision.value)
        ownWrites.record(File(root, "docs/other.png"))
        ownWrites.record(File(root, "screenplay/hero.md"))
        assertEquals(start + 2, followed.revision.value)
    }
}
