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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomAgentCommandTest {
    private val locate = { program: String -> "/opt/homebrew/bin/$program".takeIf { program == "opencode" } }

    @Test
    fun aProgramWithoutAFolderIsFoundAsTheShellWouldAndItsFolderGoesOnThePath() {
        val command = CustomAgentCommand.of("OPENCODE_MODEL=ollama/qwen opencode acp --port 0", "/Users/me", locate)!!
        assertEquals("/opt/homebrew/bin/opencode", command.program)
        assertEquals(listOf("acp", "--port", "0"), command.arguments)
        assertEquals("ollama/qwen", command.environment["OPENCODE_MODEL"])
        assertTrue(command.environment.getValue("PATH").startsWith("/opt/homebrew/bin${File.pathSeparator}"))
    }

    @Test
    fun aProgramWithAFolderIsUsedAsTypedWithHomeExpandedAndWordsUnquoted() {
        val command = CustomAgentCommand.of("env MODE='chat' ~/agents/goose \"acp\"", "/Users/me", locate)!!
        assertEquals("/Users/me/agents/goose", command.program)
        assertEquals(listOf("acp"), command.arguments)
        assertEquals("chat", command.environment["MODE"])
    }

    @Test
    fun aProgramTheShellDoesntKnowIsLeftForTheSystemToFind() =
        assertEquals("goose", CustomAgentCommand.of("goose acp", "/Users/me", locate)!!.program)

    @Test
    fun aCommandLineOfVariablesAloneStartsNothing() {
        assertNull(CustomAgentCommand.of("MODE=chat", "/Users/me", locate))
        assertNull(CustomAgentCommand.of("  ", "/Users/me", locate))
    }

    @Test
    fun theCustomAgentIsNamedAfterItsProgram() = assertEquals("goose", AgentChoice.Custom("MODE=chat ~/bin/goose acp").name)
}
