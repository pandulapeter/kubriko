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
import com.agentclientprotocol.model.McpServer
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import java.io.File

/**
 * How a conversation reaches Kubriko Creator's tools at [url] with [token]: over HTTP where the agent takes HTTP MCP
 * servers (or its [AgentProfile] says it does), otherwise through `mcp-remote`, run by [npx], which passes standard
 * input and output on to the address, with the token in its environment. Without npx, HTTP is the only way left.
 */
internal object ConversationToolServer {
    private const val AUTHORIZATION_VARIABLE = "KUBRIKO_CREATOR_AUTHORIZATION"

    fun of(capabilities: AgentCapabilities, profile: AgentProfile, url: String, token: String, npx: File?): McpServer =
        if (profile.alwaysUsesHttpTools || capabilities.mcpCapabilities.http || npx == null) {
            McpServer.Http(
                name = McpToolServer.SERVER_NAME,
                url = url,
                headers = listOf(HttpHeader("Authorization", "Bearer $token")),
            )
        } else {
            McpServer.Stdio(
                name = McpToolServer.SERVER_NAME,
                command = npx.path,
                args = listOf("-y", McpToolServer.MCP_REMOTE, url, "--header", "Authorization:\${$AUTHORIZATION_VARIABLE}"),
                env = listOf(EnvVariable(AUTHORIZATION_VARIABLE, "Bearer $token")),
            )
        }
}
