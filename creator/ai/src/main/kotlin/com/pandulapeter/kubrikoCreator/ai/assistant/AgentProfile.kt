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

import kotlinx.serialization.json.JsonElement

/**
 * What Kubriko Creator does for an agent beyond the fallbacks it chooses from the agent's `initialize` answer. Only an
 * agent checked in a real session gets a profile; every other one runs on [fallback].
 *
 * @param sessionMeta The `_meta` a conversation starts with, given the system prompt, or null for an agent that gets the
 *   system prompt as it advertises taking it ([AppendedSystemPrompt]), or else as its first prompt's leading text.
 * @param alwaysUsesHttpTools Whether the agent reaches Kubriko Creator's tools over HTTP even without advertising it.
 * @param keepsOffFiles Whether [sessionMeta] already turns off the agent's own edits and commands; otherwise its
 *   conversations are switched to its read-only mode where it offers one ([ReadOnlyMode]). Either way, what it asks to
 *   edit, delete, move, or run goes to the user ([PermissionRequest.isOutsideCreator]).
 * @param usesProjectFolder Whether the agent works in the project's folder; otherwise it gets a folder of its own, since
 *   some agents write files of their own where they start (Google Antigravity its skills) without asking.
 * @param usesAnthropicApiKey Whether the agent can sign in with the user's Anthropic API key ([ClaudeApiKey]).
 * @param environment Variables the agent is started with, under any the user's own configuration sets.
 */
public data class AgentProfile(
    val sessionMeta: ((systemPrompt: String) -> JsonElement)?,
    val alwaysUsesHttpTools: Boolean,
    val keepsOffFiles: Boolean,
    val usesProjectFolder: Boolean,
    val usesAnthropicApiKey: Boolean,
    val environment: Map<String, String> = emptyMap(),
) {
    internal companion object {
        internal val claude = AgentProfile(
            sessionMeta = ClaudeAgentOptions::sessionMeta,
            alwaysUsesHttpTools = true,
            keepsOffFiles = true,
            usesProjectFolder = true,
            usesAnthropicApiKey = true,
            // Claude Code gives up on a tool call after a minute, and ask_user waits for as long as the user takes.
            environment = mapOf("MCP_TOOL_TIMEOUT" to TOOL_CALL_TIMEOUT_IN_MILLISECONDS.toString()),
        )
        internal val fallback = AgentProfile(
            sessionMeta = null,
            alwaysUsesHttpTools = false,
            keepsOffFiles = false,
            usesProjectFolder = false,
            usesAnthropicApiKey = false,
        )

        /**
         * How long Claude Agent waits for one of Kubriko Creator's tools: a day, as long as nobody could mean to leave a
         * question unanswered and come back to it.
         */
        internal const val TOOL_CALL_TIMEOUT_IN_MILLISECONDS = 24L * 60 * 60 * 1000
    }
}
