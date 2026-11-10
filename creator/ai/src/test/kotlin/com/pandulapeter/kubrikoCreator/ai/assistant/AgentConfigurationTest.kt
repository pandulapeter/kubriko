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

import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentConfigurationTest {
    private val aliases = mapOf(
        "claude" to "echo 'Use claude-personal'",
        "claude-personal" to "CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude",
        "claude-flashfood" to "CLAUDE_CONFIG_DIR=~/.claude-flashfood claude",
        "cc" to "claude --verbose",
        "codex-work" to "CODEX_HOME=~/.codex-work codex",
        "cx" to "codex --full-auto",
    )

    @Test
    fun aliasesRunningClaudeWithVariablesOfTheirOwnAreClaudeCodesConfigurations() {
        assertEquals(
            listOf(
                AgentConfiguration(AiClient.CLAUDE_CODE),
                AgentConfiguration(AiClient.CLAUDE_CODE, "claude-flashfood", "flashfood", mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-flashfood")),
                AgentConfiguration(AiClient.CLAUDE_CODE, "claude-personal", "personal", mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-personal")),
            ),
            AgentConfiguration.discover(AiClient.CLAUDE_CODE, aliases, home = "/Users/me"),
        )
    }

    @Test
    fun aliasesRunningCodexWithVariablesOfTheirOwnAreCodexsConfigurations() {
        assertEquals(
            listOf(
                AgentConfiguration(AiClient.CODEX),
                AgentConfiguration(AiClient.CODEX, "codex-work", "work", mapOf("CODEX_HOME" to "/Users/me/.codex-work")),
            ),
            AgentConfiguration.discover(AiClient.CODEX, aliases, home = "/Users/me"),
        )
    }

    @Test
    fun anAliasNamedOnlyByItsVariantKeepsItsName() {
        assertEquals("work", AgentConfiguration.ofAlias(AiClient.CLAUDE_CODE, "work").variant)
        assertEquals("personal", AgentConfiguration.ofAlias(AiClient.CLAUDE_CODE, "claude_personal").variant)
        assertEquals("work", AgentConfiguration.ofAlias(AiClient.CODEX, "codex.work").variant)
    }

    @Test
    fun onlyAgentsOfAKnownToolHaveConfigurations() {
        assertEquals(AiClient.CLAUDE_CODE, AgentConfiguration.clientOf(AgentChoice.Registry(RegistryAgent.claude)))
        assertEquals(AiClient.CODEX, AgentConfiguration.clientOf(AgentChoice.Registry(RegistryAgent("codex-acp", "Codex", "2.4.0", RegistryAgent.claude.distribution))))
        assertNull(AgentConfiguration.clientOf(AgentChoice.Registry(RegistryAgent("gemini", "Gemini CLI", "1.0.0", RegistryAgent.claude.distribution))))
        assertNull(AgentConfiguration.clientOf(AgentChoice.Custom("CODEX_HOME=~/.codex-work codex-acp")))
    }
}
