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

import com.agentclientprotocol.model.AgentCapabilities
import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.HttpHeader
import com.agentclientprotocol.model.McpCapabilities
import com.agentclientprotocol.model.McpServer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConversationToolServerTest {
    private val url = "http://127.0.0.1:7781/conversations/1"

    @Test
    fun anAgentWithoutHttpMcpRunsThePinnedMcpRemote() {
        val server = ConversationToolServer.of(AgentCapabilities(mcpCapabilities = McpCapabilities(http = false)), AgentProfile.fallback, url, "t", File("/usr/bin/npx"))
        assertIs<McpServer.Stdio>(server)
        assertEquals(listOf("-y", "mcp-remote@0.15.2", url, "--header", "Authorization:\${KUBRIKO_CREATOR_AUTHORIZATION}"), server.args)
        assertEquals(listOf(EnvVariable("KUBRIKO_CREATOR_AUTHORIZATION", "Bearer t")), server.env)
        assertTrue(server.args.none { "Bearer" in it })
    }

    @Test
    fun anAgentWithHttpMcpGetsTheAddressDirectly() {
        val server = ConversationToolServer.of(AgentCapabilities(mcpCapabilities = McpCapabilities(http = true)), AgentProfile.fallback, url, "t", File("/usr/bin/npx"))
        assertIs<McpServer.Http>(server)
        assertEquals(url, server.url)
        assertEquals(listOf(HttpHeader("Authorization", "Bearer t")), server.headers)
    }
}
