/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.ai.assistant.AcpAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentCommand
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.FakeAgentLauncher
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.assistant.FakeAssistantPanelModel
import com.pandulapeter.kubrikoCreator.ui.components.DescriptionPreview
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
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
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * The AI button at the end of a description, which has the in-app assistant write it.
 */
@OptIn(ExperimentalTestApi::class)
class WriteDescriptionButtonTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val launcher = FakeAgentLauncher()
    private val acpAgent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO)
    private val sent = mutableListOf<String>()

    @AfterTest
    fun tearDown() {
        acpAgent.close()
        launcher.close()
        scope.cancel()
        mainThread.close()
    }

    private fun readyChat() = runBlocking {
        val chat = withContext(mainThread) {
            AssistantChat(
                parentScope = scope,
                start = { permissions, _ -> acpAgent.newConversation(File("/projects/breakout"), permissions = permissions) },
                end = { it.close() },
            )
        }
        withTimeout(10.seconds) { chat.state.first { it == ChatState.Ready } }
        chat
    }

    @Composable
    private fun Description(model: AssistantPanelModel) {
        CompositionLocalProvider(LocalDescriptionWriter provides rememberDescriptionWriter(model) { sent += "shown" }) {
            DescriptionPreview(
                name = "Ball",
                description = "The ball",
                maximumLength = 200,
                onDescriptionChange = {},
                action = { WriteDescriptionButton(prompt = PROMPT) },
            )
        }
    }

    @Test
    fun theButtonSendsItsPromptToTheConversationShownAndShowsTheAssistant() = uiTest {
        val chat = readyChat()
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        showing { Description(model) }
        saveSnapshot("write-description-button")
        onNodeWithContentDescription(LABEL).performClick()
        waitForIdle()
        assertEquals(listOf("shown"), sent)
        assertEquals(listOf(PROMPT), chat.transcript.value.items.filterIsInstance<TranscriptItem.UserMessage>().map { it.text })
    }

    @Test
    fun theButtonIsDisabledWhileTheAssistantIsInATurn() = uiTest {
        showing {
            CompositionLocalProvider(LocalDescriptionWriter provides DescriptionWriter(isBusy = true) { sent += it }) {
                WriteDescriptionButton(prompt = PROMPT)
            }
        }
        onNodeWithContentDescription(LABEL).assertIsNotEnabled()
    }

    @Test
    fun withoutAConversationThereIsNoButton() = uiTest {
        showing { Description(FakeAssistantPanelModel()) }
        onNodeWithContentDescription(LABEL).assertDoesNotExist()
    }

    @Test
    fun aConversationThatIsNotReadyShowsNoButton() = uiTest {
        val chat = runBlocking {
            withContext(mainThread) { AssistantChat(parentScope = scope, start = { _, _ -> error("No agent") }, end = {}) }
        }
        runBlocking { withTimeout(10.seconds) { chat.state.first { it is ChatState.Failed } } }
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        showing { Description(model) }
        onNodeWithContentDescription(LABEL).assertDoesNotExist()
    }

    @Test
    fun withAiFeaturesOffThereIsNoButton() = uiTest {
        val model = FakeAssistantPanelModel().apply { currentChat.value = readyChat() }
        showing { CompositionLocalProvider(LocalIsAiEnabled provides false) { Description(model) } }
        onNodeWithContentDescription(LABEL).assertDoesNotExist()
    }

    private companion object {
        const val LABEL = "Write with AI"
        const val PROMPT = "Write the description of the Actor type Ball."
    }
}
