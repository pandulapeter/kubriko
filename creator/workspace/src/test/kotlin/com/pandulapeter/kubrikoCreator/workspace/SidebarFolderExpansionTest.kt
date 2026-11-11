/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SidebarFolderExpansionTest {
    private val root = testDirectory()
    private val file = File(root, "${LocalProjectStore.FOLDER}/${SidebarFolderExpansion.FILE_NAME}")

    private fun TestScope.memory() = StandardTestDispatcher(testScheduler).let { dispatcher ->
        SidebarFolderExpansion(root, CoroutineScope(backgroundScope.coroutineContext + dispatcher), dispatcher)
    }

    @Test
    fun whatIsSavedIsThereTheNextTime() = runTest {
        val memory = memory()
        assertNull(memory.saved.value)
        testScheduler.runCurrent()
        assertEquals(emptyMap(), memory.saved.value)
        memory.save(mapOf("module:game" to false, "game|folders:SCENES|levels" to true))
        testScheduler.runCurrent()
        val next = memory()
        testScheduler.runCurrent()
        assertEquals(mapOf("module:game" to false, "game|folders:SCENES|levels" to true), next.saved.value)
    }

    @Test
    fun whatWasReadIsNotWrittenAgain() = runTest {
        file.parentFile.mkdirs()
        file.writeText("""{"choices":{"module:game":false}}""")
        file.setLastModified(1_000L)
        val memory = memory()
        testScheduler.runCurrent()
        memory.save(mapOf("module:game" to false))
        testScheduler.runCurrent()
        assertEquals(1_000L, file.lastModified())
    }

    @Test
    fun aSaveBeforeTheReadWaitsForIt() = runTest {
        file.parentFile.mkdirs()
        file.writeText("""{"choices":{"module:game":false}}""")
        val memory = memory()
        memory.save(mapOf("module:game" to false, "module:tools" to true))
        testScheduler.runCurrent()
        assertEquals(mapOf("module:game" to false), memory.saved.value)
        assertEquals(mapOf("module:game" to false, "module:tools" to true), memory().also { testScheduler.runCurrent() }.saved.value)
    }

    @Test
    fun aFileThatCantBeReadMeansNothingWasKept() = runTest {
        file.parentFile.mkdirs()
        file.writeText("{")
        val memory = memory()
        testScheduler.runCurrent()
        assertEquals(emptyMap(), memory.saved.value)
        assertFalse(File(root, ".gitignore").exists())
    }
}
