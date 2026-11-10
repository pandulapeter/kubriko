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
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import com.pandulapeter.kubrikoCreator.ai.assistant.FakeAgent.ReadOnlyOffer
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ReadOnlyModeTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val launchers = mutableListOf<FakeAgentLauncher>()
    private val agents = mutableListOf<AcpAgent>()

    @AfterTest
    fun tearDown() {
        agents.forEach { it.close() }
        launchers.forEach { it.close() }
        scope.cancel()
    }

    private fun agent(readOnlyMode: ReadOnlyOffer, reportsModeChanges: Boolean = true): Pair<AcpAgent, FakeAgentLauncher> {
        val launcher = FakeAgentLauncher({ FakeAgent(readOnlyMode = readOnlyMode, reportsModeChanges = reportsModeChanges) }).also { launchers += it }
        val agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO).also { agents += it }
        return agent to launcher
    }

    private suspend fun AgentConversation.reply(text: String) = buildString {
        prompt(listOf(ContentBlock.Text(text))) { update -> ((update as? SessionUpdate.AgentMessageChunk)?.content as? ContentBlock.Text)?.let { append(it.text) } }
    }

    @Test
    fun aConversationPreferringReadOnlyEntersTheReadOnlyValueOfAConfigOption() = blockingTest {
        val conversation = agent(ReadOnlyOffer.CONFIG_OPTION).first.newConversation(WORKING_DIRECTORY, prefersReadOnly = true)
        assertEquals(FakeAgent.READ_ONLY, conversation.reply("mode"))
        assertTrue(conversation.isReadOnly)
    }

    @Test
    fun aConversationPreferringReadOnlyEntersTheReadOnlySessionMode() = blockingTest {
        val conversation = agent(ReadOnlyOffer.MODE).first.newConversation(WORKING_DIRECTORY, prefersReadOnly = true)
        assertEquals(FakeAgent.READ_ONLY, conversation.reply("mode"))
        assertTrue(conversation.isReadOnly)
    }

    @Test
    fun aSessionModeTheAgentDoesntReportSwitchingToStillCountsAsReadOnly() = blockingTest {
        val conversation = agent(ReadOnlyOffer.MODE, reportsModeChanges = false).first.newConversation(WORKING_DIRECTORY, prefersReadOnly = true)
        assertEquals(FakeAgent.READ_ONLY, conversation.reply("mode"))
        assertTrue(conversation.isReadOnly)
    }

    @Test
    fun anAgentNeverAnsweringTheSwitchStillStartsTheConversationAndOthers() = blockingTest(timeout = TIMEOUT, context = EmptyCoroutineContext) {
        val launcher = FakeAgentLauncher({ FakeAgent(readOnlyMode = ReadOnlyOffer.MODE, answersModeChanges = false) }).also { launchers += it }
        val agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO).also { agents += it }
        val conversation = agent.newConversation(WORKING_DIRECTORY, prefersReadOnly = true)
        assertFalse(conversation.isReadOnly)
        assertEquals("Hi there", agent.newConversation(WORKING_DIRECTORY).reply("hello"))
    }

    @Test
    fun aConversationNotPreferringReadOnlyIsLeftAsTheAgentStartsIt() = blockingTest {
        val conversation = agent(ReadOnlyOffer.CONFIG_OPTION).first.newConversation(WORKING_DIRECTORY)
        assertEquals("writable", conversation.reply("mode"))
        assertFalse(conversation.isReadOnly)
    }

    @Test
    fun anAgentWithoutAReadOnlyModeStartsConversationsAsItWould() = blockingTest {
        val conversation = agent(ReadOnlyOffer.NONE).first.newConversation(WORKING_DIRECTORY, prefersReadOnly = true)
        assertEquals("writable", conversation.reply("mode"))
        assertFalse(conversation.isReadOnly)
    }

    @Test
    fun aResumedConversationEntersTheReadOnlyModeToo() = blockingTest {
        val (agent, launcher) = agent(ReadOnlyOffer.MODE)
        agent.start()
        val conversation = agent.resumeConversation(SessionId("earlier"), WORKING_DIRECTORY, prefersReadOnly = true)
        assertEquals(FakeAgent.READ_ONLY, conversation.reply("mode"))
        assertEquals(1, launcher.agents.single().loadedSessions.size)
    }

    @Test
    fun aConversationLoadedAgainAfterACrashEntersTheReadOnlyModeAgain() = blockingTest(timeout = TIMEOUT, context = EmptyCoroutineContext) {
        val (agent, launcher) = agent(ReadOnlyOffer.CONFIG_OPTION)
        val conversation = agent.newConversation(WORKING_DIRECTORY, prefersReadOnly = true)
        assertFailsWith<Exception> { conversation.prompt(listOf(ContentBlock.Text("crash"))) }
        withTimeout(TIMEOUT) { agent.state.first { it is AgentState.Running && launcher.agents.size == 2 } }
        assertEquals(FakeAgent.READ_ONLY, conversation.reply("mode"))
    }

    @Test
    fun readOnlyIsRecognizedHoweverItIsWritten() {
        listOf("read-only", "readOnly", "Read only", "workspace_read_only").forEach { assertTrue(ReadOnlyMode.isReadOnly(it, "Sandbox"), it) }
        listOf("plan", "auto", "default", "ask").forEach { assertFalse(ReadOnlyMode.isReadOnly(it, it), it) }
    }

    @Test
    fun aPlanModeNamedReadOnlyIsntOne() {
        assertFalse(ReadOnlyMode.isReadOnly("plan", "Plan (read-only)"))
        assertFalse(ReadOnlyMode.isReadOnly("read-only-plan", "Read-only planning"))
    }

    private companion object {
        val WORKING_DIRECTORY = File("/projects/breakout")
        val TIMEOUT = 10.seconds
    }
}
