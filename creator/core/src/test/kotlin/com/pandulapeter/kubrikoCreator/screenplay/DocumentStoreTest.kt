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
class DocumentStoreTest {
    private val root = testDirectory()
    private val store = DocumentStore(root, OwnWrites()) { listOf("README.md", "docs/guide.md") }

    @Test
    fun onlyTheScannedDocumentsAreKnown() {
        assertTrue(store.isKnown("docs/guide.md"))
        assertFalse(store.isKnown("notes.md"))
        assertEquals(listOf("README.md", "docs/guide.md"), store.paths())
    }

    @Test
    fun aWriteKeepsTheFilesLineBreaksAndRaisesTheRevision() {
        File(root, "README.md").writeText("# Game\r\n\r\nOld.\r\n")
        store.write("README.md", "# Game\n\nNew.\n")
        assertEquals("# Game\r\n\r\nNew.\r\n", File(root, "README.md").readText())
        assertEquals(1, store.revision.value)
        assertEquals("# Game\r\n\r\nNew.\r\n", store.read("README.md"))
        assertNull(store.read("missing.md"))
    }

    @Test
    fun aMoveCreatesTheFoldersItNeedsAndNeverReplacesAFile() {
        File(root, "docs").mkdirs()
        File(root, "docs/guide.md").writeText("# Guide\n")
        assertTrue(store.move("docs/guide.md", "screenplay/guide.md"))
        assertEquals("# Guide\n", File(root, "screenplay/guide.md").readText())
        assertFalse(File(root, "docs/guide.md").exists())
        File(root, "README.md").writeText("# Game\n")
        assertFalse(store.move("README.md", "screenplay/guide.md"))
        assertEquals("# Guide\n", File(root, "screenplay/guide.md").readText())
        assertTrue(File(root, "README.md").isFile)
    }

    @Test
    fun aChangeOutsideRaisesTheRevision() {
        store.onChangedOutside()
        assertEquals(1, store.revision.value)
    }
}
