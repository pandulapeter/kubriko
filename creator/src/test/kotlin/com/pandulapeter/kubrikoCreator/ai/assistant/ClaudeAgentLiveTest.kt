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

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellProcessRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Runs the real Claude Agent from the ACP Registry through the user's `npx`, signed in as Claude Code already is, and has it
 * call one of Kubriko Creator's tools. It costs a request, so it only runs when the `KUBRIKO_CREATOR_CLAUDE_AGENT`
 * environment variable is `true`; `KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR` picks a Claude Code configuration other than the
 * default one.
 */
class ClaudeAgentLiveTest {

    @Test
    fun claudeAgentCallsCreatorsToolsAndNothingElse() {
        if (System.getenv("KUBRIKO_CREATOR_CLAUDE_AGENT") != "true") return
        val npx = assertNotNull(ShellProcessRunner().locate("npx"), "Node.js isn't installed.")
        val configDirectory = System.getenv("KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR")
        val projectDirectory = createTempDirectory().toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val toolServer = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0")
        val assistant = Assistant(
            agent = AcpAgent(
                command = RegistryAgent.claude.command(File(npx), listOfNotNull(configDirectory?.let { "CLAUDE_CONFIG_DIR" to it }).toMap()),
                launcher = ProcessAgentLauncher(projectDirectory),
                clientInfo = AcpAgent.clientInfo("1.0.0"),
                scope = scope,
                ioDispatcher = Dispatchers.IO,
            ),
            toolServer = toolServer,
            ioDispatcher = Dispatchers.IO,
            sessionMeta = ClaudeAgentOptions.sessionMeta(AssistantSystemPrompt.text),
        )
        try {
            runBlocking(Dispatchers.IO) {
                withTimeout(5.minutes) {
                    val conversation = assistant.newConversation(projectDirectory)
                    val toolCalls = CopyOnWriteArrayList<String>()
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        conversation.updates.collect { update -> if (update is SessionUpdate.ToolCall) toolCalls += update.title }
                    }
                    val response = conversation.prompt(
                        listOf(ContentBlock.Text("Call the list_scenes tool exactly once, then run `touch created.txt` with Bash, then reply with one word.")),
                    )
                    println("Tool calls: $toolCalls")
                    assertEquals(StopReason.END_TURN, response.stopReason)
                    assertEquals(listOf("list_scenes"), toolServer.calls.value.map { it.toolName })
                    assertTrue(!File(projectDirectory, "created.txt").exists(), "Claude ran a shell command.")
                }
            }
        } finally {
            assistant.close()
            scope.cancel()
            projectDirectory.deleteRecursively()
        }
    }
}
