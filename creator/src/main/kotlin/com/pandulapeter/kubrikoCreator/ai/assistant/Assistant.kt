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

import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The in-app assistant: an [agent] whose conversations each reach Kubriko Creator's tools at an address of their own on
 * [toolServer], which the assistant starts on a free port the first time it is needed, whatever the AI connection panel's
 * switch says. Closing a conversation turns its tools off.
 *
 * @param toolServer A server of its own, not the one outside clients connect to.
 * @param profile What the agent gets beyond the fallbacks chosen from its `initialize` answer.
 * @param systemPrompt What a conversation is told at its start, given what it is told about the project.
 * @param npx Runs `mcp-remote` for an agent that takes no HTTP MCP servers.
 * @param agentFolders Where an agent that doesn't work in the project's folder ([AgentProfile.usesProjectFolder]) gets
 *   one of its own per project.
 */
internal class Assistant(
    private val agent: AcpAgent,
    private val toolServer: McpToolServer,
    private val ioDispatcher: CoroutineDispatcher,
    private val profile: AgentProfile = AgentProfile.fallback,
    private val systemPrompt: (projectContext: String?) -> String? = { null },
    private val npx: File? = null,
    private val agentFolders: File = File(System.getProperty("java.io.tmpdir"), "kubriko-creator-agents"),
) : AutoCloseable {
    private val serverStart = Mutex()
    private var port: Int? = null
    private val toolConversations = ConcurrentHashMap<AgentConversation, String>()
    val authStatus get() = agent.authStatus
    val signInMethods get() = agent.signInMethods
    val capabilities get() = agent.capabilities

    /**
     * Signs in with [method], which the agent runs by itself.
     *
     * @throws AgentStartException If the agent couldn't be started.
     */
    suspend fun authenticate(method: SignInMethod) = agent.authenticate(method.id)

    /**
     * Starts a conversation about the project in [projectDirectory].
     *
     * @param projectContext What the conversation is told about the project at its start: its digest.
     * @throws AgentStartException If the agent couldn't be started.
     * @throws java.io.IOException If the tools couldn't be served.
     */
    suspend fun newConversation(projectDirectory: File, permissions: PermissionHandler = PermissionHandler.Reject, projectContext: String? = null): AgentConversation {
        val port = toolServerPort()
        val tools = toolServer.openConversation()
        val prompt = systemPrompt(projectContext)
        val conversation = try {
            agent.newConversation(
                workingDirectory = if (profile.usesProjectFolder) projectDirectory else agentFolderOf(projectDirectory),
                mcpServers = { capabilities ->
                    listOf(ConversationToolServer.of(capabilities, profile, "http://${McpToolServer.HOST}:$port${tools.path}", tools.token, npx))
                },
                meta = prompt?.let { profile.sessionMeta?.invoke(it) },
                permissions = PermissionHandler.allowingCreatorTools(if (profile.keepsOffFiles) permissions else PermissionHandler.rejectingChanges(permissions)),
                firstPromptLead = prompt.takeIf { profile.sessionMeta == null },
            )
        } catch (exception: Exception) {
            toolServer.closeConversation(tools.id)
            throw exception
        }
        toolConversations[conversation] = tools.id
        return conversation
    }

    private suspend fun agentFolderOf(projectDirectory: File) = withContext(ioDispatcher) {
        val name = BinaryAgentInstaller.safeName(projectDirectory.name) + "-" + Integer.toHexString(projectDirectory.absolutePath.hashCode())
        File(agentFolders, name).also { it.mkdirs() }
    }

    /**
     * The ID of the address at which [conversation] reaches the tools, which its AI steps carry (`AiStepOrigin`).
     */
    fun toolsIdOf(conversation: AgentConversation) = toolConversations[conversation]

    /**
     * Ends [conversation] and turns its tools off.
     */
    suspend fun closeConversation(conversation: AgentConversation) {
        toolConversations.remove(conversation)?.let(toolServer::closeConversation)
        conversation.close()
    }

    private suspend fun toolServerPort() = serverStart.withLock {
        port ?: withContext(ioDispatcher) { toolServer.start(port = 0, token = null) }.also { port = it }
    }

    /**
     * Ends the agent and stops serving the tools.
     */
    override fun close() {
        agent.close()
        toolServer.stop()
    }
}
