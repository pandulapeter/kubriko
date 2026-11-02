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
 * @param resendRefused Sends the prompts refused for want of a sign-in again.
 * @param restart Starts the agent again, ending every conversation, and a new conversation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AssistantSignIn(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val signInLauncher: AgentLauncher,
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
     * Signs in with [method]: in the panel's terminal for a terminal method, otherwise by the agent itself. Once a
     * terminal method succeeds, prompts refused for want of a sign-in are sent again.
     */
    fun signIn(method: SignInMethod) {
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
        scope.launch {
            _session.value?.stop()
            val session = try {
                SignInSession(method, withContext(ioDispatcher) { signInLauncher.launch(command) }, scope, ioDispatcher)
            } catch (_: IOException) {
                null
            } ?: return@launch
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
     * Gives the agent [agentId] the [values] of [method]'s variables, kept in the operating system's secret store, which
     * starts it again (ending every conversation) and a new conversation.
     */
    fun setVariables(agentId: String, method: SignInMethod, values: Map<String, String>) {
        val given = values.filterValues { it.isNotBlank() }.mapValues { it.value.trim() }
        if (method.variables.orEmpty().any { !it.isOptional && it.name !in given }) return
        scope.launch {
            val isKept = withContext(ioDispatcher) { agentVariables.write(agentId, given) }
            _variablesFailed.value = !isKept
            if (!isKept) return@launch
            _variablesForm.value = null
            settings.update { it.copy(agentVariables = it.agentVariables + (agentId to (it.agentVariables[agentId].orEmpty() + given.keys).distinct())) }
            restart()
        }
    }

    /**
     * The variables given for [choice], read from the secret store off the main thread.
     */
    suspend fun environmentOf(choice: AgentChoice) = withContext(ioDispatcher) { agentVariables.read(choice.id, settings.value.value.agentVariables[choice.id].orEmpty()) }

    fun closeVariablesForm() {
        _variablesForm.value = null
    }

    /**
     * Ends the sign-in command and closes the panel's terminal.
     */
    fun closeSignIn() {
        val session = _session.value ?: return
        _session.value = null
        scope.launch { session.stop() }
    }

    /**
     * Closes the variables form and ends the sign-in command, when AI features are turned off or Kubriko Creator quits.
     */
    fun stop() {
        closeVariablesForm()
        closeSignIn()
    }
}
