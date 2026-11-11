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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AntigravityMetaTest {

    private fun advertises(meta: String?) = AntigravityMeta.isAppendingAdvertised(AgentCapabilities(_meta = meta?.let(Json::parseToJsonElement)))

    @Test
    fun onlyAnAgentAdvertisingAppendingTakesTheSystemPromptSo() {
        assertTrue(advertises("""{"antigravity": {"tier": true, "systemPrompt": {"append": true}}}"""))
        assertFalse(advertises(null))
        assertFalse(advertises("""{"antigravity": {"tier": true}}"""))
        assertFalse(advertises("""{"antigravity": {"systemPrompt": {"append": false}}}"""))
        assertFalse(advertises("""{"antigravity": {"systemPrompt": {"append": "yes"}}}"""))
        assertFalse(advertises("""{"antigravity": {"systemPrompt": true}}"""))
        assertFalse(advertises("""{"antigravity": ["systemPrompt"]}"""))
        assertFalse(advertises("""{"systemPrompt": {"append": true}}"""))
    }

    @Test
    fun theFileToolsAreTurnedOffUnderTheAgentsOwnNamespaceWithTheSystemPromptAppendedWhenGiven() {
        assertEquals(
            Json.parseToJsonElement("""{"antigravity": {"systemPrompt": {"append": "Be helpful."}, "disabledTools": ["edit_file", "create_file", "run_command"]}}"""),
            AntigravityMeta.meta(capabilities("""{"antigravity": {"systemPrompt": {"append": true}}}"""), "Be helpful.") as JsonElement?,
        )
        assertEquals(
            Json.parseToJsonElement("""{"antigravity": {"disabledTools": ["edit_file", "create_file", "run_command"]}}"""),
            AntigravityMeta.meta(capabilities("""{"antigravity": {"tier": true}}"""), null) as JsonElement?,
        )
    }

    @Test
    fun anAgentWithoutTheExtensionGetsNoMeta() {
        assertNull(AntigravityMeta.meta(capabilities(null), "Be helpful."))
        assertNull(AntigravityMeta.meta(capabilities("""{"antigravity": true}"""), "Be helpful."))
        assertNull(AntigravityMeta.meta(capabilities("""{"other": {}}"""), "Be helpful."))
    }

    private fun capabilities(meta: String?) = AgentCapabilities(_meta = meta?.let(Json::parseToJsonElement))
}
