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
import kotlin.test.assertEquals

class ClaudeConfigurationTest {

    @Test
    fun aliasesRunningClaudeWithVariablesOfTheirOwnAreConfigurations() {
        val aliases = mapOf(
            "claude" to "echo 'Use claude-personal'",
            "claude-personal" to "CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude",
            "claude-flashfood" to "CLAUDE_CONFIG_DIR=~/.claude-flashfood claude",
            "cc" to "claude --verbose",
            "codex-work" to "CODEX_HOME=~/.codex-work codex",
        )
        assertEquals(
            listOf(
                ClaudeConfiguration.default,
                ClaudeConfiguration("claude-flashfood", "flashfood", mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-flashfood")),
                ClaudeConfiguration("claude-personal", "personal", mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-personal")),
            ),
            ClaudeConfiguration.discover(aliases, home = "/Users/me"),
        )
    }

    @Test
    fun anAliasNamedOnlyByItsVariantKeepsItsName() {
        assertEquals("work", ClaudeConfiguration.ofAlias("work").variant)
        assertEquals("personal", ClaudeConfiguration.ofAlias("claude_personal").variant)
    }
}
