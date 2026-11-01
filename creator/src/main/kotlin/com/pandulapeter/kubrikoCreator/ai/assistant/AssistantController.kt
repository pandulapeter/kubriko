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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
internal class AssistantController(
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
    val errorLogFile: File? = null,
    private val conversations: SavedConversationStore? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    node: NodeRuntime? = null,
    private val isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
) : AutoCloseable {
    private val settings = MutableStateFlow(store.load())
    private val _agents = MutableStateFlow(listOf(RegistryAgent.claude))

    /**
     * The registry's agents this computer can run, Claude Agent first.
     */
    val agents = _agents.asStateFlow()
    private val _agent = MutableStateFlow(choiceOf(settings.value, _agents.value))

    /**
     * The agent the assistant runs.
     */
    val agent = _agent.asStateFlow()

    /**
     * The command line of the custom agent, whether or not it is the one chosen.
     */
    val customAgentCommand get() = settings.value.customAgentCommand
    private val claude = ClaudeAccount(scope, ioDispatcher, runner, secrets, home, settings.value.claudeConfigurationAlias)

    /**
     * The Claude Code configurations found on this computer, the default one first.
     */
    val configurations = claude.configurations

    /**
     * The Claude Code configuration Claude Agent uses.
     */
    val configuration = claude.configuration
    private val chatList = AssistantChats(scope, ioDispatcher, conversations)

    /**
     * The conversations about the open project, oldest first.
     */
    val chats = chatList.open

    /**
     * The earlier conversations about the open project with the agent chosen, newest first, which can be continued.
     */
    val savedConversations = chatList.saved

    /**
     * The conversation the panel shows.
     */
    val currentChat = chatList.current
    private val assistantLock = Mutex()
    private val assistant = MutableStateFlow<Assistant?>(null)
    private val agentsLoaded = CompletableDeferred<Unit>()
    private val commands = AgentCommands(runner, installer, home, ioDispatcher, node)
    private val agentVariables = AgentVariableStore(secrets)

    /**
     * The agent's archive being downloaded on its first start, if one is.
     */
    val download = commands.download
    private val _variablesForm = MutableStateFlow<SignInMethod?>(null)

    /**
     * The sign-in method taking environment variables the user is giving them for.
     */
    val variablesForm = _variablesForm.asStateFlow()
    private val _variablesFailed = MutableStateFlow(false)

    /**
     * Whether the last variables given couldn't be kept.
     */
    val variablesFailed = _variablesFailed.asStateFlow()

    init {
        scope.launch { loadAgents() }
        scope.launch { isAiEnabled.drop(1).filter { !it }.collect { turnOff() } }
    }

    /**
     * Who the agent works as, once it has said so.
     */
    val authStatus = assistant.flatMapLatest { it?.authStatus ?: flowOf(null) }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The ways of signing in the agent offers, once it has started.
     */
    val signInMethods = assistant.flatMapLatest { it?.signInMethods ?: flowOf(emptyList()) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val statusBeforeSignIn = MutableStateFlow<AuthStatus?>(null)

    /**
     * Whether the agent takes images in prompts, once it has started.
     */
    val acceptsImages = assistant.flatMapLatest { it?.capabilities ?: flowOf(null) }
        .map { it?.promptCapabilities?.image == true }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Whether the agent said it isn't signed in, and the user hasn't signed in since. Claude Agent reports a new sign-in
     * only at the next prompt.
     */
    val needsSignIn = combine(authStatus, statusBeforeSignIn) { status, before -> status?.isSignedIn == false && status != before }
        .stateIn(scope, SharingStarted.Eagerly, false)
    private val _signIn = MutableStateFlow<SignInSession?>(null)

    /**
     * The sign-in command running in the panel's terminal.
     */
    val signIn = _signIn.asStateFlow()

    /**
     * Whether Claude Agent uses the user's Anthropic API key, kept in the secret store, rather than a Claude Code login.
     */
    val hasApiKey = claude.hasApiKey

    /**
     * Whether this computer has somewhere to keep an API key.
     */
    val canKeepApiKey get() = claude.canKeepApiKey

    /**
     * Whether the last API key given couldn't be kept.
     */
    val apiKeyFailed = claude.apiKeyFailed
    private var project: File? = null

    /**
     * Finds the Claude Code configurations of the user's shell aliases, once per run.
     */
    fun findConfigurations() = claude.findConfigurations()

    /**
     * Makes [directory] the project conversations are about, ending those about the one before.
     */
    fun setProject(directory: File?) {
        if (directory == project) return
        project = directory
        endChats()
    }

    /**
     * Starts a conversation about the open project and shows it.
     */
    fun newChat() = openChat { owner, directory, permissions, _ -> owner.newConversation(directory, permissions, LastConversation.withProject(projectDigest(), lastConversationOf(directory))) }

    private suspend fun lastConversationOf(directory: File) = withContext(ioDispatcher) { conversations?.of(directory, _agent.value.id)?.firstOrNull() }

    /**
     * Continues [conversation], an earlier one about the open project, and shows it.
     */
    fun resume(conversation: SavedConversation) {
        if (chatList.reopen(conversation.id)) return
        openChat { owner, directory, permissions, onHistory -> owner.resumeConversation(directory, conversation.id, permissions, onHistory) }
    }

    private fun openChat(start: suspend (Assistant, File, PermissionHandler, suspend (SessionUpdate) -> Unit) -> AgentConversation) {
        if (!isAiEnabled.value) return
        val directory = project ?: return
        val agentId = _agent.value.id
        var owner: Assistant? = null
        val chat = AssistantChat(
            parentScope = scope,
            start = { permissions, onHistory -> start(assistant().also { owner = it }, directory, permissions, onHistory) },
            end = { conversation -> owner?.closeConversation(conversation) },
            toolsIdOf = { conversation -> owner?.toolsIdOf(conversation) },
            changeLog = changeLog,
            undoTurn = undoTurn,
            onTurnEnded = { id, title, lastAnswer ->
                withContext(ioDispatcher) { conversations?.save(SavedConversation(directory.absolutePath, agentId, id, title, clock(), lastAnswer?.take(LastConversation.ANSWER_LENGTH))) }
            },
        )
        chatList.add(chat)
    }

    /**
     * Shows the conversation [chat] instead.
     */
    fun select(chat: AssistantChat) = chatList.select(chat)

    /**
     * Closes [chat], showing the conversation before it; one with something in it can be continued later.
     */
    fun close(chat: AssistantChat) = chatList.close(chat, project, _agent.value.id)

    /**
     * Runs [choice] from now on, which ends every conversation and starts a new one.
     */
    fun selectAgent(choice: AgentChoice) {
        if (choice == _agent.value) return
        _agent.value = choice
        settings.update { it.copy(agentId = choice.id.takeUnless { choice.isClaude }, customAgentCommand = (choice as? AgentChoice.Custom)?.commandLine ?: it.customAgentCommand) }
        store.save(settings.value)
        restartAgent()
    }

    /**
     * Uses [configuration] from now on: its own agent, so every conversation ends, and a new one starts.
     */
    fun selectConfiguration(configuration: ClaudeConfiguration) {
        if (!claude.select(configuration)) return
        settings.update { it.copy(claudeConfigurationAlias = configuration.alias) }
        store.save(settings.value)
        restartAgent()
    }

    /**
     * Signs in with [method]: in the panel's terminal for a terminal method, otherwise by the agent itself. Once a
     * terminal method succeeds, prompts refused for want of a sign-in are sent again.
     */
    fun signIn(method: SignInMethod) {
        if (!isAiEnabled.value) return
        if (method.variables != null) {
            _variablesFailed.value = false
            _variablesForm.value = method
            return
        }
        val command = method.terminal
        if (command == null) {
            scope.launch {
                try {
                    assistant().authenticate(method)
                    chats.value.forEach { it.resendAfterSignIn() }
                } catch (exception: CancellationException) {
                    if (!currentCoroutineContext().isActive) throw exception
                } catch (_: Exception) {
                    // The agent reports the failure through the next prompt, which still asks for a sign-in.
                }
            }
            return
        }
        scope.launch {
            _signIn.value?.stop()
            val session = try {
                SignInSession(method, withContext(ioDispatcher) { signInLauncher.launch(command) }, scope, ioDispatcher)
            } catch (_: IOException) {
                null
            }
            _signIn.value = session ?: return@launch
            if (session.exitCode.first { it != null } == 0 && _signIn.value === session) {
                statusBeforeSignIn.value = authStatus.value
                _signIn.value = null
                chats.value.forEach { it.resendAfterSignIn() }
            }
        }
    }

    /**
     * Gives the agent chosen the [values] of [method]'s variables, kept in the operating system's secret store, which
     * starts it again (ending every conversation) and a new conversation.
     */
    fun setVariables(method: SignInMethod, values: Map<String, String>) {
        val agentId = _agent.value.id
        val given = values.filterValues { it.isNotBlank() }.mapValues { it.value.trim() }
        if (method.variables.orEmpty().any { !it.isOptional && it.name !in given }) return
        scope.launch {
            val isKept = withContext(ioDispatcher) { agentVariables.write(agentId, given) }
            _variablesFailed.value = !isKept
            if (!isKept) return@launch
            _variablesForm.value = null
            settings.update { it.copy(agentVariables = it.agentVariables + (agentId to (it.agentVariables[agentId].orEmpty() + given.keys).distinct())) }
            store.save(settings.value)
            restartAgent()
        }
    }

    /**
     * Whether Creator can download a Node.js of its own when the user has none.
     */
    val canInstallNode get() = commands.canInstallNode
    private val _nodeInstallFailure = MutableStateFlow<String?>(null)

    /**
     * Why the last download of Node.js failed, if it did.
     */
    val nodeInstallFailure = _nodeInstallFailure.asStateFlow()

    /**
     * Downloads a Node.js of Creator's own, then starts the conversations that couldn't start for want of one.
     */
    fun installNode() {
        if (!isAiEnabled.value) return
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

    fun closeVariablesForm() {
        _variablesForm.value = null
    }

    /**
     * Ends the sign-in command and closes the panel's terminal.
     */
    fun closeSignIn() {
        val session = _signIn.value ?: return
        _signIn.value = null
        scope.launch { session.stop() }
    }

    /**
     * Uses [apiKey] from now on, kept in the operating system's secret store, which starts the agent again (ending
     * every conversation) and a new conversation.
     *
     * @param onKept Called once the key is kept.
     */
    fun setApiKey(apiKey: String, onKept: () -> Unit = {}) {
        val key = apiKey.trim().takeIf { it.isNotEmpty() } ?: return
        scope.launch {
            if (!claude.keepApiKey(key)) return@launch
            onKept()
            restartAgent()
        }
    }

    /**
     * Forgets the API key, so that Claude Agent signs in as Claude Code does, which starts it again.
     */
    fun removeApiKey() {
        scope.launch {
            claude.forgetApiKey()
            restartAgent()
        }
    }

    private fun restartAgent() {
        endChats()
        scope.launch { replaceAssistant() }
        newChat()
    }

    private fun endChats() {
        val ended = chatList.takeAll()
        scope.launch {
            ended.forEach { it.close() }
            // The agent's client keeps the IDs of conversations it couldn't close, and continuing one needs it to let go.
            if (ended.isNotEmpty() && assistant.value?.canCloseConversations == false) replaceAssistant()
            chatList.refreshSaved(project, _agent.value.id)
        }
    }

    private suspend fun replaceAssistant() {
        val replaced = assistantLock.withLock { assistant.value.also { assistant.value = null } } ?: return
        withContext(ioDispatcher) { replaced.close() }
    }

    /**
     * The cached registry first, so that a remembered agent is known before the first conversation starts, then a fresh one.
     */
    private suspend fun loadAgents() {
        if (registry == null) {
            agentsLoaded.complete(Unit)
            return
        }
        updateAgents(withContext(ioDispatcher) { registry.cached() })
        agentsLoaded.complete(Unit)
        isAiEnabled.first { it }
        withContext(ioDispatcher) { registry.refresh() }?.let(::updateAgents)
    }

    private fun updateAgents(agents: List<RegistryAgent>) {
        _agents.value = agents
        if (assistant.value == null) _agent.value = choiceOf(settings.value, _agents.value)
    }

    private suspend fun assistant(): Assistant = assistantLock.withLock {
        check(isAiEnabled.value) { "AI features are off." }
        assistant.value ?: run {
            agentsLoaded.await()
            val choice = _agent.value
            val npx = commands.npx()
            val environment = if (choice.isClaude) claude.environment() else agentVariables.read(choice.id, settings.value.agentVariables[choice.id].orEmpty())
            createAssistant(commands.of(choice, npx, environment), npx, choice.profile).also { assistant.value = it }
        }
    }

    /**
     * Ends every conversation, the sign-in, and the agent, when AI features are turned off.
     */
    private fun turnOff() {
        endChats()
        _variablesForm.value = null
        closeSignIn()
        scope.launch { replaceAssistant() }
    }

    /**
     * Ends the agent, when Kubriko Creator quits.
     */
    override fun close() {
        _signIn.value?.let { session -> scope.launch { session.stop() } }
        assistant.value?.close()
        assistant.value = null
    }

    private companion object {

        /**
         * The agent [settings] remember, out of [agents]: Claude Agent if it isn't among them.
         */
        fun choiceOf(settings: AssistantSettings, agents: List<RegistryAgent>): AgentChoice = when (val id = settings.agentId) {
            AgentChoice.Custom.ID -> settings.customAgentCommand?.let(AgentChoice::Custom)
            else -> agents.firstOrNull { it.id == id }?.let(AgentChoice::Registry)
        } ?: AgentChoice.Registry(RegistryAgent.claude)
    }
}
