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

import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AssistantChatTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val isSignedIn = AtomicBoolean(true)
    private val launcher = FakeAgentLauncher({ FakeAgent(isSignedIn = isSignedIn) })
    private val agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO)
    private val ended = CopyOnWriteArrayList<AgentConversation>()

    @AfterTest
    fun tearDown() {
        agent.close()
        launcher.close()
        scope.cancel()
        mainThread.close()
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { withTimeout(10.seconds) { block() } }

    private suspend fun readyChat() = newChat().also { chat -> chat.state.first { it == ChatState.Ready } }

    private suspend fun newChat() = withContext(mainThread) {
        AssistantChat(
            parentScope = scope,
            start = { permissions -> agent.newConversation(File("/projects/breakout"), permissions = permissions) },
            end = { conversation ->
                ended += conversation
                conversation.close()
            },
        )
    }

    private suspend fun AssistantChat.sendAndWait(text: String) {
        withContext(mainThread) { send(text) }
        isBusy.first { !it }
    }

    @Test
    fun theAnswerStreamsIntoTheTranscript() = test {
        val chat = readyChat()
        chat.sendAndWait("hello")
        assertEquals(
            listOf(TranscriptItem.UserMessage("hello"), TranscriptItem.AgentMessage("Hi there")),
            chat.transcript.value.items,
        )
    }

    @Test
    fun toolCallsAndPlansShowUp() = test {
        val chat = readyChat()
        chat.sendAndWait("tool")
        val toolCall = chat.transcript.value.items.filterIsInstance<TranscriptItem.ToolCall>().single()
        assertEquals("list_scenes", toolCall.creatorToolName)
        assertEquals(ToolCallStatus.COMPLETED, toolCall.status)
        assertEquals("List the Scenes", chat.transcript.value.plan.single().content)
        assertEquals(TranscriptItem.AgentMessage("Done"), chat.transcript.value.items.last())
    }

    @Test
    fun permissionRequestsWaitForTheUsersAnswer() = test {
        val chat = readyChat()
        withContext(mainThread) { chat.send("edit") }
        val request = chat.permissionRequest.first { it != null }!!
        assertEquals("Edit Game.kt", request.toolCall.title)
        withContext(mainThread) { chat.answer(request.options.first()) }
        chat.isBusy.first { !it }
        assertEquals(TranscriptItem.AgentMessage("allow"), chat.transcript.value.items.last())
        assertEquals(null, chat.permissionRequest.value)
    }

    @Test
    fun stoppingCancelsWhatTheAgentWaitsFor() = test {
        val chat = readyChat()
        withContext(mainThread) { chat.send("edit") }
        chat.permissionRequest.first { it != null }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(null, chat.permissionRequest.value)
        assertTrue(TranscriptItem.AgentMessage("allow") !in chat.transcript.value.items)
    }

    @Test
    fun stoppingEndsTheTurn() = test {
        val chat = readyChat()
        withContext(mainThread) { chat.send("wait") }
        chat.isBusy.first { it }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(TranscriptItem.TurnEnded(StopReason.CANCELLED), chat.transcript.value.items.last())
    }

    @Test
    fun nothingIsSentWhileATurnRunsOrForBlankText() = test {
        val chat = readyChat()
        withContext(mainThread) {
            chat.send("  ")
            chat.send("wait")
            chat.send("hello")
        }
        assertEquals(listOf<TranscriptItem>(TranscriptItem.UserMessage("wait")), chat.transcript.value.items)
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
    }

    @Test
    fun aConversationThatCouldntStartCanBeTriedAgain() = test {
        launcher.failingLaunches = 1
        val chat = newChat()
        val failed = chat.state.first { it is ChatState.Failed } as ChatState.Failed
        assertEquals(FailureReason.AGENT_NOT_STARTED, failed.reason)
        withContext(mainThread) { chat.retry() }
        chat.state.first { it == ChatState.Ready }
    }

    @Test
    fun configOptionsFollowTheAgent() = test {
        val chat = readyChat()
        val model = chat.configOptions.first { it.isNotEmpty() }.single() as SessionConfigOption.Select
        assertEquals("sonnet", model.currentValue.value)
        withContext(mainThread) { chat.setConfigOption(SessionConfigId(FakeAgent.MODEL), SessionConfigOptionValue.of("opus")) }
        chat.configOptions.first { (it.single() as SessionConfigOption.Select).currentValue.value == "opus" }
    }

    @Test
    fun slashCommandsAnnouncedAtTheStartAreKept() = test {
        val chat = readyChat()
        assertEquals(FakeAgent.COMPACT, chat.availableCommands.first { it.isNotEmpty() }.single().name)
    }

    @Test
    fun closingEndsTheConversation() = test {
        val chat = readyChat()
        withContext(mainThread) { chat.close() }
        assertTrue(ended.single().isLost.value)
    }

    @Test
    fun aPromptRefusedForWantOfASignInIsSentAgainAfterSigningIn() = test {
        isSignedIn.set(false)
        val chat = readyChat()
        chat.sendAndWait("hello")
        assertTrue(chat.transcript.value.needsSignIn)
        assertEquals(TranscriptItem.Failure(FailureReason.AUTH_REQUIRED), chat.transcript.value.items.last())
        isSignedIn.set(true)
        withContext(mainThread) { chat.resendAfterSignIn() }
        chat.isBusy.first { !it }
        assertEquals(listOf(TranscriptItem.UserMessage("hello"), TranscriptItem.AgentMessage("Hi there")), chat.transcript.value.items)
        assertEquals(listOf("hello", "hello"), launcher.agents.single().prompts)
    }

    @Test
    fun onlyARefusedPromptIsSentAgain() = test {
        val chat = readyChat()
        chat.sendAndWait("hello")
        withContext(mainThread) { chat.resendAfterSignIn() }
        assertFalse(chat.isBusy.value)
        assertEquals(listOf("hello"), launcher.agents.single().prompts)
    }
}
