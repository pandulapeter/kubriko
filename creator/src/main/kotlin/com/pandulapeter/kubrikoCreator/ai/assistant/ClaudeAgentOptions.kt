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

import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * What a conversation with [RegistryAgent.claude] is started with, through the `_meta` of `session/new`.
 */
internal object ClaudeAgentOptions {

    /**
     * Claude's own tools that change files or run commands, removed so that every change goes through Kubriko Creator's
     * tools, as the user's own do.
     */
    val disallowedTools = listOf("Bash", "Write", "Edit", "NotebookEdit")

    /**
     * Claude Code's permission rule allowing every tool of Kubriko Creator's MCP server without asking, since those
     * changes show up in the editor and can be undone.
     */
    val allowedTools = listOf("mcp__${McpToolServer.SERVER_NAME}")

    /**
     * @param systemPrompt Appended to Claude Code's own system prompt.
     */
    fun sessionMeta(systemPrompt: String): JsonObject = buildJsonObject {
        putJsonObject("systemPrompt") { put("append", systemPrompt) }
        putJsonObject("claudeCode") {
            putJsonObject("options") {
                putJsonArray("disallowedTools") { disallowedTools.forEach { add(it) } }
                putJsonArray("allowedTools") { allowedTools.forEach { add(it) } }
            }
        }
    }
}
