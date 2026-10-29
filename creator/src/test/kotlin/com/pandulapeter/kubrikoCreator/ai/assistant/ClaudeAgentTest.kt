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

import com.pandulapeter.kubrikoCreator.commands.actorTypes.GetBlockCatalog
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClaudeAgentTest {

    @Test
    fun claudeRunsThroughNpxWithItsFolderOnThePath() {
        val npx = File("/opt/homebrew/bin/npx")
        val command = RegistryAgent.claude.command(npx, mapOf("CLAUDE_CONFIG_DIR" to "/Users/someone/.claude-personal"))
        assertEquals("/opt/homebrew/bin/npx", command.program)
        assertEquals(listOf("--yes", "@agentclientprotocol/claude-agent-acp@0.99.0"), command.arguments)
        assertTrue(command.environment.getValue("PATH").startsWith("/opt/homebrew/bin${File.pathSeparator}"))
        assertEquals("/Users/someone/.claude-personal", command.environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun claudesConversationsAppendTheSystemPromptAndUseOnlyCreatorsToolsForChanges() {
        val meta = ClaudeAgentOptions.sessionMeta("Be helpful.")
        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "systemPrompt": {"append": "Be helpful."},
                  "claudeCode": {
                    "options": {
                      "disallowedTools": ["Bash", "Write", "Edit", "NotebookEdit"],
                      "allowedTools": ["mcp__kubriko-creator"]
                    }
                  }
                }
                """,
            ),
            meta,
        )
    }

    @Test
    fun theSystemPromptExplainsCreatorAndCarriesTheBlockCatalog() {
        val prompt = AssistantSystemPrompt.text
        assertContains(prompt, "Make every change through these tools")
        assertContains(prompt, GetBlockCatalog.catalog.toString())
        assertTrue(prompt.length < SYSTEM_PROMPT_BUDGET_IN_CHARACTERS, "The system prompt grew to ${prompt.length} characters.")
    }

    private companion object {
        /**
         * About 10,000 tokens, which the system prompt costs on every request that misses the prompt cache.
         */
        const val SYSTEM_PROMPT_BUDGET_IN_CHARACTERS = 40_000
    }
}
