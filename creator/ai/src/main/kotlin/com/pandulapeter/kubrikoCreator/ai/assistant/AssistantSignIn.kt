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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Signing the agent in: a terminal method in the panel's terminal, an agent method by the agent itself, and a method taking
 * environment variables through a form, which keeps them in the operating system's secret store for the agent's next start.
 *
 * @param scope Where sign-ins run: the main thread, which the panel's state is read on.
 * @param signInLauncher Runs the sign-in commands of an agent's terminal sign-in methods.
 * @param systemTerminal Runs them in the system's terminal instead, for one that needs a terminal device, where this
 *   computer has a terminal.
 * @param resendRefused Sends the prompts refused for want of a sign-in again.
 * @param restart Starts the agent again, ending every conversation, and a new conversation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AssistantSignIn(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val signInLauncher: AgentLauncher,
    private val systemTerminal: AgentLauncher?,
    secrets: SecretStore,
    private val settings: AssistantSettingsState,
    private val host: AssistantHost<Assistant>,
    private val resendRefused: () -> Unit,
    private val restart: () -> Unit,
) {
    private val agentVariables = AgentVariableStore(secrets)
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

    /**
     * Who the agent works as, once it has said so.
     */
    val authStatus = host.current.flatMapLatest { it?.authStatus ?: flowOf(null) }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The ways of signing in the agent offers, once it has started.
     */
    val signInMethods = host.current.flatMapLatest { it?.signInMethods ?: flowOf(emptyList()) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val statusBeforeSignIn = MutableStateFlow<AuthStatus?>(null)

    /**
     * Whether the agent said it isn't signed in, and the user hasn't signed in since. Claude Agent reports a new sign-in
     * only at the next prompt.
     */
    val needsSignIn = combine(authStatus, statusBeforeSignIn) { status, before -> status?.isSignedIn == false && status != before }
        .stateIn(scope, SharingStarted.Eagerly, false)
    private val _session = MutableStateFlow<SignInSession?>(null)

    /**
     * The sign-in command running in the panel's terminal.
     */
    val session = _session.asStateFlow()

    /**
     * Whether a terminal method can run in the system's terminal.
     */
    val canUseSystemTerminal get() = systemTerminal != null
    private val _isOpeningSystemTerminal = MutableStateFlow(false)

    /**
     * Whether the system's terminal is being opened, during which another can't be.
     */
    val isOpeningSystemTerminal = _isOpeningSystemTerminal.asStateFlow()
    private val _systemTerminalFailed = MutableStateFlow(false)

    /**
     * Whether the system's terminal couldn't be opened the last time, leaving the sign-in that was there before running.
     */
    val systemTerminalFailed = _systemTerminalFailed.asStateFlow()

    /**
     * Signs in with [method]: in the panel's terminal for a terminal method (or, with [isInSystemTerminal], in the
     * system's, ending the sign-in there before only once the terminal opened), otherwise by the agent itself. Once a
     * terminal method succeeds, prompts refused for want of a sign-in are sent again.
     */
    fun signIn(method: SignInMethod, isInSystemTerminal: Boolean = false) {
        if (method.variables != null) {
            _variablesFailed.value = false
            _variablesForm.value = method
            return
        }
        val command = method.terminal
        if (command == null) {
            // The agent reports a failure through the next prompt, which still asks for a sign-in.
            scope.launch {
                runIgnoringFailure {
                    host.get().authenticate(method)
                    resendRefused()
                }
            }
            return
        }
        val launcher = if (isInSystemTerminal) systemTerminal?.takeUnless { _isOpeningSystemTerminal.value } ?: return else signInLauncher
        _systemTerminalFailed.value = false
        if (isInSystemTerminal) _isOpeningSystemTerminal.value = true
        scope.launch {
            if (!isInSystemTerminal) _session.value?.stop()
            val session = try {
                SignInSession(method, withContext(ioDispatcher) { launcher.launch(command) }, scope, ioDispatcher, isInSystemTerminal)
            } catch (_: IOException) {
                null
            } finally {
                if (isInSystemTerminal) _isOpeningSystemTerminal.value = false
            }
            if (session == null) {
                // Only once it no longer says it is opening, so whoever sees the failure sees it done.
                if (isInSystemTerminal) _systemTerminalFailed.value = true
                return@launch
            }
            val displaced = _session.value
            _session.value = session
            displaced?.stop()
            if (session.exitCode.first { it != null } == 0 && _session.value === session) {
                statusBeforeSignIn.value = authStatus.value
                _session.value = null
                resendRefused()
            }
        }
    }

    /**
     * Gives the agent's [account] ([AgentConfigurations.accountOf]) the [values] of [method]'s variables, kept in the
     * operating system's secret store, which starts it again (ending every conversation) and a new conversation.
     */
    fun setVariables(account: String, method: SignInMethod, values: Map<String, String>) {
        val given = values.filterValues { it.isNotBlank() }.mapValues { it.value.trim() }
        if (method.variables.orEmpty().any { !it.isOptional && it.name !in given }) return
        scope.launch {
            val isKept = withContext(ioDispatcher) { agentVariables.write(account, given) }
            _variablesFailed.value = !isKept
            if (!isKept) return@launch
            _variablesForm.value = null
            settings.update { it.copy(agentVariables = it.agentVariables + (account to (it.agentVariables[account].orEmpty() + given.keys).distinct())) }
            restart()
        }
    }

    /**
     * The variables given for the agent's [account] ([AgentConfigurations.accountOf]), read from the secret store off the
     * main thread.
     */
    suspend fun environmentOf(account: String) = withContext(ioDispatcher) { agentVariables.read(account, settings.value.value.agentVariables[account].orEmpty()) }

    fun closeVariablesForm() {
        _variablesForm.value = null
    }

    /**
     * Ends the sign-in command and closes the panel's terminal.
     */
    fun closeSignIn() {
        _systemTerminalFailed.value = false
        val session = _session.value ?: return
        _session.value = null
        scope.launch { session.stop() }
    }

    /**
     * Closes the variables form and ends the sign-in command, when AI features are turned off.
     */
    fun stop() {
        closeVariablesForm()
        closeSignIn()
    }

    /**
     * Closes the variables form and ends the sign-in command before returning, when Kubriko Creator quits. It blocks, so it
     * is called off the main thread.
     */
    fun stopNow() {
        _variablesForm.value = null
        _session.getAndUpdate { null }?.stopNow()
    }
}
