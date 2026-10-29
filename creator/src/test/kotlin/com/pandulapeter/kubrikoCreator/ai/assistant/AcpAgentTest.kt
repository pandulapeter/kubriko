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

import com.agentclientprotocol.model.AuthMethodId
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.HttpHeader
import com.agentclientprotocol.model.McpServer
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class AcpAgentTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val launchers = mutableListOf<FakeAgentLauncher>()
    private val agents = mutableListOf<AcpAgent>()
    private val timeSource = TestTimeSource()
    private val workingDirectory = File("/projects/breakout")

    @AfterTest
    fun tearDown() {
        agents.forEach { it.close() }
        launchers.forEach { it.close() }
        scope.cancel()
    }

    private fun launcher(fakeAgent: () -> FakeAgent = { FakeAgent() }) = FakeAgentLauncher(fakeAgent).also { launchers += it }

    private fun agent(launcher: FakeAgentLauncher) = AcpAgent(
        command = AgentCommand("fake-agent", listOf("--acp")),
        launcher = launcher,
        clientInfo = AcpAgent.clientInfo("1.2.3"),
        scope = scope,
        ioDispatcher = Dispatchers.IO,
        timeSource = timeSource,
    ).also { agents += it }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking { withTimeout(TIMEOUT) { block() } }

    private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean) = withTimeout(TIMEOUT) { first(predicate) }

    private suspend fun awaitTrue(condition: () -> Boolean) = withTimeout(TIMEOUT) {
        while (!condition()) delay(10)
    }

    /**
     * Waits for [messages] to have caught up with [expected], since they are collected on another thread than the prompt's.
     */
    private suspend fun assertReceived(expected: List<String>, messages: List<String>) {
        awaitTrue { messages.size >= expected.size }
        assertEquals(expected, messages)
    }

    private fun messagesOf(conversation: AgentConversation) = CopyOnWriteArrayList<String>().also { messages ->
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            conversation.updates.collect { update ->
                ((update as? SessionUpdate.AgentMessageChunk)?.content as? ContentBlock.Text)?.let { messages += it.text }
            }
        }
    }

    private fun text(text: String) = listOf(ContentBlock.Text(text))

    @Test
    fun startingInitializesWithProtocolVersion1AndNeitherFileSystemNorTerminal() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        assertEquals(AgentState.Stopped, agent.state.value)
        val info = agent.start()
        assertEquals(FakeAgent.AGENT_LOGIN, info.authMethods.first().id.value)
        assertEquals(AgentState.Running(info), agent.state.value)
        assertEquals(AgentCommand("fake-agent", listOf("--acp")), launcher.commands.single())
        val clientInfo = launcher.agents.single().clientInfos.single()
        assertEquals(1, clientInfo.protocolVersion)
        assertNull(clientInfo.capabilities.fs)
        assertFalse(clientInfo.capabilities.terminal)
        assertEquals(JsonPrimitive(true), clientInfo.capabilities._meta?.jsonObject?.get("terminal-auth"))
        assertEquals("kubriko-creator", clientInfo.implementation?.name)
        assertEquals("1.2.3", clientInfo.implementation?.version)
        agent.errorOutput.await { it == listOf("Fake agent started.") }
        agent.start()
        assertEquals(1, launcher.commands.size)
    }

    @Test
    fun aPromptStreamsTheAgentsMessagesAndEndsWithItsTurn() = test {
        val launcher = launcher()
        val mcpServer = McpServer.Http("kubriko-creator", "http://127.0.0.1:7781/mcp/conversations/1", listOf(HttpHeader("Authorization", "Bearer token")))
        val meta = buildJsonObject { put("claudeCode", JsonPrimitive("options")) }
        val conversation = agent(launcher).newConversation(workingDirectory, listOf(mcpServer), meta)
        val messages = messagesOf(conversation)
        assertEquals(StopReason.END_TURN, conversation.prompt(text("hello")).stopReason)
        assertReceived(listOf("Hi", " there"), messages)
        val parameters = launcher.agents.single().newSessions.single()
        assertEquals(workingDirectory.absolutePath, parameters.cwd)
        assertEquals(listOf<McpServer>(mcpServer), parameters.mcpServers)
        assertEquals(meta, parameters._meta)
    }

    @Test
    fun aPromptsOwnUpdatesReachItsCallerBeforeItReturns() = test {
        val agent = agent(launcher())
        val conversation = agent.newConversation(workingDirectory)
        val messages = messagesOf(conversation)
        val received = mutableListOf<String>()
        conversation.prompt(text("hello")) { update -> ((update as? SessionUpdate.AgentMessageChunk)?.content as? ContentBlock.Text)?.let { received += it.text } }
        assertEquals(listOf("Hi", " there"), received)
        conversation.prompt(text("later"))
        assertReceived(listOf(FakeAgent.LATER), messages)
    }

    @Test
    fun permissionRequestsAreRejectedUnlessTheHandlerAllowsThem() = test {
        val agent = agent(launcher())
        val rejecting = agent.newConversation(workingDirectory)
        val rejected = messagesOf(rejecting)
        rejecting.prompt(text("edit"))
        assertReceived(listOf("reject"), rejected)
        val allowing = agent.newConversation(
            workingDirectory,
            permissions = { _, options -> RequestPermissionOutcome.Selected(options.first { it.kind == PermissionOptionKind.ALLOW_ONCE }.optionId) },
        )
        val allowed = messagesOf(allowing)
        allowing.prompt(text("edit"))
        assertReceived(listOf("allow"), allowed)
        val cancelling = agent.newConversation(workingDirectory, permissions = { _, _ -> RequestPermissionOutcome.Cancelled })
        val cancelled = messagesOf(cancelling)
        cancelling.prompt(text("edit"))
        assertReceived(listOf("cancelled"), cancelled)
    }

    @Test
    fun cancellingEndsTheTurnAsCancelled() = test {
        val launcher = launcher()
        val conversation = agent(launcher).newConversation(workingDirectory)
        val response = async { conversation.prompt(text("wait")) }
        awaitTrue { launcher.agents.single().prompts.isNotEmpty() }
        conversation.cancel()
        assertEquals(StopReason.CANCELLED, response.await().stopReason)
    }

    @Test
    fun updatesBetweenPromptsReachTheConversation() = test {
        val conversation = agent(launcher()).newConversation(workingDirectory)
        val messages = messagesOf(conversation)
        conversation.prompt(text("later"))
        awaitTrue { messages == listOf(FakeAgent.LATER) }
    }

    @Test
    fun aCrashedAgentIsStartedAgainWithItsConversationsLoaded() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        val conversation = agent.newConversation(workingDirectory)
        val messages = messagesOf(conversation)
        assertFailsWith<Exception> { conversation.prompt(text("crash")) }
        awaitTrue { launcher.agents.size == 2 }
        agent.state.await { it is AgentState.Running }
        assertEquals(listOf(conversation.id), launcher.agents[1].loadedSessions)
        conversation.prompt(text("hello"))
        assertReceived(listOf("Hi", " there"), messages)
        assertFalse(conversation.isLost.value)
        assertEquals(listOf("Fake agent started."), agent.errorOutput.value)
    }

    @Test
    fun conversationsTheAgentCantLoadAreLost() = test {
        val launcher = launcher { FakeAgent(supportsLoading = false) }
        val agent = agent(launcher)
        val conversation = agent.newConversation(workingDirectory)
        assertFailsWith<Exception> { conversation.prompt(text("crash")) }
        conversation.isLost.await { it }
        assertFailsWith<ConversationLostException> { conversation.prompt(text("hello")) }
        assertEquals(2, launcher.agents.size)
        assertTrue(launcher.agents[1].loadedSessions.isEmpty())
    }

    @Test
    fun anAgentWithoutConversationsIsntStartedAgainUntilUsed() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        agent.start()
        launcher.channels.single().crash(1)
        agent.state.await { it == AgentState.Stopped }
        assertEquals(1, launcher.agents.size)
        agent.newConversation(workingDirectory).prompt(text("hello"))
        assertEquals(2, launcher.agents.size)
    }

    @Test
    fun anAgentCrashingRightAfterEachStartFailsUntilUsedAgain() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        val conversation = agent.newConversation(workingDirectory)
        repeat(4) { crash ->
            awaitTrue { launcher.agents.size == crash + 1 && agent.state.value is AgentState.Running }
            launcher.channels.last().crash(7)
        }
        val failed = agent.state.await { it is AgentState.Failed }
        assertEquals(7, (failed as AgentState.Failed).exitCode)
        assertEquals(4, launcher.agents.size)
        conversation.prompt(text("hello"))
        assertEquals(5, launcher.agents.size)
        assertEquals(listOf(conversation.id), launcher.agents.last().loadedSessions)
    }

    @Test
    fun anAgentThatRanLongEnoughIsAlwaysStartedAgain() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        agent.newConversation(workingDirectory)
        repeat(6) { crash ->
            awaitTrue { launcher.agents.size == crash + 1 && agent.state.value is AgentState.Running }
            timeSource += 31.seconds
            launcher.channels.last().crash(1)
        }
        awaitTrue { launcher.agents.size == 7 }
        agent.state.await { it is AgentState.Running }
    }

    @Test
    fun anAgentExitingBeforeItAnswersFailsWithItsExitCodeAndErrors() = test {
        val launcher = launcher { FakeAgent(exitsAtStart = 2) }
        val agent = agent(launcher)
        val exception = assertFailsWith<AgentStartException> { agent.start() }
        assertEquals(2, exception.exitCode)
        assertIs<AgentState.Failed>(agent.state.value)
        assertEquals(2, (agent.state.value as AgentState.Failed).exitCode)
        assertFailsWith<AgentStartException> { agent.start() }
        assertEquals(2, launcher.agents.size)
    }

    @Test
    fun aProgramThatCantBeStartedFailsWithoutAnExitCode() = test {
        val launcher = launcher().apply { failingLaunches = 1 }
        val agent = agent(launcher)
        assertNull(assertFailsWith<AgentStartException> { agent.start() }.exitCode)
        assertEquals(AgentState.Failed(null, emptyList()), agent.state.value)
        agent.start()
        assertIs<AgentState.Running>(agent.state.value)
    }

    @Test
    fun cancellingAStartThatHangsStopsTheAgent() = test {
        val launcher = launcher { FakeAgent(answersInitialize = false) }
        val agent = agent(launcher)
        assertNull(withTimeoutOrNull(200) { agent.start() })
        assertTrue(launcher.channels.single().isStopped)
    }

    @Test
    fun aClosedConversationCantBeContinued() = test {
        val conversation = agent(launcher()).newConversation(workingDirectory)
        conversation.close()
        assertTrue(conversation.isLost.value)
        assertFailsWith<ConversationLostException> { conversation.prompt(text("hello")) }
    }

    @Test
    fun closingTheAgentStopsItsProcess() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        agent.start()
        agent.close()
        assertTrue(launcher.channels.single().isStopped)
        assertEquals(AgentState.Stopped, agent.state.value)
        assertFailsWith<IllegalStateException> { agent.start() }
        delay(100)
        assertEquals(1, launcher.agents.size)
    }

    @Test
    fun theAgentsSignInMethodsAndIdentityAreKept() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        agent.start()
        val methods = agent.signInMethods.value
        assertEquals(listOf(FakeAgent.AGENT_LOGIN, "terminal-login"), methods.map { it.id.value })
        assertEquals(FakeAgent.TERMINAL_LOGIN, methods.last().terminal?.program)
        assertNull(agent.authStatus.value)
        launcher.channels.single().pushAuthStatus("account", "Claude Max", "me@example.com")
        assertEquals(AuthStatus(AuthStatus.Kind.ACCOUNT, "Claude Max", email = "me@example.com"), agent.authStatus.await { it != null })
    }

    @Test
    fun agentSignInIsAskedOfTheAgent() = test {
        val launcher = launcher()
        val agent = agent(launcher)
        agent.authenticate(AuthMethodId(FakeAgent.AGENT_LOGIN))
        assertEquals(listOf(AuthMethodId(FakeAgent.AGENT_LOGIN)), launcher.agents.single().authenticated)
    }

    private companion object {
        val TIMEOUT = 10.seconds
    }
}
