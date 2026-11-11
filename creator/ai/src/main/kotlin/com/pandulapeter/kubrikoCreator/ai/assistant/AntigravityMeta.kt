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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Google Antigravity's extension of a conversation's `_meta` (`session/new` and `session/load`), for an agent without a
 * profile that answers `initialize` with an `agentCapabilities._meta.antigravity` object: its own tools that edit or create
 * files or run commands turned off, since it has no read-only mode, and, where it advertises taking it so
 * (`systemPrompt.append`), the system prompt appended to its own rather than sent ahead of the first prompt.
 */
internal object AntigravityMeta {
    private const val NAMESPACE = "antigravity"
    private const val SYSTEM_PROMPT = "systemPrompt"
    private const val APPEND = "append"
    private const val DISABLED_TOOLS = "disabledTools"

    /**
     * Antigravity's tools that edit or create files or run commands (it deletes and moves files only through commands), by
     * the names `disabledTools` takes, which are those its tool calls' titles show ("Run edit_file?"), not the ones its model
     * calls them by (`replace_file_content`, `write_to_file`), which it ignores there.
     */
    val toolsChangingFiles = listOf("edit_file", "create_file", "run_command")

    /**
     * Whether an agent answering `initialize` with [capabilities] takes the system prompt through [meta].
     */
    fun isAppendingAdvertised(capabilities: AgentCapabilities): Boolean {
        val systemPrompt = namespaceOf(capabilities).child(SYSTEM_PROMPT)
        return (systemPrompt.child(APPEND) as? JsonPrimitive)?.booleanOrNull == true
    }

    /**
     * The `_meta` that turns off [toolsChangingFiles] and appends [appendedSystemPrompt], if one is given, to the agent's
     * own, or null for an agent whose [capabilities] don't show the extension.
     */
    fun meta(capabilities: AgentCapabilities, appendedSystemPrompt: String?): JsonObject? {
        if (namespaceOf(capabilities) !is JsonObject) return null
        return buildJsonObject {
            putJsonObject(NAMESPACE) {
                appendedSystemPrompt?.let { prompt -> putJsonObject(SYSTEM_PROMPT) { put(APPEND, prompt) } }
                putJsonArray(DISABLED_TOOLS) { toolsChangingFiles.forEach { add(it) } }
            }
        }
    }

    private fun namespaceOf(capabilities: AgentCapabilities) = capabilities._meta.child(NAMESPACE)

    private fun JsonElement?.child(key: String) = (this as? JsonObject)?.get(key)
}
