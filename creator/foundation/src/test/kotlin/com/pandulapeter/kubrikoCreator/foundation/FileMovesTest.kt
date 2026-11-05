/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileMovesTest {
    private val root = testDirectory()

    @Test
    fun aMoveCreatesTheFoldersItNeedsAndNeverReplacesAnotherFile() {
        val source = File(root, "notes.md").apply { writeText("Notes") }
        assertTrue(source.moveWithoutReplacing(File(root, "docs/deep/notes.md")))
        assertEquals("Notes", File(root, "docs/deep/notes.md").readText())
        assertFalse(source.exists())
        val other = File(root, "other.md").apply { writeText("Other") }
        assertFalse(other.moveWithoutReplacing(File(root, "docs/deep/notes.md")))
        assertEquals("Notes", File(root, "docs/deep/notes.md").readText())
        assertEquals("Other", other.readText())
    }

    @Test
    fun aMoveChangingOnlyTheCaseRenamesTheFile() {
        File(root, "guide.md").writeText("Guide")
        assertTrue(File(root, "guide.md").moveWithoutReplacing(File(root, "Guide.md")))
        assertEquals(listOf("Guide.md"), root.list()?.toList())
        assertEquals("Guide", File(root, "Guide.md").readText())
    }

    @Test
    fun aMoveOntoItselfChangesNothing() {
        File(root, "guide.md").writeText("Guide")
        assertTrue(File(root, "guide.md").moveWithoutReplacing(File(root, "guide.md")))
        assertEquals(listOf("guide.md"), root.list()?.toList())
    }

    @Test
    fun aMissingFileIsntMoved() {
        assertFalse(File(root, "missing.md").moveWithoutReplacing(File(root, "moved.md")))
    }
}
