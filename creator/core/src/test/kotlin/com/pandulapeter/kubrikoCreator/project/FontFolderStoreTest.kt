/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(EditorMutation::class)
class FontFolderStoreTest {
    private val root = testDirectory()
    private val store = FontFolderStore(root, OwnWrites())

    @Test
    fun fontsKeepTheirFoldersInTheirOwnDirectoryApartFromSprites() {
        File(root, ".kubriko/sprites/titles").mkdirs()
        store.createFolder("titles")
        store.move("pixel", "titles")
        assertTrue(File(root, ".kubriko/fonts/titles/pixel.json").isFile)
        assertEquals(FontFolderStore.State(setOf("titles"), mapOf("pixel" to "titles")), store.state.value)
        store.rename("pixel", "blocky")
        assertTrue(store.moveFolder("titles", "headings"))
        assertEquals(FontFolderStore.State(setOf("headings"), mapOf("blocky" to "headings")), store.state.value)
        store.move("blocky", "")
        assertFalse(File(root, ".kubriko/fonts/headings/blocky.json").exists())
        assertEquals(FontFolderStore.State(setOf("headings")), store.refresh())
    }
}
