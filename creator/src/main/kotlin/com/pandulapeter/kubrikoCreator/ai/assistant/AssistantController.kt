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

import com.pandulapeter.kubrikoCreator.mcp.clients.ProcessRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The assistant panel's state for as long as Kubriko Creator runs: the one [Assistant], created on first use and held
 * until Creator quits, the Claude Code configuration it signs in with, and the conversations about the open project,
 * which end when the project closes, since their tools work on whatever project is open.
 *
 * @param scope Where conversations run: the main thread, which the panel's state is read on.
 * @param secrets Where the API key is kept.
 * @param signInLauncher Runs the sign-in commands of an agent's terminal sign-in methods.
 * @param createAssistant Creates the assistant running [AgentCommand], the agent through the user's `npx`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AssistantController(
    private val scope: CoroutineScope,
    private val runner: ProcessRunner,
    private val store: AssistantSettingsStore,
    private val secrets: SecretStore,
    private val signInLauncher: AgentLauncher,
    private val ioDispatcher: CoroutineDispatcher,
    private val createAssistant: (AgentCommand) -> Assistant,
    private val home: String = System.getProperty("user.home"),
) : AutoCloseable {
    private val settings = MutableStateFlow(store.load())
    private val _configurations = MutableStateFlow(listOf(ClaudeConfiguration.default))

    /**
     * The Claude Code configurations found on this computer, the default one first.
     */
    val configurations = _configurations.asStateFlow()
    private val _configuration = MutableStateFlow(
        settings.value.claudeConfigurationAlias?.let(ClaudeConfiguration::ofAlias) ?: ClaudeConfiguration.default,
    )

    /**
     * The Claude Code configuration the assistant uses.
     */
    val configuration = _configuration.asStateFlow()
    private val _chats = MutableStateFlow(emptyList<AssistantChat>())

    /**
     * The conversations about the open project, oldest first.
     */
    val chats = _chats.asStateFlow()
    private val _currentChat = MutableStateFlow<AssistantChat?>(null)

    /**
     * The conversation the panel shows.
     */
    val currentChat = _currentChat.asStateFlow()
    private val assistantLock = Mutex()
    private val assistant = MutableStateFlow<Assistant?>(null)

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
    private val _hasApiKey = MutableStateFlow(false)

    /**
     * Whether Claude Agent uses the user's Anthropic API key, kept in [secrets], rather than a Claude Code login.
     */
    val hasApiKey = _hasApiKey.asStateFlow()

    /**
     * Whether this computer has somewhere to keep an API key.
     */
    val canKeepApiKey get() = secrets.isAvailable
    private val _apiKeyFailed = MutableStateFlow(false)

    /**
     * Whether the last API key given couldn't be kept.
     */
    val apiKeyFailed = _apiKeyFailed.asStateFlow()
    private val apiKeyLookup = CompletableDeferred<Unit>()
    private var hasLookedUpApiKey = false
    private var project: File? = null
    private var hasFoundConfigurations = false
    private val discovery = CompletableDeferred<Unit>()

    /**
     * Finds the Claude Code configurations of the user's shell aliases, once per run. A remembered configuration whose
     * alias is gone falls back to the default one.
     */
    fun findConfigurations() {
        lookUpApiKey()
        if (hasFoundConfigurations) return
        hasFoundConfigurations = true
        scope.launch {
            val found = ClaudeConfiguration.discover(withContext(ioDispatcher) { runner.aliases() }, home)
            _configurations.value = found
            _configuration.value.alias?.let { alias -> _configuration.value = found.firstOrNull { it.alias == alias } ?: ClaudeConfiguration.default }
            discovery.complete(Unit)
        }
    }

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
    fun newChat() {
        val directory = project ?: return
        var owner: Assistant? = null
        val chat = AssistantChat(
            parentScope = scope,
            start = { permissions -> assistant().also { owner = it }.newConversation(directory, permissions) },
            end = { conversation -> owner?.closeConversation(conversation) },
        )
        _chats.update { it + chat }
        _currentChat.value = chat
    }

    /**
     * Shows the conversation [chat] instead.
     */
    fun select(chat: AssistantChat) {
        if (chat in _chats.value) _currentChat.value = chat
    }

    /**
     * Ends [chat], showing the conversation before it.
     */
    fun close(chat: AssistantChat) {
        val index = _chats.value.indexOf(chat)
        if (index < 0) return
        _chats.update { it - chat }
        if (_currentChat.value === chat) _currentChat.value = _chats.value.getOrNull((index - 1).coerceAtLeast(0))
        scope.launch { chat.close() }
    }

    /**
     * Uses [configuration] from now on: its own agent, so every conversation ends, and a new one starts.
     */
    fun selectConfiguration(configuration: ClaudeConfiguration) {
        if (configuration == _configuration.value) return
        _configuration.value = configuration
        settings.update { it.copy(claudeConfigurationAlias = configuration.alias) }
        store.save(settings.value)
        restartAgent()
    }

    /**
     * Signs in with [method]: in the panel's terminal for a terminal method, otherwise by the agent itself. Once a
     * terminal method succeeds, prompts refused for want of a sign-in are sent again.
     */
    fun signIn(method: SignInMethod) {
        val command = method.terminal
        if (command == null) {
            scope.launch {
                try {
                    assistant().authenticate(method)
                    _chats.value.forEach { it.resendAfterSignIn() }
                } catch (exception: CancellationException) {
                    throw exception
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
                _chats.value.forEach { it.resendAfterSignIn() }
            }
        }
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
            val isKept = withContext(ioDispatcher) { secrets.write(API_KEY, key) }
            _apiKeyFailed.value = !isKept
            if (!isKept) return@launch
            _hasApiKey.value = true
            onKept()
            restartAgent()
        }
    }

    /**
     * Forgets the API key, so that Claude Agent signs in as Claude Code does, which starts it again.
     */
    fun removeApiKey() {
        scope.launch {
            withContext(ioDispatcher) { secrets.delete(API_KEY) }
            _hasApiKey.value = false
            restartAgent()
        }
    }

    private fun lookUpApiKey() {
        if (hasLookedUpApiKey) return
        hasLookedUpApiKey = true
        scope.launch {
            _hasApiKey.value = withContext(ioDispatcher) { secrets.read(API_KEY) } != null
            apiKeyLookup.complete(Unit)
        }
    }

    private fun restartAgent() {
        endChats()
        scope.launch { replaceAssistant() }
        newChat()
    }

    private fun endChats() {
        val ended = _chats.value
        _chats.value = emptyList()
        _currentChat.value = null
        scope.launch { ended.forEach { it.close() } }
    }

    private suspend fun replaceAssistant() {
        val replaced = assistantLock.withLock { assistant.value.also { assistant.value = null } } ?: return
        withContext(ioDispatcher) { replaced.close() }
    }

    private suspend fun assistant(): Assistant = assistantLock.withLock {
        assistant.value ?: run {
            if (_configuration.value.alias != null) {
                findConfigurations()
                discovery.await()
            }
            lookUpApiKey()
            apiKeyLookup.await()
            val apiKey = if (_hasApiKey.value) withContext(ioDispatcher) { secrets.read(API_KEY) } else null
            val npx = withContext(ioDispatcher) { runner.locate(NPX) } ?: throw NodeMissingException()
            val environment = _configuration.value.environment + listOfNotNull(apiKey?.let { ANTHROPIC_API_KEY to it })
            createAssistant(RegistryAgent.claude.command(File(npx), environment)).also { assistant.value = it }
        }
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
        const val NPX = "npx"
        const val API_KEY = "anthropic-api-key"
        const val ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY"
    }
}
