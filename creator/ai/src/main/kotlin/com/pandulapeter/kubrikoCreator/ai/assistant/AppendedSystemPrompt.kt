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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The system prompt appended to an agent's own through the `_meta` of `session/new` and `session/load`, for an agent
 * without a profile that advertises taking it so in its `initialize` answer, as Google Antigravity does
 * (`agentCapabilities._meta.antigravity.systemPrompt.append`), rather than sent ahead of the first prompt.
 */
internal object AppendedSystemPrompt {
    private const val NAMESPACE = "antigravity"
    private const val SYSTEM_PROMPT = "systemPrompt"
    private const val APPEND = "append"

    /**
     * Whether an agent answering `initialize` with [capabilities] takes the system prompt through [meta].
     */
    fun isAdvertised(capabilities: AgentCapabilities): Boolean {
        val systemPrompt = capabilities._meta.child(NAMESPACE).child(SYSTEM_PROMPT)
        return (systemPrompt.child(APPEND) as? JsonPrimitive)?.booleanOrNull == true
    }

    /**
     * The `_meta` that appends [systemPrompt] to the agent's own.
     */
    fun meta(systemPrompt: String): JsonObject = buildJsonObject {
        putJsonObject(NAMESPACE) {
            putJsonObject(SYSTEM_PROMPT) { put(APPEND, systemPrompt) }
        }
    }

    private fun JsonElement?.child(key: String) = (this as? JsonObject)?.get(key)
}
