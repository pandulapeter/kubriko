/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import kotlin.test.Test
import kotlin.test.assertEquals

class AiClientSetupTest {
    private val usual: (AiClient) -> String = { it.defaultCommand }

    @Test
    fun withoutAliasesEveryToolHasItsUsualCommand() {
        assertEquals(AiClient.entries.map { AiClientSetup(it, it.defaultCommand) }, AiClientSetup.discover(emptyMap(), usual))
    }

    @Test
    fun aliasesThatRunAToolAreAddedNamedByWhatTellsThemApart() {
        val setups = AiClientSetup.discover(mapOf("codex-work" to "CODEX_HOME=~/.codex-work codex", "cx" to "codex", "ll" to "ls -l"), usual)
        assertEquals(
            listOf(AiClientSetup(AiClient.CODEX, "codex"), AiClientSetup(AiClient.CODEX, "codex-work", "work"), AiClientSetup(AiClient.CODEX, "cx", "cx")),
            setups.filter { it.client == AiClient.CODEX },
        )
    }

    @Test
    fun anAliasThatShadowsTheUsualCommandIsGonePastSoTheDefaultConfigurationCanStillBeConnected() {
        val shadowing = mapOf("claude" to "echo 'Use claude-personal'")
        val bypassing = AiClientSetup(AiClient.CLAUDE_CODE, "command claude")
        assertEquals(listOf(bypassing), AiClientSetup.discover(shadowing, usual).filter { it.client == AiClient.CLAUDE_CODE })
        assertEquals("claudeCode", bypassing.id)
        val withVariant = shadowing + ("claude-personal" to "CLAUDE_CONFIG_DIR=~/.claude-personal claude")
        assertEquals(
            listOf(bypassing, AiClientSetup(AiClient.CLAUDE_CODE, "claude-personal", "personal")),
            AiClientSetup.discover(withVariant, usual).filter { it.client == AiClient.CLAUDE_CODE },
        )
        val wrappingItself = mapOf("claude" to "CLAUDE_CONFIG_DIR=~/.other claude")
        assertEquals(listOf(AiClientSetup(AiClient.CLAUDE_CODE, "claude")), AiClientSetup.discover(wrappingItself, usual).filter { it.client == AiClient.CLAUDE_CODE })
    }

    @Test
    fun aReplacedCommandIsListedOnceNextToTheProgramsDefaultConfiguration() {
        val aliases = mapOf(
            "claude" to "echo 'Use claude-personal'",
            "claude-personal" to "CLAUDE_CONFIG_DIR=~/.claude-personal claude",
        )
        val replaced: (AiClient) -> String = { if (it == AiClient.CLAUDE_CODE) "claude-personal" else it.defaultCommand }
        val setups = AiClientSetup.discover(aliases, replaced).filter { it.client == AiClient.CLAUDE_CODE }
        assertEquals(listOf(AiClientSetup(AiClient.CLAUDE_CODE, "command claude"), AiClientSetup(AiClient.CLAUDE_CODE, "claude-personal", "personal")), setups)
        assertEquals(listOf("claudeCode", "claudeCode@claude-personal"), setups.map { it.id })
    }

    @Test
    fun idsSurviveARestartBeforeTheAliasesAreFoundAgain() {
        val personal = AiClientSetup(AiClient.CLAUDE_CODE, "claude-personal", "personal")
        assertEquals("claudeCode@claude-personal", personal.id)
        assertEquals(personal, AiClientSetup.fromId(personal.id))
        assertEquals(AiClientSetup(AiClient.CODEX, "codex"), AiClientSetup.fromId("codex"))
        assertEquals(null, AiClientSetup.fromId("nope"))
    }
}
