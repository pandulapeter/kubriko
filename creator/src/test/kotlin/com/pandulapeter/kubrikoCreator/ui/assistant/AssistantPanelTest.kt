/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pandulapeter.kubrikoCreator.ai.assistant.AcpAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentCommand
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AuthStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.ClaudeConfiguration
import com.pandulapeter.kubrikoCreator.ai.assistant.FakeAgentLauncher
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ui.saveSnapshot
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.assistant_account_signed_in
import kubriko.creator.generated.resources.assistant_account_use_api_key
import kubriko.creator.generated.resources.assistant_agent_custom
import kubriko.creator.generated.resources.assistant_agent_untested
import kubriko.creator.generated.resources.assistant_configuration_default
import kubriko.creator.generated.resources.assistant_note
import kubriko.creator.generated.resources.assistant_note_untested
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * The whole assistant panel, rendered offscreen from a [FakeAssistantPanelModel].
 */
@OptIn(ExperimentalTestApi::class)
class AssistantPanelTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val launcher = FakeAgentLauncher()
    private val acpAgent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO)
    private val untested = AgentChoice.Registry(RegistryAgent("antigravity-acp", "Google Antigravity", "1.4.5", RegistryAgent.claude.distribution))
    private val signedIn = AuthStatus(AuthStatus.Kind.ACCOUNT, "Claude Max", email = "me@example.com")

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

    @Test
    fun anUntestedAgentShowsTheUntestedNoteAndNoClaudeOptions() = uiTest {
        val model = FakeAssistantPanelModel(agent = untested).apply {
            authStatus.value = signedIn
            currentChat.value = readyChat()
        }
        lateinit var accountLabel: String
        lateinit var useApiKey: String
        lateinit var configurationLabel: String
        lateinit var note: String
        showing {
            accountLabel = stringResource(Res.string.assistant_account_signed_in, signedIn.label, signedIn.email!!)
            useApiKey = stringResource(Res.string.assistant_account_use_api_key)
            configurationLabel = stringResource(Res.string.assistant_configuration_default)
            note = stringResource(Res.string.assistant_note_untested, untested.name)
            AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {})
        }
        onNodeWithText(note).assertExists()
        onNodeWithText(configurationLabel).assertDoesNotExist()
        onNodeWithText(accountLabel).performClick()
        saveSnapshot("assistant-panel-untested")
        onNodeWithText(useApiKey).assertDoesNotExist()
    }

    @Test
    fun claudeAgentOffersItsApiKeyAndConfigurations() = uiTest {
        val model = FakeAssistantPanelModel(canKeepApiKey = true).apply {
            authStatus.value = signedIn
            configurations.value = listOf(ClaudeConfiguration.default, ClaudeConfiguration(alias = "claude-personal", variant = "claude-personal"))
            currentChat.value = readyChat()
        }
        lateinit var accountLabel: String
        lateinit var useApiKey: String
        lateinit var configurationLabel: String
        lateinit var note: String
        showing {
            accountLabel = stringResource(Res.string.assistant_account_signed_in, signedIn.label, signedIn.email!!)
            useApiKey = stringResource(Res.string.assistant_account_use_api_key)
            configurationLabel = stringResource(Res.string.assistant_configuration_default)
            note = stringResource(Res.string.assistant_note)
            AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {})
        }
        onNodeWithText(note).assertExists()
        onNodeWithText(configurationLabel).assertExists()
        onNodeWithText(accountLabel).performClick()
        saveSnapshot("assistant-panel-claude")
        onNodeWithText(useApiKey).assertExists()
    }

    @Test
    fun theFirstShowingStartsAConversation() = uiTest {
        val model = FakeAssistantPanelModel()
        showing { AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}) }
        waitForIdle()
        assertEquals(1, model.calls.count { it == "newChat" })
        assertEquals(1, model.calls.count { it == "findConfigurations" })
    }

    @Test
    fun choosingACustomAgentPassesItsCommand() = uiTest {
        val model = FakeAssistantPanelModel(agent = untested)
        lateinit var agentLabel: String
        lateinit var customAgent: String
        lateinit var save: String
        showing {
            agentLabel = stringResource(Res.string.assistant_agent_untested, untested.name)
            customAgent = stringResource(Res.string.assistant_agent_custom)
            save = stringResource(Res.string.action_save)
            AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {})
        }
        onNodeWithText(agentLabel).performClick()
        onNodeWithText(customAgent).performClick()
        onAllNodes(hasSetTextAction())[0].performTextInput("goose acp")
        onNodeWithText(save).performClick()
        assertEquals(listOf<AgentChoice>(AgentChoice.Custom("goose acp")), model.selectedAgents)
    }
}
