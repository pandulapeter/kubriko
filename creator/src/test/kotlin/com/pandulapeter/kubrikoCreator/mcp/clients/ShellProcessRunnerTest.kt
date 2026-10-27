/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Runs real processes through a real shell, so it only runs where there is one (not on Windows).
 */
class ShellProcessRunnerTest {
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
    private val runner = ShellProcessRunner(shell = "/bin/sh")

    @Test
    fun argumentsArePassedAsTheyAre() {
        if (isWindows) return
        val result = runner.run("printf", listOf("%s|", "Authorization: Bearer abc", "it's \"quoted\"", "\$HOME", "a;b"))
        assertEquals(0, result.exitCode)
        assertEquals("Authorization: Bearer abc|it's \"quoted\"|\$HOME|a;b|", result.output)
    }

    @Test
    fun theCommandMaySetEnvironmentVariablesFirst() {
        if (isWindows) return
        assertEquals("personal", runner.run("PROFILE_NAME=personal sh", listOf("-c", "printf %s \"\$PROFILE_NAME\"")).output)
        assertTrue(runner.exists("PROFILE_NAME=personal sh"))
    }

    @Test
    fun aliasesFromTheShellsStartupFilesWorkLikeInATerminal() {
        if (isWindows || !File("/bin/zsh").canExecute()) return
        val home = createTempDirectory().toFile()
        try {
            File(home, ".zshrc").writeText("alias claude-personal='PROFILE_NAME=personal sh'\n")
            val runner = ShellProcessRunner(shell = "/bin/zsh", environment = mapOf("ZDOTDIR" to home.absolutePath))
            assertTrue(runner.exists("claude-personal"))
            assertEquals("personal", runner.run("claude-personal", listOf("-c", "printf %s \"\$PROFILE_NAME\"")).output.lines().last())
        } finally {
            home.deleteRecursively()
        }
    }

    @Test
    fun missingProgramsAreReportedAndFailuresKeepTheirOutput() {
        if (isWindows) return
        assertFalse(runner.exists("kubriko-creator-no-such-program"))
        val result = runner.run("sh", listOf("-c", "echo broken >&2; exit 3"))
        assertEquals(3, result.exitCode)
        assertEquals("broken", result.output)
    }

    @Test
    fun programsAreLocatedByTheirFullPath() {
        if (isWindows) return
        assertEquals("/bin/sh", runner.locate("sh"))
        assertNull(runner.locate("kubriko-creator-no-such-program"))
    }
}
