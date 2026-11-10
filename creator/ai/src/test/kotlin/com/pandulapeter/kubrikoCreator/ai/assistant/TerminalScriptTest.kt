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

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalScriptTest {

    @Test
    fun doubleQuotesInsideAnArgumentReachAWindowsProgramAsTheyAre() {
        assertEquals("plain", TerminalScript.PowerShell.nativeArgument("plain"))
        assertEquals("say \\\"hi\\\"", TerminalScript.PowerShell.nativeArgument("say \"hi\""))
        assertEquals("a\\\\\\\"b", TerminalScript.PowerShell.nativeArgument("a\\\"b"))
        assertEquals("C:\\My Folder\\\\", TerminalScript.PowerShell.nativeArgument("C:\\My Folder\\"))
        assertEquals("C:\\Folder\\", TerminalScript.PowerShell.nativeArgument("C:\\Folder\\"))
    }

    @Test
    fun aPowerShellScriptPassesArgumentsTheSameWayOnEveryVersion() {
        val files = TerminalScriptFiles(File("s.ps1"), File("s.pid"), File("s.exit"))
        val text = TerminalScript.PowerShell.text(AgentCommand("login.cmd", listOf("say \"hi\"")), File("home"), files)
        assertTrue(text.startsWith("\uFEFF"))
        assertTrue("\$PSNativeCommandArgumentPassing = 'Legacy'" in text)
        assertTrue("& 'login.cmd' 'say \\\"hi\\\"'" in text)
    }
}
