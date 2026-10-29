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

import com.agentclientprotocol.model.HttpHeader
import com.agentclientprotocol.model.McpServer
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The in-app assistant: an [agent] whose conversations each reach Kubriko Creator's tools at an address of their own on
 * [toolServer], which the assistant starts on a free port the first time it is needed, whatever the AI connection panel's
 * switch says. Closing a conversation turns its tools off.
 *
 * @param toolServer A server of its own, not the one outside clients connect to.
 * @param sessionMeta The `_meta` a conversation is started with, through which an agent takes its own options.
 */
internal class Assistant(
    private val agent: AcpAgent,
    private val toolServer: McpToolServer,
    private val ioDispatcher: CoroutineDispatcher,
    private val sessionMeta: JsonElement? = null,
) : AutoCloseable {
    private val serverStart = Mutex()
    private var port: Int? = null
    private val toolConversations = ConcurrentHashMap<AgentConversation, String>()

    /**
     * Starts a conversation about the project in [projectDirectory].
     *
     * @throws AgentStartException If the agent couldn't be started.
     * @throws java.io.IOException If the tools couldn't be served.
     */
    suspend fun newConversation(projectDirectory: File, permissions: PermissionHandler = PermissionHandler.Reject): AgentConversation {
        val port = toolServerPort()
        val tools = toolServer.openConversation()
        val conversation = try {
            agent.newConversation(
                workingDirectory = projectDirectory,
                mcpServers = listOf(
                    McpServer.Http(
                        name = McpToolServer.SERVER_NAME,
                        url = "http://${McpToolServer.HOST}:$port${tools.path}",
                        headers = listOf(HttpHeader("Authorization", "Bearer ${tools.token}")),
                    ),
                ),
                meta = sessionMeta,
                permissions = permissions,
            )
        } catch (exception: Exception) {
            toolServer.closeConversation(tools.id)
            throw exception
        }
        toolConversations[conversation] = tools.id
        return conversation
    }

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
