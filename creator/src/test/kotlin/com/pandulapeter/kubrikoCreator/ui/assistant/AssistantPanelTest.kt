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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import com.pandulapeter.kubrikoCreator.ai.assistant.AcpAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentCommand
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentConfiguration
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.AuthStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.FakeAgentLauncher
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import com.pandulapeter.kubrikoCreator.testing.PANEL_WIDTH
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
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
import kubriko.creator.generated.resources.action_delete
import kubriko.creator.generated.resources.action_delete_conversation
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.assistant_account_signed_in
import kubriko.creator.generated.resources.assistant_account_use_api_key
import kubriko.creator.generated.resources.assistant_agent_custom
import kubriko.creator.generated.resources.assistant_agent_untested
import kubriko.creator.generated.resources.assistant_note
import kubriko.creator.generated.resources.assistant_note_untested
import kubriko.creator.generated.resources.assistant_setup_configuration
import kubriko.creator.generated.resources.assistant_unasked_changes
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
    private val codex = AgentChoice.Registry(RegistryAgent("codex-acp", "Codex", "2.4.0", RegistryAgent.claude.distribution))
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
                toolsIdOf = { TOOLS_ID },
            )
        }
        withTimeout(10.seconds) { chat.state.first { it == ChatState.Ready } }
        chat
    }

    /**
     * The panel with its buttons in the AI panel's header, as the app shows them.
     */
    @Composable
    private fun AssistantPanelWithActions(model: AssistantPanelModel) {
        val forms = rememberAssistantForms()
        Column {
            Row { AssistantHeaderActions(model = model, forms = forms) }
            AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, forms = forms)
        }
    }

    private fun ComposeUiTest.openSetup() = onNodeWithContentDescription("Assistant setup", substring = true).performClick()

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
            configurationLabel = stringResource(Res.string.assistant_setup_configuration)
            note = stringResource(Res.string.assistant_note_untested, untested.name)
            AssistantPanelWithActions(model)
        }
        onNodeWithText(note).assertExists()
        onNodeWithText(accountLabel).assertDoesNotExist()
        openSetup()
        onNodeWithText(configurationLabel).assertDoesNotExist()
        onNodeWithText(accountLabel).performClick()
        saveSnapshot("assistant-panel-untested")
        onNodeWithText(useApiKey).assertDoesNotExist()
    }

    @Test
    fun aConversationOfAnAgentChangingFilesWithoutAskingSuggestsVersionControl() = uiTest {
        val model = FakeAssistantPanelModel(agent = untested).apply { currentChat.value = readyChat() }
        lateinit var notice: String
        showing {
            notice = stringResource(Res.string.assistant_unasked_changes, untested.name)
            AssistantPanelWithActions(model)
        }
        onNodeWithText(notice).assertDoesNotExist()
        model.agentsChangingFilesUnasked.value = setOf(untested.identity)
        onNodeWithText(notice).assertExists()
        saveSnapshot("assistant-panel-unasked-changes")
    }

    @Test
    fun claudeAgentOffersItsApiKeyAndConfigurations() = uiTest {
        val model = FakeAssistantPanelModel(canKeepApiKey = true).apply {
            authStatus.value = signedIn
            configurations.value = listOf(AgentConfiguration(AiClient.CLAUDE_CODE), AgentConfiguration(AiClient.CLAUDE_CODE, alias = "claude-personal", variant = "personal"))
            currentChat.value = readyChat()
        }
        lateinit var accountLabel: String
        lateinit var useApiKey: String
        lateinit var configurationLabel: String
        lateinit var note: String
        showing {
            accountLabel = stringResource(Res.string.assistant_account_signed_in, signedIn.label, signedIn.email!!)
            useApiKey = stringResource(Res.string.assistant_account_use_api_key)
            configurationLabel = AiClient.CLAUDE_CODE.displayName
            note = stringResource(Res.string.assistant_note)
            AssistantPanelWithActions(model)
        }
        onNodeWithText(note).assertExists()
        openSetup()
        saveSnapshot("assistant-setup")
        onNodeWithText(configurationLabel).assertExists()
        onNodeWithText(accountLabel).performClick()
        saveSnapshot("assistant-panel-claude")
        onNodeWithText(useApiKey).assertExists()
    }

    @Test
    fun codexOffersTheConfigurationsOfItsAliases() = uiTest {
        val work = AgentConfiguration(AiClient.CODEX, alias = "codex-work", variant = "work", environment = mapOf("CODEX_HOME" to "/Users/me/.codex-work"))
        val model = FakeAssistantPanelModel(agent = codex).apply {
            configuration.value = work
            configurations.value = listOf(AgentConfiguration(AiClient.CODEX), work)
            currentChat.value = readyChat()
        }
        showing { AssistantPanelWithActions(model) }
        openSetup()
        onNodeWithText("Codex (work)").performClick()
        onAllNodesWithText("Codex").onLast().performClick()
        saveSnapshot("assistant-setup-codex")
        assertEquals(listOf("selectConfiguration"), model.calls.filter { it == "selectConfiguration" })
    }

    @Test
    fun theFirstShowingStartsAConversation() = uiTest {
        val model = FakeAssistantPanelModel()
        showing { AssistantPanelWithActions(model) }
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
            AssistantPanelWithActions(model)
        }
        openSetup()
        onNodeWithText(agentLabel).performClick()
        onNodeWithText(customAgent).performClick()
        onAllNodes(hasSetTextAction())[0].performTextInput("goose acp")
        onNodeWithText(save).performClick()
        assertEquals(listOf<AgentChoice>(AgentChoice.Custom("goose acp")), model.selectedAgents)
    }

    @Test
    fun anEmptyConversationCanNeitherBeDeletedNorFollowedByANewOne() = uiTest {
        val model = FakeAssistantPanelModel().apply { currentChat.value = readyChat() }
        lateinit var delete: String
        lateinit var newConversation: String
        showing {
            delete = stringResource(Res.string.action_delete_conversation)
            newConversation = stringResource(Res.string.action_new_conversation)
            AssistantPanelWithActions(model)
        }
        onNodeWithContentDescription(delete).assertDoesNotExist()
        onNodeWithContentDescription(newConversation).assertIsNotEnabled()
    }

    @Test
    fun aConversationWithMessagesIsDeletedOnlyOnceConfirmed() = uiTest {
        val chat = readyChat()
        runBlocking {
            withContext(mainThread) { chat.send("hello") }
            withTimeout(10.seconds) { chat.isBusy.first { !it } }
        }
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        lateinit var deleteConversation: String
        lateinit var newConversation: String
        lateinit var delete: String
        showing {
            deleteConversation = stringResource(Res.string.action_delete_conversation)
            newConversation = stringResource(Res.string.action_new_conversation)
            delete = stringResource(Res.string.action_delete)
            AssistantPanelWithActions(model)
        }
        onNodeWithContentDescription(newConversation).assertIsEnabled()
        onNodeWithContentDescription(deleteConversation).performClick()
        assertEquals(emptyList(), model.calls.filter { it == "delete" })
        saveSnapshot("assistant-delete-conversation")
        onNodeWithText(delete).performClick()
        assertEquals(listOf("delete"), model.calls.filter { it == "delete" })
    }

    @Test
    fun aLongAccountNameShowsInFullInOneLineWhereTheWindowHasRoom() = showingALongAccountName(width = 1200) { layout ->
        assertEquals(1, layout.lineCount)
    }

    @Test
    fun aLongAccountNameWrapsInsteadOfBeingCutOffInANarrowWindow() = showingALongAccountName(width = PANEL_WIDTH) { layout ->
        assertTrue(layout.lineCount > 1)
    }

    private fun showingALongAccountName(width: Int, checkLayout: (TextLayoutResult) -> Unit) = uiTest(width = width) {
        val status = AuthStatus(AuthStatus.Kind.ACCOUNT, "Claude Max", email = "a.rather.long.address.for.a.subscription@example.com")
        val model = FakeAssistantPanelModel().apply { authStatus.value = status }
        lateinit var accountLabel: String
        showing {
            accountLabel = stringResource(Res.string.assistant_account_signed_in, status.label, status.email!!)
            AssistantPanelWithActions(model)
        }
        openSetup()
        saveSnapshot("assistant-setup-long-account-$width")
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText(accountLabel).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertFalse(layout.hasVisualOverflow)
        checkLayout(layout)
    }

    @Test
    fun anAnswerTheAgentStoppedWaitingForGoesToItAsTheNextMessage() = uiTest {
        val chat = readyChat()
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        val question = UserQuestions.Question(1, "How does the hero get around?", listOf("Runs and jumps", "Flies"), AiStepOrigin(TOOLS_ID))
        val answered = mutableListOf<Pair<Long, String>>()
        showing {
            AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, questions = listOf(question), onAnswer = { id, answer -> answered += id to answer })
        }
        onNodeWithText("Flies").performClick()
        waitUntil(timeoutMillis = 10_000) { launcher.agents.single().prompts.isNotEmpty() }
        assertEquals(listOf(1L to "Flies"), answered)
        assertEquals(listOf("My answer to \"How does the hero get around?\": Flies"), launcher.agents.single().prompts)
    }

    @Test
    fun whatIsTypedWhileTheAgentWaitsForAnAnswerAnswersItRatherThanBeingLost() = uiTest {
        val chat = readyChat()
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        val question = UserQuestions.Question(1, "How does the hero get around?", listOf("Runs and jumps", "Flies"), AiStepOrigin(TOOLS_ID))
        val answered = mutableListOf<Pair<Long, String>>()
        var questions by mutableStateOf(emptyList<UserQuestions.Question>())
        showing {
            AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, questions = questions, onAnswer = { id, answer -> answered += id to answer })
        }
        runBlocking { withContext(mainThread) { chat.send("wait") } }
        waitUntil(timeoutMillis = 10_000) { chat.isBusy.value }
        questions = listOf(question)
        onNodeWithText("Answer the question above", substring = true).assertExists()
        onNodeWithText("Another answer").assertDoesNotExist()
        onNode(hasSetTextAction()).performTextInput("Rides a dragon")
        onNodeWithContentDescription("Answer").performClick()
        assertEquals(listOf(1L to "Rides a dragon"), answered)
        assertEquals(listOf("wait"), launcher.agents.single().prompts)
    }

    @Test
    fun aConversationTheUserIsDoneWithIsLeftForANewOneWithoutStoppingAnything() = uiTest {
        val chat = readyChat()
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        showing { AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}) }
        onNodeWithText("Done? Start a new conversation").assertDoesNotExist()
        runBlocking { withContext(mainThread) { chat.send("hello") } }
        waitUntil(timeoutMillis = 10_000) { !chat.isEmpty.value && !chat.isBusy.value }
        saveSnapshot("assistant-new-conversation-offer")
        onNodeWithText("Done? Start a new conversation").performClick()
        assertEquals(1, model.calls.count { it == "newChat" })
    }

    @Test
    fun leavingAConversationWhoseAgentWaitsForAnAnswerStopsItsTurnAndAnswersForTheUser() = uiTest {
        val chat = readyChat()
        val model = FakeAssistantPanelModel().apply {
            currentChat.value = chat
            onNewChat = { currentChat.value = null }
        }
        val question = UserQuestions.Question(1, "Anything else?", listOf("Yes", "No"), AiStepOrigin(TOOLS_ID))
        val answered = mutableListOf<Pair<Long, String>>()
        var questions by mutableStateOf(emptyList<UserQuestions.Question>())
        showing { AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, questions = questions, onAnswer = { id, answer -> answered += id to answer }) }
        runBlocking { withContext(mainThread) { chat.send("wait") } }
        waitUntil(timeoutMillis = 10_000) { chat.isBusy.value }
        onNodeWithText("Done? Start a new conversation").assertDoesNotExist()
        questions = listOf(question)
        onNodeWithText("Done? Start a new conversation").performClick()
        waitUntil(timeoutMillis = 10_000) { !chat.isBusy.value }
        assertEquals(listOf(1L to UserQuestions.LEFT_UNANSWERED), answered)
    }

    @Test
    fun aConversationThatCouldntBeLeftKeepsItsTurn() = uiTest {
        val chat = readyChat()
        val model = FakeAssistantPanelModel().apply { currentChat.value = chat }
        val question = UserQuestions.Question(1, "Anything else?", listOf("Yes", "No"), AiStepOrigin(TOOLS_ID))
        val answered = mutableListOf<Pair<Long, String>>()
        showing { AssistantPanel(model = model, aiSteps = emptyList(), onShowStep = {}, questions = listOf(question), onAnswer = { id, answer -> answered += id to answer }) }
        runBlocking { withContext(mainThread) { chat.send("wait") } }
        waitUntil(timeoutMillis = 10_000) { chat.isBusy.value }
        onNodeWithText("Done? Start a new conversation").performClick()
        waitForIdle()
        assertTrue(chat.isBusy.value)
        assertEquals(emptyList(), answered)
    }

    private companion object {
        const val TOOLS_ID = "tools"
    }
}
