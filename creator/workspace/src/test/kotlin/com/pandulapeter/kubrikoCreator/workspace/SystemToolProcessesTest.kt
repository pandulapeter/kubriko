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

import com.pandulapeter.kubrikoCreator.session.JavaArgumentFile
import com.pandulapeter.kubrikoCreator.testing.GameProcessTestGame
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemToolProcessesTest {
    private val directory = testDirectory()
    private val java = File(System.getProperty("java.home"), "bin/java").path
    private val missing = File(directory, "missing-tool").path

    private val classpathFile = File(directory, "classpath.args").apply { writeText(JavaArgumentFile.format(listOf("-cp", System.getProperty("java.class.path")))) }

    /**
     * The test game, its classpath through an argument file, since Windows caps a command line at 32,767 characters.
     */
    private fun game(mode: String?, pidFile: File? = null) = listOfNotNull(
        java,
        "@${classpathFile.path}",
        mode?.let { "-D${GameProcessTestGame.MODE_PROPERTY}=$it" },
        pidFile?.let { "-D${GameProcessTestGame.PID_FILE_PROPERTY}=${it.path}" },
        GameProcessTestGame::class.java.name,
    )

    @Test
    fun aCommandRunsToItsEndWithWhatItPrinted() = blockingTest {
        assertEquals(ToolResult(exitCode = 0, output = "Played.${System.lineSeparator()}"), SystemToolProcesses.run(game(mode = null), 10_000))
        val failed = SystemToolProcesses.run(game(mode = "fail"), 10_000)
        assertEquals(3, failed.exitCode)
        assertContains(failed.output, "Line 40")
    }

    @Test
    fun aCommandThatRunsTooLongOrCantStartHasNoExitCode() = blockingTest {
        assertNull(SystemToolProcesses.run(game(mode = "sleep"), 200).exitCode)
        assertNull(SystemToolProcesses.run(listOf(missing), 1_000).exitCode)
        assertNull(SystemToolProcesses.stream(listOf(missing)) {})
    }

    @Test
    fun aStreamHandsOverEveryLineAndStopsItsProcessWhenCancelled() = blockingTest {
        val lines = mutableListOf<String>()
        assertEquals(3, SystemToolProcesses.stream(game(mode = "fail"), lines::add))
        assertEquals((1..40).map { "Line $it" }, lines)
        val pidFile = File(directory, "pid")
        val streaming = launch { SystemToolProcesses.stream(game(mode = "sleep", pidFile = pidFile)) {} }
        withTimeout(10_000) { while (!pidFile.isFile || pidFile.readText().isEmpty()) delay(10) }
        val process = ProcessHandle.of(pidFile.readText().trim().toLong()).get()
        streaming.cancelAndJoin()
        withTimeout(5_000) { while (process.isAlive) delay(10) }
    }

    @Test
    fun aDetachedCommandReportsItsEndAndItsLastLines() = blockingTest {
        val tool = SystemToolProcesses.startDetached(game(mode = "fail"))
        withTimeout(10_000) { while (tool.exitCode == null) delay(10) }
        assertEquals(3, tool.exitCode)
        assertEquals((11..40).map { "Line $it" }, tool.output)
        val missingTool = SystemToolProcesses.startDetached(listOf(missing))
        assertTrue(missingTool.exitCode != null)
        assertFalse(missingTool.output.isEmpty())
    }
}
