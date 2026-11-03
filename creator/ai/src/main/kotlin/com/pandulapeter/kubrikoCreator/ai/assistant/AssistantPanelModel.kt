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

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * What the assistant panel shows and does: the agent chosen, its conversations, and signing in. What an agent offers
 * beyond the fallbacks (an API key, Claude Code configurations) is already accounted for, so the panel shows these as
 * they are.
 */
public interface AssistantPanelModel {

    /**
     * The agent the assistant runs.
     */
    public val agent: StateFlow<AgentChoice>

    /**
     * The registry's agents this computer can run, Claude Agent first.
     */
    public val agents: StateFlow<List<RegistryAgent>>

    /**
     * The command line of the custom agent, whether or not it is the one chosen.
     */
    public val customAgentCommand: String?

    /**
     * The conversations about the open project, oldest first.
     */
    public val chats: StateFlow<List<AssistantChat>>

    /**
     * The conversation the panel shows.
     */
    public val currentChat: StateFlow<AssistantChat?>

    /**
     * The earlier conversations about the open project with the agent chosen, newest first, which can be continued.
     */
    public val savedConversations: StateFlow<List<SavedConversation>>

    /**
     * Who the agent works as, once it has said so.
     */
    public val authStatus: StateFlow<AuthStatus?>

    /**
     * The ways of signing in the agent offers, once it has started.
     */
    public val signInMethods: StateFlow<List<SignInMethod>>

    /**
     * Whether the agent said it isn't signed in, and the user hasn't signed in since. Claude Agent reports a new sign-in
     * only at the next prompt.
     */
    public val needsSignIn: StateFlow<Boolean>

    /**
     * The sign-in command running in the panel's terminal.
     */
    public val signIn: StateFlow<SignInSession?>

    /**
     * Whether the agent chosen uses the user's Anthropic API key, kept in the secret store, rather than a Claude Code
     * login: never for an agent without a Claude account ([AgentProfile.usesClaudeAccount]).
     */
    public val hasApiKey: StateFlow<Boolean>

    /**
     * Whether the agent chosen takes an API key and this computer has somewhere to keep it.
     */
    public val canKeepApiKey: Boolean

    /**
     * Whether the last API key given couldn't be kept.
     */
    public val apiKeyFailed: StateFlow<Boolean>

    /**
     * The Claude Code configuration Claude Agent uses.
     */
    public val configuration: StateFlow<ClaudeConfiguration>

    /**
     * The Claude Code configurations found on this computer, the default one first, or none for an agent without a
     * Claude account.
     */
    public val configurations: StateFlow<List<ClaudeConfiguration>>

    /**
     * The agent's archive being downloaded on its first start, if one is.
     */
    public val download: StateFlow<AgentDownload?>

    /**
     * Whether Creator can download a Node.js of its own when the user has none.
     */
    public val canInstallNode: Boolean

    /**
     * Why the last download of Node.js failed, if it did.
     */
    public val nodeInstallFailure: StateFlow<String?>

    /**
     * The sign-in method taking environment variables the user is giving them for.
     */
    public val variablesForm: StateFlow<SignInMethod?>

    /**
     * Whether the last variables given couldn't be kept.
     */
    public val variablesFailed: StateFlow<Boolean>

    /**
     * Where agents' standard error is kept ([AgentErrorLog]), for the user to open.
     */
    public val errorLogFile: File?

    /**
     * Finds the Claude Code configurations of the user's shell aliases, once per run.
     */
    public fun findConfigurations()

    /**
     * Starts a conversation about the open project and shows it.
     */
    public fun newChat()

    /**
     * Continues [conversation], an earlier one about the open project, and shows it.
     */
    public fun resume(conversation: SavedConversation)

    /**
     * Shows the conversation [chat] instead.
     */
    public fun select(chat: AssistantChat)

    /**
     * Closes [chat], showing the conversation before it; one with something in it can be continued later.
     */
    public fun close(chat: AssistantChat)

    /**
     * Runs [choice] from now on, which ends every conversation and starts a new one.
     */
    public fun selectAgent(choice: AgentChoice)

    /**
     * Uses [configuration] from now on: its own agent, so every conversation ends, and a new one starts.
     */
    public fun selectConfiguration(configuration: ClaudeConfiguration)

    /**
     * Signs in with [method]: in the panel's terminal for a terminal method, otherwise by the agent itself. Once a
     * terminal method succeeds, prompts refused for want of a sign-in are sent again.
     */
    public fun signIn(method: SignInMethod)

    /**
     * Ends the sign-in command and closes the panel's terminal.
     */
    public fun closeSignIn()

    /**
     * Uses [apiKey] from now on, kept in the operating system's secret store, which starts the agent again (ending
     * every conversation) and a new conversation.
     *
     * @param onKept Called once the key is kept.
     */
    public fun setApiKey(apiKey: String, onKept: () -> Unit = {})

    /**
     * Forgets the API key, so that Claude Agent signs in as Claude Code does, which starts it again.
     */
    public fun removeApiKey()

    /**
     * Downloads a Node.js of Creator's own, then starts the conversations that couldn't start for want of one.
     */
    public fun installNode()

    /**
     * Gives the agent chosen the [values] of [method]'s variables, kept in the operating system's secret store, which
     * starts it again (ending every conversation) and a new conversation.
     */
    public fun setVariables(method: SignInMethod, values: Map<String, String>)

    /**
     * Closes the form for a sign-in method's variables without keeping them.
     */
    public fun closeVariablesForm()
}
