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

import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiClientTest {
    private val settings = McpSettings(port = 9000, token = "abc")

    @Test
    fun claudeCodeRegistersForEveryProjectWithTheTokenAsAHeader() {
        assertEquals(
            listOf("mcp", "add", "--scope", "user", "--transport", "http", "kubriko-creator", "http://127.0.0.1:9000/mcp", "--header", "Authorization: Bearer abc"),
            AiClient.CLAUDE_CODE.addArguments(settings),
        )
        assertEquals(listOf("mcp", "remove", "--scope", "user", "kubriko-creator"), AiClient.CLAUDE_CODE.removeArguments)
        assertEquals(listOf("mcp", "get", "kubriko-creator"), AiClient.CLAUDE_CODE.getArguments)
    }

    @Test
    fun codexGetsTheTokenInTheUrl() {
        assertEquals(listOf("mcp", "add", "kubriko-creator", "--url", "http://127.0.0.1:9000/mcp?token=abc"), AiClient.CODEX.addArguments(settings))
        assertEquals(listOf("mcp", "remove", "kubriko-creator"), AiClient.CODEX.removeArguments)
    }

    @Test
    fun geminiCliIsCheckedByListingItsServers() {
        assertTrue("--header" in AiClient.GEMINI_CLI.addArguments(settings))
        assertTrue(AiClient.GEMINI_CLI.isConnected(0, "kubriko-creator: http://127.0.0.1:9000/mcp (http)"))
        assertFalse(AiClient.GEMINI_CLI.isConnected(0, "No MCP servers configured."))
    }

    @Test
    fun theUsualCommandCanBeReplaced() {
        assertEquals("claude", settings.commandOf(AiClient.CLAUDE_CODE))
        assertEquals("my-claude", settings.copy(clientCommands = mapOf(AiClient.CLAUDE_CODE to "my-claude")).commandOf(AiClient.CLAUDE_CODE))
    }
}
