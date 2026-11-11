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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AntigravityMetaTest {

    private fun advertises(meta: String?) = AntigravityMeta.isAdvertised(AgentCapabilities(_meta = meta?.let(Json::parseToJsonElement)))

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
    fun theSystemPromptIsAppendedUnderTheAgentsOwnNamespaceWithItsToolsThatChangeFilesOff() {
        assertEquals(
            Json.parseToJsonElement("""{"antigravity": {"systemPrompt": {"append": "Be helpful."}, "disabledTools": ["edit_file", "create_file", "run_command"]}}"""),
            AntigravityMeta.meta("Be helpful.", isKeepingOffFiles = true),
        )
        assertEquals(
            Json.parseToJsonElement("""{"antigravity": {"systemPrompt": {"append": "Be helpful."}}}"""),
            AntigravityMeta.meta("Be helpful.", isKeepingOffFiles = false),
        )
    }
}
