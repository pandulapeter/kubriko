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

import com.agentclientprotocol.model.AgentCapabilities
import com.agentclientprotocol.model.McpServer
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.io.IOException
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
public class Assistant(
    private val agent: AcpAgent,
    private val toolServer: McpToolServer,
    private val ioDispatcher: CoroutineDispatcher,
    private val profile: AgentProfile = AgentProfile.fallback,
    private val systemPrompt: (projectContext: String?) -> String? = { null },
    private val npx: File? = null,
    private val agentFolders: File = File(settingsDirectory(), "agents/workspaces"),
) : AutoCloseable {
    private val serverStart = Mutex()
    private var port: Int? = null

    @Volatile
    private var isClosed = false
    private val toolConversations = ConcurrentHashMap<AgentConversation, String>()
    internal val authStatus get() = agent.authStatus
    internal val signInMethods get() = agent.signInMethods
    internal val capabilities get() = agent.capabilities

    /**
     * Whether the agent forgets the conversations closed, so that they can be continued later in the same run; otherwise
     * the agent's own client keeps their IDs taken until the agent starts again.
     */
    internal val canCloseConversations get() = agent.capabilities.value?.sessionCapabilities?.close != null

    /**
     * Signs in with [method], which the agent runs by itself.
     *
     * @throws AgentStartException If the agent couldn't be started.
     */
    internal suspend fun authenticate(method: SignInMethod) = agent.authenticate(method.id)

    /**
     * Starts a conversation about the project in [projectDirectory].
     *
     * @param projectContext What the conversation is told about the project at its start: its digest.
     * @param rules What this conversation alone is told after that, such as the writers' room's
     *   ([AssistantSystemPrompt.WRITERS_ROOM]).
     * @throws AgentStartException If the agent couldn't be started.
     * @throws java.io.IOException If the tools couldn't be served.
     */
    internal suspend fun newConversation(
        projectDirectory: File,
        permissions: PermissionHandler = PermissionHandler.Reject,
        projectContext: String? = null,
        rules: String? = null,
    ): AgentConversation {
        val prompt = listOfNotNull(systemPrompt(projectContext), rules).joinToString("\n\n").ifEmpty { null }
        return withTools(projectDirectory, permissions) { workingDirectory, mcpServers, guarded ->
            agent.newConversation(
                workingDirectory = workingDirectory,
                mcpServers = mcpServers,
                meta = { capabilities -> prompt?.let { sessionMetaOf(capabilities, it) } },
                permissions = guarded,
                firstPromptLead = { capabilities -> prompt.takeIf { !takesSystemPrompt(capabilities) } },
                prefersReadOnly = !profile.keepsOffFiles,
            )
        }
    }

    /**
     * Continues the conversation [id] about the project in [projectDirectory], which an earlier run started, handing its
     * history to [onHistory].
     *
     * @throws ConversationLostException If the agent can't continue it.
     * @throws AgentStartException If the agent couldn't be started.
     * @throws java.io.IOException If the tools couldn't be served.
     */
    internal suspend fun resumeConversation(
        projectDirectory: File,
        id: String,
        permissions: PermissionHandler = PermissionHandler.Reject,
        onHistory: suspend (SessionUpdate) -> Unit = {},
        rules: String? = null,
    ): AgentConversation = withTools(projectDirectory, permissions) { workingDirectory, mcpServers, guarded ->
        agent.resumeConversation(
            id = SessionId(id),
            workingDirectory = workingDirectory,
            mcpServers = mcpServers,
            meta = { capabilities -> listOfNotNull(systemPrompt(null), rules).joinToString("\n\n").ifEmpty { null }?.let { sessionMetaOf(capabilities, it) } },
            permissions = guarded,
            onHistory = onHistory,
            prefersReadOnly = !profile.keepsOffFiles,
        )
    }

    /**
     * Whether the agent takes the system prompt through a conversation's `_meta`: through its profile, or by advertising
     * it in [capabilities] ([AppendedSystemPrompt]).
     */
    private fun takesSystemPrompt(capabilities: AgentCapabilities) = profile.sessionMeta != null || AppendedSystemPrompt.isAdvertised(capabilities)

    private fun sessionMetaOf(capabilities: AgentCapabilities, systemPrompt: String): JsonElement? = profile.sessionMeta?.invoke(systemPrompt)
        ?: AppendedSystemPrompt.meta(systemPrompt).takeIf { AppendedSystemPrompt.isAdvertised(capabilities) }

    /**
     * Opens an address of the tools for the conversation [start] starts in the agent's working folder, allowing Creator's
     * own tools and asking [permissions] about the rest, and closes the address again if starting fails.
     */
    private suspend fun withTools(
        projectDirectory: File,
        permissions: PermissionHandler,
        start: suspend (workingDirectory: File, mcpServers: (AgentCapabilities) -> List<McpServer>, permissions: PermissionHandler) -> AgentConversation,
    ): AgentConversation {
        val port = toolServerPort()
        val tools = toolServer.openConversation()
        val conversation = try {
            start(
                if (profile.usesProjectFolder) projectDirectory else agentFolderOf(projectDirectory),
                { capabilities -> listOf(ConversationToolServer.of(capabilities, profile, "http://${McpToolServer.HOST}:$port${tools.path}", tools.token, npx)) },
                PermissionHandler.allowingCreatorTools(permissions),
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
    internal fun toolsIdOf(conversation: AgentConversation) = toolConversations[conversation]

    /**
     * Forgets what [conversation] read through its tools, so that each read is answered in full again.
     */
    internal fun forgetReads(conversation: AgentConversation) {
        toolConversations[conversation]?.let(toolServer::forgetReads)
    }

    /**
     * Ends [conversation] and turns its tools off.
     */
    internal suspend fun closeConversation(conversation: AgentConversation) {
        toolConversations.remove(conversation)?.let(toolServer::closeConversation)
        conversation.close()
    }

    private suspend fun toolServerPort() = serverStart.withLock {
        port ?: run {
            if (isClosed) throw AgentClosedException()
            try {
                withContext(ioDispatcher) { toolServer.start(port = 0, token = null) }
            } catch (exception: IOException) {
                throw if (isClosed) AgentClosedException() else exception
            }.also { port = it }
        }
    }

    /**
     * Ends the agent and stops serving the tools.
     */
    override fun close() {
        isClosed = true
        agent.close()
        toolServer.stop()
    }
}
