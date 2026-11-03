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

import com.agentclientprotocol.model.SessionUpdate
import com.pandulapeter.kubrikoCreator.commands.ChangeLog
import com.pandulapeter.kubrikoCreator.mcp.clients.ProcessRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The assistant panel's state for as long as Kubriko Creator runs: the agent chosen, the one [Assistant] running it,
 * created on first use and held until Creator quits or another agent is chosen, the Claude Code configuration Claude
 * Agent signs in with, and the conversations about the open project,
 * which end when the project closes, since their tools work on whatever project is open.
 *
 * @param scope Where conversations run: the main thread, which the panel's state is read on.
 * @param secrets Where the API key is kept.
 * @param signInLauncher Runs the sign-in commands of an agent's terminal sign-in methods.
 * @param createAssistant Creates the assistant running [AgentCommand] with [AgentProfile], given the user's `npx` where
 *   there is one.
 * @param projectDigest The digest of the open project, which a new conversation starts with.
 * @param changeLog The open project's changes, which a prompt sums up for the agent when the user made some since its last turn.
 * @param undoTurn Reverts what a conversation changed after a revision ([AssistantChat]'s Undo).
 * @param registry Where the registry's agents come from, or null for Claude Agent alone.
 * @param installer Downloads the agents the registry distributes as archives.
 * @param errorLogFile Where agents' standard error is kept ([AgentErrorLog]), for the user to open.
 * @param conversations Where the conversations are kept per project, to be continued later.
 * @param node Downloads a Node.js of Creator's own for a user without one.
 * @param isAiEnabled Whether AI features are on: while they are off, no agent starts, no conversation opens, the registry
 *   isn't fetched, and turning them off ends what runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class AssistantController(
    private val scope: CoroutineScope,
    private val runner: ProcessRunner,
    private val store: AssistantSettingsStore,
    private val secrets: SecretStore,
    private val signInLauncher: AgentLauncher,
    private val ioDispatcher: CoroutineDispatcher,
    private val createAssistant: (AgentCommand, npx: File?, AgentProfile) -> Assistant,
    private val projectDigest: suspend () -> String? = { null },
    private val changeLog: () -> ChangeLog? = { null },
    private val undoTurn: suspend (toolsId: String, sinceRevision: Int) -> Int = { _, _ -> 0 },
    private val home: String = System.getProperty("user.home"),
    private val registry: AgentRegistryStore? = null,
    private val installer: BinaryAgentInstaller? = null,
    override val errorLogFile: File? = null,
    private val conversations: SavedConversationStore? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    node: NodeRuntime? = null,
    private val isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
) : AssistantPanelModel, AutoCloseable {
    private val settings = AssistantSettingsState(scope, store, ioDispatcher)

    /**
     * Built before [selection], whose registry loading starts on [scope]'s thread while this constructor still runs and
     * asks whether an agent runs.
     */
    private val host: AssistantHost<Assistant> = AssistantHost(scope, ioDispatcher, isAiEnabled) {
        selection.awaitLoaded()
        val choice = selection.agent.value
        val npx = commands.npx()
        val environment = if (choice.isClaude) claude.environment() else signIns.environmentOf(choice)
        createAssistant(commands.of(choice, npx, environment), npx, choice.profile)
    }
    private val selection: AgentSelection = AgentSelection(scope, settings, registry, ioDispatcher, isAiEnabled, isAgentRunning = { host.current.value != null })

    override val agents: StateFlow<List<RegistryAgent>> = selection.agents

    override val agent: StateFlow<AgentChoice> = selection.agent

    override val customAgentCommand: String? get() = selection.customAgentCommand
    private val claude = ClaudeAccount(scope, ioDispatcher, runner, secrets, home, settings.value.value.claudeConfigurationAlias)

    override val configurations: StateFlow<List<ClaudeConfiguration>> = combine(claude.configurations, agent) { configurations, choice -> configurations.takeIf { choice.profile.usesClaudeAccount }.orEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, claude.configurations.value.takeIf { agent.value.profile.usesClaudeAccount }.orEmpty())

    override val configuration: StateFlow<ClaudeConfiguration> = claude.configuration
    private val chatList = AssistantChats(scope, ioDispatcher, conversations)

    override val chats: StateFlow<List<AssistantChat>> = chatList.open

    override val savedConversations: StateFlow<List<SavedConversation>> = chatList.saved

    override val currentChat: StateFlow<AssistantChat?> = chatList.current
    private val commands = AgentCommands(runner, installer, home, ioDispatcher, node)
    private val signIns: AssistantSignIn = AssistantSignIn(
        scope = scope,
        ioDispatcher = ioDispatcher,
        signInLauncher = signInLauncher,
        secrets = secrets,
        settings = settings,
        host = host,
        resendRefused = { chats.value.forEach { it.resendAfterSignIn() } },
        restart = ::restartAgent,
    )

    override val download: StateFlow<AgentDownload?> = commands.download

    override val variablesForm: StateFlow<SignInMethod?> = signIns.variablesForm

    override val variablesFailed: StateFlow<Boolean> = signIns.variablesFailed

    init {
        scope.launch { isAiEnabled.drop(1).filter { !it }.collect { turnOff() } }
    }

    override val authStatus: StateFlow<AuthStatus?> = signIns.authStatus

    override val signInMethods: StateFlow<List<SignInMethod>> = signIns.signInMethods

    /**
     * Whether the agent takes images in prompts, once it has started.
     */
    public val acceptsImages: StateFlow<Boolean> = host.current.flatMapLatest { it?.capabilities ?: flowOf(null) }
        .map { it?.promptCapabilities?.image == true }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override val needsSignIn: StateFlow<Boolean> = signIns.needsSignIn

    override val signIn: StateFlow<SignInSession?> = signIns.session

    override val hasApiKey: StateFlow<Boolean> = combine(claude.hasApiKey, agent) { hasApiKey, choice -> hasApiKey && choice.profile.usesClaudeAccount }
        .stateIn(scope, SharingStarted.Eagerly, claude.hasApiKey.value && agent.value.profile.usesClaudeAccount)

    override val canKeepApiKey: Boolean get() = claude.canKeepApiKey && agent.value.profile.usesClaudeAccount

    override val apiKeyFailed: StateFlow<Boolean> = claude.apiKeyFailed
    private var project: File? = null

    override fun findConfigurations(): Unit = claude.findConfigurations()

    /**
     * Makes [directory] the project conversations are about, ending those about the one before.
     */
    public fun setProject(directory: File?) {
        if (directory == project) return
        project = directory
        endChats()
    }

    override fun newChat() {
        if (!host.isOn) return
        openChat { owner, directory, permissions, _ -> owner.newConversation(directory, permissions, LastConversation.withProject(projectDigest(), lastConversationOf(directory))) }
    }

    private suspend fun lastConversationOf(directory: File) = withContext(ioDispatcher) { conversations?.of(directory, selection.agent.value.id)?.firstOrNull() }

    override fun resume(conversation: SavedConversation) {
        if (!host.isOn) return
        if (chatList.reopen(conversation.id)) return
        openChat { owner, directory, permissions, onHistory -> owner.resumeConversation(directory, conversation.id, permissions, onHistory) }
    }

    private fun openChat(start: suspend (Assistant, File, PermissionHandler, suspend (SessionUpdate) -> Unit) -> AgentConversation) {
        val directory = project ?: return
        val agentId = selection.agent.value.id
        var owner: Assistant? = null
        val chat = AssistantChat(
            parentScope = scope,
            start = { permissions, onHistory -> start(host.get().also { owner = it }, directory, permissions, onHistory) },
            end = { conversation -> owner?.closeConversation(conversation) },
            toolsIdOf = { conversation -> owner?.toolsIdOf(conversation) },
            forgetReads = { conversation -> owner?.forgetReads(conversation) },
            changeLog = changeLog,
            undoTurn = undoTurn,
            onTurnEnded = { id, title, lastAnswer ->
                withContext(ioDispatcher) { conversations?.save(SavedConversation(directory.absolutePath, agentId, id, title, clock(), lastAnswer?.take(LastConversation.ANSWER_LENGTH))) }
            },
        )
        chatList.add(chat)
    }

    override fun select(chat: AssistantChat): Unit = chatList.select(chat)

    override fun close(chat: AssistantChat): Unit = chatList.close(chat, project, selection.agent.value.id)

    override fun selectAgent(choice: AgentChoice) {
        if (!host.isOn) return
        if (selection.select(choice)) restartAgent()
    }

    override fun selectConfiguration(configuration: ClaudeConfiguration) {
        if (!host.isOn) return
        if (!claude.select(configuration)) return
        settings.update { it.copy(claudeConfigurationAlias = configuration.alias) }
        restartAgent()
    }

    override fun signIn(method: SignInMethod) {
        if (!host.isOn) return
        signIns.signIn(method)
    }

    override fun setVariables(method: SignInMethod, values: Map<String, String>) {
        if (!host.isOn) return
        signIns.setVariables(selection.agent.value.id, method, values)
    }

    override val canInstallNode: Boolean get() = commands.canInstallNode
    private val _nodeInstallFailure = MutableStateFlow<String?>(null)

    override val nodeInstallFailure: StateFlow<String?> = _nodeInstallFailure.asStateFlow()

    override fun installNode() {
        if (!host.isOn) return
        _nodeInstallFailure.value = null
        scope.launch {
            try {
                commands.installNode()
                chats.value.forEach { it.retry() }
            } catch (exception: IOException) {
                _nodeInstallFailure.value = exception.message.orEmpty()
            }
        }
    }

    override fun closeVariablesForm(): Unit = signIns.closeVariablesForm()

    override fun closeSignIn(): Unit = signIns.closeSignIn()

    override fun setApiKey(apiKey: String, onKept: () -> Unit) {
        if (!host.isOn) return
        val key = apiKey.trim().takeIf { it.isNotEmpty() } ?: return
        scope.launch {
            if (!claude.keepApiKey(key)) return@launch
            onKept()
            restartAgent()
        }
    }

    override fun removeApiKey() {
        if (!host.isOn) return
        scope.launch {
            claude.forgetApiKey()
            restartAgent()
        }
    }

    private fun restartAgent() {
        endChats(isAgentReplaced = true)
        newChat()
    }

    private fun endChats(isAgentReplaced: Boolean = false) {
        val ended = chatList.takeAll()
        val owner = host.current.value
        host.afterPending {
            ended.forEach { chat -> runIgnoringFailure { chat.close() } }
            // The agent's client keeps the IDs of conversations it couldn't close, and continuing one needs it to let go.
            if (isAgentReplaced) host.replace() else if (ended.isNotEmpty() && owner?.canCloseConversations == false) host.replace(expected = owner)
            chatList.refreshSaved(project, selection.agent.value.id)
        }
    }

    /**
     * Ends every conversation, the sign-in, and the agent, when AI features are turned off.
     */
    private fun turnOff() {
        signIns.stop()
        endChats(isAgentReplaced = true)
    }

    /**
     * Ends the sign-in command and the agent, and refuses to start another agent, when Kubriko Creator quits. It blocks
     * until their processes are gone, so it is called off the main thread.
     */
    override fun close() {
        signIns.stopNow()
        host.close()
    }

    /**
     * Waits until the restarts queued so far have run, so an agent being replaced when Kubriko Creator quits is closed
     * before it exits. Called on the main thread.
     */
    public suspend fun awaitPending(): Unit = host.awaitPending()
}
