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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The assistant panel's state for as long as Kubriko Creator runs: the one [Assistant], created on first use and held
 * until Creator quits, the Claude Code configuration it signs in with, and the conversations about the open project,
 * which end when the project closes, since their tools work on whatever project is open.
 *
 * @param scope Where conversations run: the main thread, which the panel's state is read on.
 * @param createAssistant Creates the assistant running [AgentCommand], the agent through the user's `npx`.
 */
internal class AssistantController(
    private val scope: CoroutineScope,
    private val runner: ProcessRunner,
    private val store: AssistantSettingsStore,
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
    private var assistant: Assistant? = null
    private var project: File? = null
    private var hasFoundConfigurations = false
    private val discovery = CompletableDeferred<Unit>()

    /**
     * Finds the Claude Code configurations of the user's shell aliases, once per run. A remembered configuration whose
     * alias is gone falls back to the default one.
     */
    fun findConfigurations() {
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
        val replaced = assistantLock.withLock { assistant.also { assistant = null } } ?: return
        withContext(ioDispatcher) { replaced.close() }
    }

    private suspend fun assistant(): Assistant = assistantLock.withLock {
        assistant ?: run {
            if (_configuration.value.alias != null) {
                findConfigurations()
                discovery.await()
            }
            val npx = withContext(ioDispatcher) { runner.locate(NPX) } ?: throw NodeMissingException()
            createAssistant(RegistryAgent.claude.command(File(npx), _configuration.value.environment)).also { assistant = it }
        }
    }

    /**
     * Ends the agent, when Kubriko Creator quits.
     */
    override fun close() {
        assistant?.close()
        assistant = null
    }

    private companion object {
        const val NPX = "npx"
    }
}
