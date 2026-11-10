/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.testing.blockingTest
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SystemTerminalLauncherTest {
    private val directory = createTempDirectory().toFile()
    private val scripts = File(directory, "sign-in")
    private val workingDirectory = File(directory, "home folder").apply { mkdirs() }
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    /**
     * Runs the script as the terminal would, in the background, with its input closed, so a failed command's waiting for
     * the user ends at once.
     */
    private fun runningScripts(terminal: SystemTerminal) = SystemTerminalLauncher(terminal, scripts, workingDirectory, open = { command ->
        val script = command.last()
        val process = ProcessBuilder(if (isWindows) listOf("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script) else listOf("sh", script))
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        process.outputStream.close()
    })

    @Test
    fun theCommandRunsInItsFolderWithItsVariablesAndItsExitCodeIsReadOnce() = blockingTest(timeout = 30.seconds) {
        val greeting = "it's \"quoted\" \$HOME ‘typographic’ %PATH%"
        val command = if (isWindows) {
            AgentCommand("powershell.exe", listOf("-NoProfile", "-Command", "[IO.File]::WriteAllText(\"\$PWD\\out.txt\", \$env:GREETING); exit 3"), mapOf("GREETING" to greeting))
        } else {
            AgentCommand("sh", listOf("-c", "printf '%s' \"\$GREETING\" > out.txt; exit 3"), mapOf("GREETING" to greeting, "not a name" to "skipped"))
        }
        val channel = runningScripts(if (isWindows) SystemTerminal.Windows else SystemTerminal.Mac).launch(command)
        assertEquals(3, channel.awaitExit())
        assertEquals(greeting, File(workingDirectory, "out.txt").readText())
        assertEquals(emptyList(), scripts.listFiles()!!.map { it.name })
    }

    @Test
    fun aTerminalThatCantBeOpenedLeavesNoScript() {
        val launcher = SystemTerminalLauncher(SystemTerminal.Mac, scripts, workingDirectory, open = { throw IOException("No terminal") })
        assertFailsWith<IOException> { launcher.launch(AgentCommand("login", environment = mapOf("API_KEY" to "secret"))) }
        assertEquals(emptyList(), scripts.listFiles()!!.toList())
    }

    @Test
    fun stoppingDeletesAScriptNotRunYetAndWhatEarlierLaunchesLeft() {
        val now = 100L * 60 * 1000
        scripts.mkdirs()
        val leftover = File(scripts, "earlier.command").apply { writeText("export API_KEY='secret'") }
        leftover.setLastModified(now - 11L * 60 * 1000)
        val recent = File(scripts, "running.exit").apply { writeText("0") }
        recent.setLastModified(now - 60L * 1000)
        val opened = mutableListOf<List<String>>()
        val launcher = SystemTerminalLauncher(SystemTerminal.Mac, scripts, workingDirectory, clock = { now }, open = { opened += it })
        val channel = launcher.launch(AgentCommand("login"))
        val script = File(opened.single().last())
        assertFalse(leftover.exists())
        assertTrue(recent.exists())
        assertTrue(script.canExecute())
        channel.stop()
        assertFalse(script.exists())
    }

    @Test
    fun eachSystemOpensTheScriptInATerminalOfItsOwn() {
        val script = File("/settings/sign-in/a b.command")
        assertEquals(listOf("open", script.path), SystemTerminal.Mac.openCommand(script))
        assertEquals(listOf("konsole", "-e", "sh", script.path), SystemTerminal.Linux(listOf("konsole", "-e")).openCommand(script))
        assertEquals(
            listOf("cmd", "/c", "start", "\"\"", "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.path),
            SystemTerminal.Windows.openCommand(script),
        )
    }

    @Test
    fun linuxUsesTheFirstTerminalEmulatorFoundOnThePath() {
        val bin = File(directory, "bin").apply { mkdirs() }
        assertEquals(SystemTerminal.Mac, SystemTerminal.find("mac os x", emptyList()))
        assertEquals(SystemTerminal.Windows, SystemTerminal.find("windows 11", emptyList()))
        assertNull(SystemTerminal.find("linux", listOf(bin)))
        listOf("xterm", "konsole").forEach { File(bin, it).apply { writeText("") }.setExecutable(true) }
        assertEquals(SystemTerminal.Linux(listOf("konsole", "-e")), SystemTerminal.find("linux", listOf(bin)))
    }
}
