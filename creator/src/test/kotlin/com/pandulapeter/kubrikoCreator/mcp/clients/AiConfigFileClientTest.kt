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
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AiConfigFileClientTest {
    private val settings = McpSettings(port = 7781, token = "secret")
    private val home = File("/home/someone")

    @Test
    fun claudeDesktopKeepsItsServersInItsSettingsFolder() {
        assertEquals(File(home, "Library/Application Support/Claude/claude_desktop_config.json"), AiConfigFileClient.CLAUDE_DESKTOP.configFile(home, "mac os x", null))
        assertEquals(File("C:/Users/someone/AppData/Roaming", "Claude/claude_desktop_config.json"), AiConfigFileClient.CLAUDE_DESKTOP.configFile(home, "windows 11", "C:/Users/someone/AppData/Roaming"))
        assertEquals(File(home, ".config/Claude/claude_desktop_config.json"), AiConfigFileClient.CLAUDE_DESKTOP.configFile(home, "linux", null))
        assertEquals(File(home, ".cursor/mcp.json"), AiConfigFileClient.CURSOR.configFile(home, "windows 11", "C:/Users/someone/AppData/Roaming"))
    }

    @Test
    fun claudeDesktopReachesTheServerThroughMcpRemoteRunByTheNpxTheShellFinds() {
        assertEquals(
            Json.parseToJsonElement(
                """
                    {
                        "command": "/opt/homebrew/bin/npx",
                        "args": ["-y", "mcp-remote@0.15", "http://127.0.0.1:7781/mcp", "--header", "Authorization:${'$'}{KUBRIKO_CREATOR_AUTHORIZATION}"],
                        "env": {
                            "KUBRIKO_CREATOR_AUTHORIZATION": "Bearer secret",
                            "PATH": "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin"
                        }
                    }
                """,
            ),
            AiConfigFileClient.CLAUDE_DESKTOP.entry(settings, "/opt/homebrew/bin/npx"),
        )
        val withoutPath = AiConfigFileClient.CLAUDE_DESKTOP.entry(settings, programPath = null)
        assertEquals(Json.parseToJsonElement("\"npx\""), withoutPath["command"])
        assertEquals(Json.parseToJsonElement("""{"KUBRIKO_CREATOR_AUTHORIZATION":"Bearer secret"}"""), withoutPath["env"])
        assertFalse("secret" in withoutPath["args"].toString())
    }

    @Test
    fun cursorTalksToTheServerDirectly() {
        assertEquals(
            Json.parseToJsonElement("""{"url":"http://127.0.0.1:7781/mcp","headers":{"Authorization":"Bearer secret"}}"""),
            AiConfigFileClient.CURSOR.entry(settings, programPath = null),
        )
    }

    @Test
    fun idsDontClashWithThoseOfCommandLineTools() {
        val ids = AiConfigFileClient.entries.map { it.id } + AiClient.entries.map { it.id }
        assertEquals(ids.distinct(), ids)
        AiConfigFileClient.entries.forEach { assertEquals(null, AiClientSetup.fromId(it.id)) }
    }
}
