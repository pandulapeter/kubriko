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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgramLookupTest {
    private val directory = createTempDirectory().toFile()

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun emptyEntriesOfThePathAreLeftOut() {
        val separator = File.pathSeparator
        assertEquals(listOf(File("/usr/bin"), File("/bin")), pathFolders("/usr/bin$separator$separator/bin$separator"))
        assertEquals(emptyList(), pathFolders(null))
    }

    @Test
    fun theFirstProgramThatCanBeRunIsFound() {
        val first = File(directory, "first").apply { mkdirs() }
        val second = File(directory, "second").apply { mkdirs() }
        File(first, "tool").mkdirs()
        val program = File(second, "tool").apply { writeText("") }.also { it.setExecutable(true) }
        assertEquals(program, findProgram("tool", listOf(first, second)))
        assertNull(findProgram("other", listOf(first, second)))
    }
}
