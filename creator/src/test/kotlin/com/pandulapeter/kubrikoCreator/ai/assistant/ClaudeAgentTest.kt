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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClaudeAgentTest {

    @Test
    fun claudeRunsThroughNpxWithItsFolderOnThePath() {
        val npx = File("/opt/homebrew/bin/npx")
        val command = RegistryAgent.npxCommand(RegistryAgent.claude.distribution as AgentDistribution.Npx, npx, mapOf("CLAUDE_CONFIG_DIR" to "/Users/someone/.claude-personal"))
        assertEquals("/opt/homebrew/bin/npx", command.program)
        assertEquals(listOf("--yes", "@agentclientprotocol/claude-agent-acp@0.99.0"), command.arguments)
        assertTrue(command.environment.getValue("PATH").startsWith("/opt/homebrew/bin${File.pathSeparator}"))
        assertEquals("/Users/someone/.claude-personal", command.environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun claudesConversationsAppendTheSystemPromptAndUseOnlyCreatorsToolsForChanges() {
        val meta = ClaudeAgentOptions.sessionMeta("Be helpful.")
        assertEquals(JsonPrimitive("Be helpful."), meta.getValue("systemPrompt").jsonObject.getValue("append"))
        val options = meta.getValue("claudeCode").jsonObject.getValue("options").jsonObject
        assertEquals(Json.parseToJsonElement("""["Bash", "Write", "Edit", "NotebookEdit"]"""), options.getValue("disallowedTools"))
        assertEquals(Json.parseToJsonElement("""["mcp__kubriko-creator"]"""), options.getValue("allowedTools"))
    }

    @Test
    fun claudeHasASubagentThatExploresTheGameWithReadToolsAlone() {
        val explorer = ClaudeAgentOptions.sessionMeta("").getValue("claudeCode").jsonObject.getValue("options").jsonObject
            .getValue("agents").jsonObject.getValue(ClaudeAgentOptions.EXPLORER_NAME).jsonObject
        val tools = explorer.getValue("tools").jsonArray.map { it.jsonPrimitive.content }
        assertContains(tools, "mcp__kubriko-creator__get_relations")
        assertTrue(tools.none { it.endsWith("__add_block") || it.endsWith("__start_game") })
        assertTrue(explorer.getValue("description").jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun theSystemPromptExplainsCreatorAndCarriesTheBlockCatalog() {
        val prompt = AssistantSystemPrompt.text
        assertContains(prompt, "Make every change through these tools")
        assertContains(prompt, GetBlockCatalog.catalog.toString())
        assertTrue(prompt.length < SYSTEM_PROMPT_BUDGET_IN_CHARACTERS, "The system prompt grew to ${prompt.length} characters.")
    }

    @Test
    fun theProjectsDigestComesAfterThePromptThatStaysTheSame() {
        assertEquals(AssistantSystemPrompt.text, AssistantSystemPrompt.withProject(null))
        val prompt = AssistantSystemPrompt.withProject("Project breakout.")
        assertTrue(prompt.startsWith(AssistantSystemPrompt.text))
        assertTrue(prompt.endsWith("Project breakout."))
    }

    private companion object {
        /**
         * About 10,000 tokens, which the system prompt costs on every request that misses the prompt cache.
         */
        const val SYSTEM_PROMPT_BUDGET_IN_CHARACTERS = 40_000
    }
}
