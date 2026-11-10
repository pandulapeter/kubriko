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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentProfileTest {
    private val untested = AgentChoice.Registry(RegistryAgent("codex-acp", "Codex", "2.4.0", AgentDistribution.Npx("@zed-industries/codex-acp@2.4.0")))

    @Test
    fun onlyClaudeAgentTakesAnAnthropicApiKey() {
        assertTrue(AgentChoice.Registry(RegistryAgent.claude).profile.usesAnthropicApiKey)
        assertFalse(untested.profile.usesAnthropicApiKey)
        assertFalse(AgentChoice.Custom("goose acp").profile.usesAnthropicApiKey)
    }

    @Test
    fun onlyATestedRegistryAgentIsTested() {
        assertTrue(AgentChoice.Registry(RegistryAgent.claude).isTested)
        assertFalse(untested.isTested)
        assertFalse(AgentChoice.Custom("goose acp").isTested)
    }
}
