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
import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallStatus
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.ChangeLog
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssistantChatTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val isSignedIn = AtomicBoolean(true)
    private val launcher = FakeAgentLauncher({ FakeAgent(isSignedIn = isSignedIn) })
    private val agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO)
    private val ended = CopyOnWriteArrayList<AgentConversation>()
    private val forgotten = CopyOnWriteArrayList<AgentConversation>()

    @AfterTest
    fun tearDown() {
        agent.close()
        launcher.close()
        scope.cancel()
        mainThread.close()
    }

    private suspend fun readyChat() = newChat().also { chat -> chat.state.first { it == ChatState.Ready } }

    private val undoneTurns = CopyOnWriteArrayList<Pair<String, Int>>()

    private suspend fun newChat(changeLog: ChangeLog? = null) = withContext(mainThread) {
        AssistantChat(
            parentScope = scope,
            start = { permissions, _ -> agent.newConversation(File("/projects/breakout"), permissions = permissions) },
            end = { conversation ->
                ended += conversation
                conversation.close()
            },
            toolsIdOf = { "tools" },
            forgetReads = { forgotten += it },
            changeLog = { changeLog },
            undoTurn = { toolsId, sinceRevision ->
                undoneTurns += toolsId to sinceRevision
                0
            },
        )
    }

    private suspend fun AssistantChat.sendAndWait(text: String) {
        withContext(mainThread) { send(text) }
        isBusy.first { !it }
    }

    @Test
    fun theUsersChangesSinceTheLastTurnAreSummedUpBeforeTheNextMessage() = blockingTest {
        val changeLog = ChangeLog()
        changeLog.record("add_block", CommandTarget.BlockType("Ball"))
        val chat = newChat(changeLog).also { chat -> chat.state.first { it == ChatState.Ready } }
        chat.sendAndWait("hello")
        changeLog.record("add_block", CommandTarget.BlockType("Ball", 1, 0))
        changeLog.record("add_block", CommandTarget.BlockType("Ball", 1, 1), origin = AiStepOrigin("tools"), isByAi = true)
        chat.sendAndWait("hello")
        chat.sendAndWait("hello")
        val summary = launcher.agents.single().leadingTexts.single()
        assertContains(summary, "add_block at Ball#1.0")
        assertFalse("Ball#1.1" in summary)
        assertEquals(listOf("hello", "hello", "hello"), launcher.agents.single().prompts)
    }

    @Test
    fun theAnswerStreamsIntoTheTranscript() = blockingTest {
        val chat = readyChat()
        chat.sendAndWait("hello")
        assertEquals(
            listOf(TranscriptItem.UserMessage("hello"), TranscriptItem.AgentMessage("Hi there")),
            chat.transcript.value.items,
        )
    }

    @Test
    fun toolCallsAndPlansShowUp() = blockingTest {
        val chat = readyChat()
        chat.sendAndWait("tool")
        val toolCall = chat.transcript.value.items.filterIsInstance<TranscriptItem.ToolCall>().single()
        assertEquals("list_scenes", toolCall.creatorToolName)
        assertEquals(ToolCallStatus.COMPLETED, toolCall.status)
        assertEquals("List the Scenes", chat.transcript.value.plan.single().content)
        assertEquals(TranscriptItem.AgentMessage("Done"), chat.transcript.value.items.last())
    }

    @Test
    fun permissionRequestsWaitForTheUsersAnswer() = blockingTest {
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
    fun stoppingCancelsWhatTheAgentWaitsFor() = blockingTest {
        val chat = readyChat()
        withContext(mainThread) { chat.send("edit") }
        chat.permissionRequest.first { it != null }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(null, chat.permissionRequest.value)
        assertTrue(TranscriptItem.AgentMessage("allow") !in chat.transcript.value.items)
    }

    @Test
    fun stoppingEndsTheTurn() = blockingTest {
        val chat = readyChat()
        withContext(mainThread) { chat.send("wait") }
        chat.isBusy.first { it }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(TranscriptItem.TurnEnded(StopReason.CANCELLED), chat.transcript.value.items.last())
    }

    @Test
    fun aTurnStoppedBeforeItStartsEndsTheBusyState() = blockingTest {
        val chat = readyChat()
        withContext(mainThread) {
            chat.send("wait")
            chat.stop()
        }
        chat.isBusy.first { !it }
        assertEquals(TranscriptItem.TurnEnded(StopReason.CANCELLED), chat.transcript.value.items.last())
        assertTrue(launcher.agents.single().prompts.isEmpty())
    }

    @Test
    fun aCancelledTurnForgetsWhatTheConversationRead() = blockingTest {
        val chat = readyChat()
        withContext(mainThread) { chat.send("wait") }
        while ("wait" !in launcher.agents.single().prompts) delay(10)
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(listOf(chat.conversationId.value), forgotten.map { it.id.value })
    }

    @Test
    fun aTurnStoppedBeforeItIsSentForgetsNothing() = blockingTest {
        val chat = readyChat()
        val context = CompletableDeferred<List<ContentBlock>>()
        withContext(mainThread) { chat.send("wait") { context.await() } }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(TranscriptItem.TurnEnded(StopReason.CANCELLED), chat.transcript.value.items.last())
        assertTrue(forgotten.isEmpty())
    }

    @Test
    fun aFinishedTurnForgetsNothing() = blockingTest {
        readyChat().sendAndWait("hello")
        assertTrue(forgotten.isEmpty())
    }

    @Test
    fun nothingIsSentWhileATurnRunsOrForBlankText() = blockingTest {
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
    fun aConversationThatCouldntStartCanBeTriedAgain() = blockingTest {
        launcher.failingLaunches = 1
        val chat = newChat()
        val failed = chat.state.first { it is ChatState.Failed } as ChatState.Failed
        assertEquals(FailureReason.AGENT_NOT_STARTED, failed.reason)
        withContext(mainThread) { chat.retry() }
        chat.state.first { it == ChatState.Ready }
    }

    @Test
    fun configOptionsFollowTheAgent() = blockingTest {
        val chat = readyChat()
        val model = chat.configOptions.first { it.isNotEmpty() }.single() as SessionConfigOption.Select
        assertEquals("sonnet", model.currentValue.value)
        withContext(mainThread) { chat.setConfigOption(SessionConfigId(FakeAgent.MODEL), SessionConfigOptionValue.of("opus")) }
        chat.configOptions.first { (it.single() as SessionConfigOption.Select).currentValue.value == "opus" }
    }

    @Test
    fun slashCommandsAnnouncedAtTheStartAreKept() = blockingTest {
        val chat = readyChat()
        assertEquals(FakeAgent.COMPACT, chat.availableCommands.first { it.isNotEmpty() }.single().name)
    }

    @Test
    fun closingEndsTheConversation() = blockingTest {
        val chat = readyChat()
        withContext(mainThread) { chat.close() }
        assertTrue(ended.single().isLost.value)
    }

    @Test
    fun aPromptRefusedForWantOfASignInIsSentAgainAfterSigningIn() = blockingTest {
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
    fun anAgentThatStopsMidTurnEndsItWithAFailureAndTheConversationGoesOn() = blockingTest {
        val chat = readyChat()
        chat.sendAndWait("crash")
        assertEquals(TranscriptItem.Failure(FailureReason.AGENT_STOPPED), chat.transcript.value.items.last())
        chat.sendAndWait("hello")
        assertEquals(TranscriptItem.AgentMessage("Hi there"), chat.transcript.value.items.last())
    }

    @Test
    fun whatTheUserLooksAtGoesAheadOfTheirMessage() = blockingTest {
        val chat = readyChat()
        withContext(mainThread) { chat.send("hello") { listOf(AttentionChip.Scene("level-1").toContent()) } }
        chat.isBusy.first { !it }
        assertEquals<List<ContentBlock>>(listOf(AttentionChip.Scene("level-1").toContent()), launcher.agents.single().leadingBlocks)
        assertEquals(listOf("hello"), launcher.agents.single().prompts)
    }

    @Test
    fun aMessageSentWhileItsContextIsGatheredIsntLost() = blockingTest {
        val chat = readyChat()
        val context = CompletableDeferred<List<ContentBlock>>()
        withContext(mainThread) {
            chat.send("first") { context.await() }
            assertTrue(chat.isBusy.value)
            chat.send("second")
        }
        assertEquals(listOf(TranscriptItem.UserMessage("first")), chat.transcript.value.items)
        context.complete(emptyList())
        chat.isBusy.first { !it }
        assertEquals(listOf("first"), launcher.agents.single().prompts)
    }

    @Test
    fun stoppingWhileTheContextIsGatheredSendsNothing() = blockingTest {
        val chat = readyChat()
        val context = CompletableDeferred<List<ContentBlock>>()
        withContext(mainThread) { chat.send("first") { context.await() } }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        assertEquals(listOf(TranscriptItem.UserMessage("first"), TranscriptItem.TurnEnded(StopReason.CANCELLED)), chat.transcript.value.items)
        chat.sendAndWait("second")
        assertEquals(listOf("second"), launcher.agents.single().prompts)
    }

    @Test
    fun theChangesATurnStoppedWhileItsContextIsGatheredDidntSendAreSummedUpNextTime() = blockingTest {
        val changeLog = ChangeLog()
        changeLog.record("add_block", CommandTarget.BlockType("Ball"))
        val chat = newChat(changeLog).also { chat -> chat.state.first { it == ChatState.Ready } }
        changeLog.record("add_block", CommandTarget.BlockType("Ball", 1, 0))
        val context = CompletableDeferred<List<ContentBlock>>()
        withContext(mainThread) { chat.send("first") { context.await() } }
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        chat.sendAndWait("second")
        assertContains(launcher.agents.single().leadingTexts.single(), "add_block at Ball#1.0")
        assertEquals(listOf("second"), launcher.agents.single().prompts)
    }

    @Test
    fun whatATurnChangedIsOfferedToKeepOrUndo() = blockingTest {
        val changeLog = ChangeLog()
        changeLog.record("add_block", null)
        val chat = newChat(changeLog).also { chat -> chat.state.first { it == ChatState.Ready } }
        withContext(mainThread) { chat.send("wait") }
        while ("wait" !in launcher.agents.single().prompts) delay(10)
        changeLog.record("add_block", CommandTarget.BlockType("Ball", 0), origin = AiStepOrigin("tools"), isByAi = true)
        changeLog.record("add_block", CommandTarget.BlockType("Brick", 0), origin = AiStepOrigin("other"), isByAi = true)
        withContext(mainThread) { chat.stop() }
        chat.isBusy.first { !it }
        val changes = chat.transcript.value.items.filterIsInstance<TranscriptItem.TurnChanges>().single()
        assertEquals(listOf<CommandTarget?>(CommandTarget.BlockType("Ball", 0)), changes.changes.map { it.target })
        withContext(mainThread) { chat.undoChanges(changes.sinceRevision) }
        chat.transcript.first { transcript -> transcript.items.any { it is TranscriptItem.TurnChanges && it.state == TranscriptItem.TurnChanges.State.UNDONE } }
        assertEquals(listOf("tools" to 1), undoneTurns)
    }

    @Test
    fun onlyARefusedPromptIsSentAgain() = blockingTest {
        val chat = readyChat()
        chat.sendAndWait("hello")
        withContext(mainThread) { chat.resendAfterSignIn() }
        assertFalse(chat.isBusy.value)
        assertEquals(listOf("hello"), launcher.agents.single().prompts)
    }
}
