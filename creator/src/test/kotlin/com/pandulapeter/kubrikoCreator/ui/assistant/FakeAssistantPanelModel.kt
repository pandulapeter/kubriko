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

import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentConfiguration
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentDownload
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.AuthStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationMatch
import com.pandulapeter.kubrikoCreator.ai.assistant.KeptMessage
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversation
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInSession
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An [AssistantPanelModel] of plain flows, which records the calls the panel makes.
 */
internal class FakeAssistantPanelModel(
    agent: AgentChoice = AgentChoice.Registry(RegistryAgent.claude),
    override val canKeepApiKey: Boolean = false,
    override val customAgentCommand: String? = null,
) : AssistantPanelModel {
    val calls = CopyOnWriteArrayList<String>()
    val selectedAgents = CopyOnWriteArrayList<AgentChoice>()
    override val agent = MutableStateFlow(agent)
    override val agents = MutableStateFlow(listOf(RegistryAgent.claude))
    override val agentsChangingFilesUnasked = MutableStateFlow(emptySet<String>())
    override val chats = MutableStateFlow(emptyList<AssistantChat>())
    override val currentChat = MutableStateFlow<AssistantChat?>(null)
    override val savedConversations = MutableStateFlow(emptyList<SavedConversation>())
    override val authStatus = MutableStateFlow<AuthStatus?>(null)
    override val signInMethods = MutableStateFlow(emptyList<SignInMethod>())
    override val needsSignIn = MutableStateFlow(false)
    override val signIn = MutableStateFlow<SignInSession?>(null)
    override val hasApiKey = MutableStateFlow(false)
    override val apiKeyFailed = MutableStateFlow(false)
    override val configuration = MutableStateFlow<AgentConfiguration?>(AgentConfiguration(AiClient.CLAUDE_CODE))
    override val configurations = MutableStateFlow(emptyList<AgentConfiguration>())
    override val download = MutableStateFlow<AgentDownload?>(null)
    override val canInstallNode = false
    override val nodeInstallFailure = MutableStateFlow<String?>(null)
    override val variablesForm = MutableStateFlow<SignInMethod?>(null)
    override val variablesFailed = MutableStateFlow(false)
    override val errorLogFile: File? = null

    override fun findConfigurations() {
        calls += "findConfigurations"
    }

    var onNewChat: () -> Unit = {}

    override fun newChat() {
        calls += "newChat"
        onNewChat()
    }

    override fun startWritersRoom(prompt: String) {
        calls += "startWritersRoom: $prompt"
    }

    override fun resume(conversation: SavedConversation) {
        calls += "resume"
    }

    var conversationMatches = emptyList<ConversationMatch>()
    var keptMessages = emptyList<KeptMessage>()

    override suspend fun searchConversations(query: String) = conversationMatches.filter { it.query == query }.map { it.copy(isHeldByAnotherAgent = it.conversation.agentId != agent.value.id) }

    override suspend fun keptMessagesOf(conversation: SavedConversation) = keptMessages

    override fun show(conversation: SavedConversation) {
        calls += "show: ${conversation.id}"
    }

    override fun select(chat: AssistantChat) {
        calls += "select"
    }

    override fun delete(chat: AssistantChat) {
        calls += "delete"
    }

    override fun selectAgent(choice: AgentChoice, isPanelShown: Boolean) {
        selectedAgents += choice
    }

    override fun selectConfiguration(configuration: AgentConfiguration, isPanelShown: Boolean) {
        calls += "selectConfiguration"
    }

    override var canSignInInSystemTerminal = true
    override val isOpeningSystemTerminal = MutableStateFlow(false)
    override val systemTerminalFailed = MutableStateFlow(false)

    override fun signInInSystemTerminal(method: SignInMethod) {
        calls += "signInInSystemTerminal: ${method.name}"
    }

    override fun signIn(method: SignInMethod) {
        calls += "signIn"
    }

    override fun closeSignIn() {
        calls += "closeSignIn"
    }

    override fun setApiKey(apiKey: String, onKept: () -> Unit) {
        calls += "setApiKey"
    }

    override fun removeApiKey() {
        calls += "removeApiKey"
    }

    override fun installNode() {
        calls += "installNode"
    }

    override fun setVariables(method: SignInMethod, values: Map<String, String>) {
        calls += "setVariables"
    }

    override fun closeVariablesForm() {
        calls += "closeVariablesForm"
    }
}
