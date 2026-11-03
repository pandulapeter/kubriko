/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GradlePropertiesFileTest {
    private val root = testDirectory()
    private val file = File(root, "gradle.properties")

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aMissingFileIsCreatedAndAnExistingOneKeepsItsLineBreaks() {
        assertNull(GradlePropertiesFile.read(root))
        GradlePropertiesFile.update(root) { "$it" + "a=1\n" }
        assertEquals("a=1\n", file.readText())
        file.writeText("a=1\r\nb=2\r\n")
        assertEquals("a=1\nb=2\n", GradlePropertiesFile.read(root))
        GradlePropertiesFile.update(root) { it.replace("b=2", "b=3") }
        assertEquals("a=1\r\nb=3\r\n", file.readText())
    }

    @Test
    fun aChangeThatChangesNothingWritesNothing() {
        file.writeText("a=1\r\n")
        file.setLastModified(1_000_000L)
        GradlePropertiesFile.update(root) { it }
        assertEquals(1_000_000L, file.lastModified())
    }
}
