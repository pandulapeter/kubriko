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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.context.RecordedCall
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellProcessRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Collections
import kotlin.io.path.createTempDirectory
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Runs [block] only when `KUBRIKO_CREATOR_CLAUDE_AGENT` is `true`, since a test with the real Claude Agent costs requests
 * on the user's subscription; `KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR` picks a Claude Code configuration other than the default.
 */
internal fun liveTest(timeout: Duration, block: suspend TestScope.() -> Unit) {
    assumeTrue(System.getenv("KUBRIKO_CREATOR_CLAUDE_AGENT") == "true")
    runTest(timeout = timeout) { block() }
}

/**
 * Runs [block] with a [LiveSession] on a fresh Breakout fixture, or on an empty project when [isBlank], closing it after.
 */
internal suspend fun <T> TestScope.withLiveSession(isBlank: Boolean = false, block: suspend (LiveSession) -> T): T {
    val session = LiveSession().apply { prepare(isBlank) }
    return try {
        block(session)
    } finally {
        session.close()
    }
}

/**
 * Sends [text] and waits for the turn to end, in real time: [runTest]'s own clock would let the timeout pass at once.
 */
internal suspend fun AssistantChat.ask(
    text: String,
    timeout: Duration = 5.minutes,
    model: SmallerModel? = null,
    chips: List<AttentionChip> = emptyList(),
    picture: ByteArray? = null,
) = withContext(Dispatchers.Default) {
    withTimeout(timeout) {
        state.first { it == ChatState.Ready }
        send(text, model = model) { AttentionChip.contentOf(chips) { picture } }
        isBusy.first { it }
        isBusy.first { !it }
    }
}

internal fun AssistantChat.lastAnswer() = transcript.value.items.filterIsInstance<TranscriptItem.AgentMessage>().lastOrNull()?.text.orEmpty()

/**
 * The real Claude Agent with Kubriko Creator's tools on a project of its own, in a temporary folder.
 */
internal class LiveSession {
    val mainThread = Dispatchers.Default
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    val projectDirectory = createTempDirectory().toFile()
    val projectPath: String = projectDirectory.path
    private val npx = File(assertNotNull(ShellProcessRunner().locate("npx"), "Node.js isn't installed."))
    private val configDirectory = System.getenv("KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR")
    lateinit var context: CommandContext
    lateinit var executor: CommandExecutor

    @Volatile
    var lastSummary: String? = null

    /**
     * Every tool call the agent made, oldest first, with its whole input.
     */
    val calls: MutableList<Call> = Collections.synchronizedList(mutableListOf())
    val toolServer = McpToolServer(executor = { executor }, callDispatcher = mainThread, version = "1.0.0") { toolName, arguments, isError ->
        calls += Call(RecordedCall(toolName, arguments), isError)
    }
    private var assistant = newAssistant()

    data class Call(
        val call: RecordedCall,
        val isError: Boolean,
    )

    @OptIn(EditorMutation::class)
    fun TestScope.prepare(isBlank: Boolean) {
        context = commandContext(*if (isBlank) emptyArray() else BreakoutFixture.types.toTypedArray(), rootDirectory = projectDirectory)
        if (!isBlank) context.scenes.write("level-1", BreakoutFixture.level)
        executor = CommandExecutor(context, testScope())
    }

    private fun newAssistant() = Assistant(
        agent = AcpAgent(
            command = RegistryAgent.npxCommand(RegistryAgent.claude.distribution as AgentDistribution.Npx, npx, AgentProfile.claude.environment + listOfNotNull(configDirectory?.let { "CLAUDE_CONFIG_DIR" to it })),
            launcher = ProcessAgentLauncher(projectDirectory),
            clientInfo = AcpAgent.clientInfo("1.0.0"),
            scope = scope,
            ioDispatcher = Dispatchers.IO,
        ),
        toolServer = toolServer,
        ioDispatcher = Dispatchers.IO,
        profile = AgentProfile.claude,
        systemPrompt = AssistantSystemPrompt::withProject,
    )

    suspend fun chat(resuming: String? = null, rules: String? = null, projectContext: String? = null) = withContext(mainThread) {
        val owner = assistant
        AssistantChat(
            parentScope = scope,
            start = { permissions, onHistory ->
                if (resuming == null) {
                    owner.newConversation(projectDirectory, permissions, projectContext, rules)
                } else {
                    owner.resumeConversation(projectDirectory, resuming, permissions, onHistory, rules)
                }
            },
            end = owner::closeConversation,
            toolsIdOf = owner::toolsIdOf,
            project = OpenProjectAccess.of { executor },
            onTurnEnded = { turn -> turn.toolsId?.let(context.summaries::take)?.let { lastSummary = it } },
        )
    }

    fun restartAgent() {
        assistant.close()
        assistant = newAssistant()
    }

    fun close() {
        assistant.close()
        scope.cancel()
        projectDirectory.deleteRecursively()
    }
}
