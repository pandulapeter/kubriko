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
 *   system prompt as its first prompt's leading text.
 * @param alwaysUsesHttpTools Whether the agent reaches Kubriko Creator's tools over HTTP even without advertising it.
 * @param keepsOffFiles Whether [sessionMeta] already turns off the agent's own edits and commands, so that what it asks
 *   to do goes to the user; otherwise its requests for tool calls that edit, delete, move, or run something are rejected
 *   ([PermissionHandler.rejectingChanges]).
 * @param usesProjectFolder Whether the agent works in the project's folder; otherwise it gets a folder of its own, since
 *   some agents write files of their own where they start (Google Antigravity its skills) without asking.
 */
internal data class AgentProfile(
    val sessionMeta: ((systemPrompt: String) -> JsonElement)?,
    val alwaysUsesHttpTools: Boolean,
    val keepsOffFiles: Boolean,
    val usesProjectFolder: Boolean,
) {
    companion object {
        val claude = AgentProfile(
            sessionMeta = ClaudeAgentOptions::sessionMeta,
            alwaysUsesHttpTools = true,
            keepsOffFiles = true,
            usesProjectFolder = true,
        )
        val fallback = AgentProfile(
            sessionMeta = null,
            alwaysUsesHttpTools = false,
            keepsOffFiles = false,
            usesProjectFolder = false,
        )
    }
}
