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
 * created on first use and held until Creator quits or another agent is chosen, the configuration each agent runs as,
 * and the conversations about the open project, which end when the project closes, since their tools work on whatever
 * project is open.
 *
 * @param scope Where conversations run: the main thread, which the panel's state is read on.
 * @param storage What the assistant keeps on this computer.
 * @param signInLauncher Runs the sign-in commands of an agent's terminal sign-in methods.
 * @param systemTerminal Runs them in the system's terminal instead, where this computer has one.
 * @param createAssistant Creates the assistant running [AgentCommand] with [AgentProfile], given the user's `npx` where
 *   there is one.
 * @param openProject What conversations read and undo in the open project.
 * @param sources Where agents come from, all null for Claude Agent alone.
 * @param isAiEnabled Whether AI features are on: while they are off, no agent starts, no conversation opens, the registry
 *   isn't fetched, and turning them off ends what runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class AssistantController(
    private val scope: CoroutineScope,
    private val runner: ProcessRunner,
    private val storage: AssistantStorage,
    private val signInLauncher: AgentLauncher,
    private val systemTerminal: AgentLauncher? = null,
    private val ioDispatcher: CoroutineDispatcher,
    private val createAssistant: (AgentCommand, npx: File?, AgentProfile) -> Assistant,
    private val openProject: OpenProjectAccess = OpenProjectAccess.None,
    private val sources: AgentSources = AgentSources(),
    private val home: String = System.getProperty("user.home"),
    private val clock: () -> Long = System::currentTimeMillis,
    private val isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
) : AssistantPanelModel, AutoCloseable {
    private val settings = AssistantSettingsState(scope, storage.settings, ioDispatcher)

    override val errorLogFile: File? = storage.errorLogFile

    /**
     * Built before [selection], whose registry loading starts on [scope]'s thread while this constructor still runs and
     * asks whether an agent runs.
     */
    private val host: AssistantHost<Assistant> = AssistantHost(scope, ioDispatcher, isAiEnabled) {
        selection.awaitLoaded()
        val choice = selection.agent.value
        val npx = commands.npx()
        val environment = choice.profile.environment + configurationList.environmentOf(choice) + if (choice.isClaude) claudeApiKey.environment() else signIns.environmentOf(configurationList.accountOf(choice))
        createAssistant(commands.of(choice, npx, environment), npx, choice.profile)
    }
    private val selection: AgentSelection = AgentSelection(scope, settings, sources.registry, ioDispatcher, isAiEnabled, isAgentRunning = { host.current.value != null })

    override val agents: StateFlow<List<RegistryAgent>> = selection.agents

    override val agent: StateFlow<AgentChoice> = selection.agent

    override val customAgentCommand: String? get() = selection.customAgentCommand

    override val agentsChangingFilesUnasked: StateFlow<Set<String>> = settings.value.map { it.agentsChangingFilesUnasked }
        .stateIn(scope, SharingStarted.Eagerly, settings.value.value.agentsChangingFilesUnasked)
    private val configurationList = AgentConfigurations(scope, ioDispatcher, runner, home, agent, settings.value.value.configurationAliases)

    override val configurations: StateFlow<List<AgentConfiguration>> = configurationList.configurations

    override val configuration: StateFlow<AgentConfiguration?> = configurationList.configuration
    private val claudeApiKey = ClaudeApiKey(scope, ioDispatcher, storage.secrets)
    private val chatList = AssistantChats(scope, ioDispatcher, storage.conversations)

    override val chats: StateFlow<List<AssistantChat>> = chatList.open

    override val savedConversations: StateFlow<List<SavedConversation>> = chatList.saved

    override val currentChat: StateFlow<AssistantChat?> = chatList.current
    private val commands = AgentCommands(runner, sources.installer, home, ioDispatcher, sources.node)
    private val signIns: AssistantSignIn = AssistantSignIn(
        scope = scope,
        ioDispatcher = ioDispatcher,
        signInLauncher = signInLauncher,
        systemTerminal = systemTerminal,
        secrets = storage.secrets,
        settings = settings,
        host = host,
        resendRefused = { chats.value.forEach { it.resendAfterSignIn() } },
        restart = { restartAgent() },
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

    override val hasApiKey: StateFlow<Boolean> = combine(claudeApiKey.hasApiKey, agent) { hasApiKey, choice -> hasApiKey && choice.profile.usesAnthropicApiKey }
        .stateIn(scope, SharingStarted.Eagerly, claudeApiKey.hasApiKey.value && agent.value.profile.usesAnthropicApiKey)

    override val canKeepApiKey: Boolean get() = claudeApiKey.canKeepApiKey && agent.value.profile.usesAnthropicApiKey

    override val apiKeyFailed: StateFlow<Boolean> = claudeApiKey.apiKeyFailed
    private var project: File? = null

    override fun findConfigurations() {
        if (agent.value.profile.usesAnthropicApiKey) claudeApiKey.lookUpApiKey()
        configurationList.find()
    }

    /**
     * Makes [directory] the project conversations are about, ending those about the one before, and opens a conversation
     * about it right away when the chosen agent has worked before ([startIfSetUp]).
     */
    public fun setProject(directory: File?) {
        if (directory == project) return
        project = directory
        endChats()
        if (directory == null) {
            pendingWritersRoom = null
            return
        }
        val writersRoom = pendingWritersRoom
        pendingWritersRoom = null
        if (writersRoom != null) startWritersRoom(writersRoom) else startIfSetUp(directory)
    }

    /**
     * The writers' room asked for before the project reached the assistant, started once it does.
     */
    private var pendingWritersRoom: String? = null

    /**
     * Opens a conversation about [directory] while the panel may still be hidden, so that what needs one taking prompts
     * (writing descriptions, for one) works from the start, if the chosen agent has a saved conversation: one that
     * finished a turn, so it is installed and signed in, and starting it fetches and asks for nothing.
     */
    private fun startIfSetUp(directory: File) {
        if (!host.isOn) return
        scope.launch {
            selection.awaitLoaded()
            val agentId = selection.agent.value.id
            val hasWorked = withContext(ioDispatcher) { storage.conversations?.hasAny(agentId) } == true
            if (hasWorked && project == directory && selection.agent.value.id == agentId && chatList.current.value == null) newChat()
        }
    }

    override fun newChat() {
        if (!host.isOn) return
        chatList.open.value.firstOrNull { it.isEmpty.value }?.let { empty ->
            chatList.select(empty)
            return
        }
        openChat { owner, directory, permissions, _ -> owner.newConversation(directory, permissions, LastConversation.withProject(openProject.digest(), lastConversationOf(directory))) }
    }

    override fun startWritersRoom(prompt: String) {
        if (!host.isOn) return
        if (project == null) {
            // The workspace that asks may be shown just before the project reaches the assistant.
            pendingWritersRoom = prompt
            return
        }
        openChat(firstPrompt = prompt, isWritersRoom = true) { owner, directory, permissions, _ ->
            owner.newConversation(directory, permissions, LastConversation.withProject(openProject.digest(), null), rules = AssistantSystemPrompt.WRITERS_ROOM)
        }
    }

    private suspend fun lastConversationOf(directory: File) = withContext(ioDispatcher) { storage.conversations?.of(directory, selection.agent.value.id)?.firstOrNull() }

    override fun resume(conversation: SavedConversation) {
        if (!host.isOn) return
        chatList.onResumed(conversation.id)
        openChat(lastMessageAt = conversation.updatedAt, isWritersRoom = conversation.isWritersRoom) { owner, directory, permissions, onHistory ->
            owner.resumeConversation(directory, conversation.id, permissions, onHistory, rules = AssistantSystemPrompt.WRITERS_ROOM.takeIf { conversation.isWritersRoom })
        }
    }

    private fun openChat(lastMessageAt: Long? = null, firstPrompt: String? = null, isWritersRoom: Boolean = false, start: suspend (Assistant, File, PermissionHandler, suspend (SessionUpdate) -> Unit) -> AgentConversation) {
        val directory = project ?: return
        val agentId = selection.agent.value.id
        val agentIdentity = selection.agent.value.identity
        var owner: Assistant? = null
        val chat = AssistantChat(
            parentScope = scope,
            start = { permissions, onHistory -> start(host.get().also { owner = it }, directory, permissions, onHistory) },
            end = { conversation -> owner?.closeConversation(conversation) },
            toolsIdOf = { conversation -> owner?.toolsIdOf(conversation) },
            forgetReads = { conversation -> owner?.forgetReads(conversation) },
            changeLog = { openProject.changeLog() },
            undoTurn = { toolsId, sinceRevision -> openProject.undoTurn(toolsId, sinceRevision) },
            onTurnEnded = { id, title, lastAnswer, toolsId ->
                val summary = toolsId?.let(openProject::takeSummary)
                withContext(ioDispatcher) {
                    storage.conversations?.save(SavedConversation(directory.absolutePath, agentId, id, title, clock(), lastAnswer?.take(LastConversation.ANSWER_LENGTH), isWritersRoom, summary))
                }
            },
            onChangedFilesUnasked = {
                settings.update { it.takeIf { agentIdentity in it.agentsChangingFilesUnasked } ?: it.withChangingFilesUnasked(agentIdentity) }
            },
            lastMessageAt = lastMessageAt,
            clock = clock,
            firstPrompt = firstPrompt,
        )
        chatList.add(chat)
    }

    override fun select(chat: AssistantChat): Unit = chatList.select(chat)

    override fun delete(chat: AssistantChat) {
        chatList.delete(chat, project, selection.agent.value.id)
        if (chatList.open.value.isEmpty()) newChat()
    }

    override fun selectAgent(choice: AgentChoice, isPanelShown: Boolean) {
        if (!host.isOn) return
        if (selection.select(choice)) restartAgent(isPanelShown)
    }

    override fun selectConfiguration(configuration: AgentConfiguration, isPanelShown: Boolean) {
        if (!host.isOn) return
        if (!configurationList.select(configuration)) return
        settings.update { it.copy(configurationAliases = configurationList.chosenAliases) }
        // Saved conversations don't say which account they ran as, so another configuration may still need a sign-in.
        restartAgent(isPanelShown, mayStartIfWorked = false)
    }

    override fun signIn(method: SignInMethod) {
        if (!host.isOn) return
        signIns.signIn(method)
    }

    override val canSignInInSystemTerminal: Boolean get() = signIns.canUseSystemTerminal

    override val isOpeningSystemTerminal: StateFlow<Boolean> = signIns.isOpeningSystemTerminal

    override val systemTerminalFailed: StateFlow<Boolean> = signIns.systemTerminalFailed

    override fun signInInSystemTerminal(method: SignInMethod) {
        if (!host.isOn) return
        signIns.signIn(method, isInSystemTerminal = true)
    }

    override fun setVariables(method: SignInMethod, values: Map<String, String>) {
        if (!host.isOn) return
        signIns.setVariables(configurationList.accountOf(selection.agent.value), method, values)
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
            if (!claudeApiKey.keepApiKey(key)) return@launch
            onKept()
            restartAgent()
        }
    }

    override fun removeApiKey() {
        if (!host.isOn) return
        scope.launch {
            claudeApiKey.forgetApiKey()
            restartAgent()
        }
    }

    /**
     * Ends every conversation and opens a new one with the agent as it is now: right away when [isPanelShown], otherwise only
     * for an agent that has worked before when [mayStartIfWorked], leaving the rest to the panel. A writers' room whose idea
     * the agent hasn't answered is started again with it, so the idea isn't lost.
     */
    private fun restartAgent(isPanelShown: Boolean = true, mayStartIfWorked: Boolean = true) {
        val unansweredWritersRooms = chatList.open.value.mapNotNull { it.unansweredFirstPrompt }
        endChats(isAgentReplaced = true)
        when {
            unansweredWritersRooms.isNotEmpty() -> unansweredWritersRooms.forEach(::startWritersRoom)
            isPanelShown -> newChat()
            mayStartIfWorked -> project?.let(::startIfSetUp)
        }
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
