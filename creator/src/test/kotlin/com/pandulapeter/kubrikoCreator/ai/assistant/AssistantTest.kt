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

import com.agentclientprotocol.model.McpServer
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class AssistantTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val launcher = FakeAgentLauncher()
    private val httpClient = HttpClient(CIO) { install(SSE) }
    private val sessionMeta = buildJsonObject { put("systemPrompt", JsonPrimitive("prompt")) }
    private val assistant = Assistant(
        agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO),
        toolServer = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
        ioDispatcher = Dispatchers.IO,
        sessionMeta = sessionMeta,
    )
    private val projectDirectory = File("/projects/breakout")

    @AfterTest
    fun tearDown() {
        assistant.close()
        launcher.close()
        httpClient.close()
        scope.cancel()
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { withTimeout(10.seconds) { block() } }

    private fun toolsOf(index: Int) = launcher.agents.single().newSessions[index].mcpServers.single() as McpServer.Http

    private suspend fun toolCount(tools: McpServer.Http): Int {
        val client = Client(Implementation(name = "test", version = "1.0.0"))
        client.connect(StreamableHttpClientTransport(httpClient, tools.url) { tools.headers.forEach { header(it.name, it.value) } })
        try {
            return client.listTools().tools.size
        } finally {
            client.close()
        }
    }

    private suspend fun status(url: String, authorization: String) = httpClient.post(url) {
        header("Authorization", authorization)
        contentType(ContentType.Application.Json)
        setBody("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")
    }.status

    @Test
    fun eachConversationReachesTheToolsAtAnAddressOfItsOwn() = test {
        assistant.newConversation(projectDirectory)
        assistant.newConversation(projectDirectory)
        val parameters = launcher.agents.single().newSessions
        assertEquals(projectDirectory.absolutePath, parameters[0].cwd)
        assertEquals(sessionMeta, parameters[0]._meta)
        val first = toolsOf(0)
        val second = toolsOf(1)
        assertEquals(McpToolServer.SERVER_NAME, first.name)
        assertEquals(first.url.substringBefore("/mcp/"), second.url.substringBefore("/mcp/"))
        assertEquals(CommandRegistry.editor.tools.size, toolCount(first))
        assertEquals(CommandRegistry.editor.tools.size, toolCount(second))
        assertEquals(HttpStatusCode.Unauthorized, status(first.url, second.headers.single().value))
    }

    @Test
    fun closingAConversationTurnsItsToolsOff() = test {
        val conversation = assistant.newConversation(projectDirectory)
        val tools = toolsOf(0)
        assistant.closeConversation(conversation)
        assertEquals(HttpStatusCode.Unauthorized, status(tools.url, tools.headers.single().value))
    }

    @Test
    fun outsideClientsCantUseTheAssistantsServer() = test {
        assistant.newConversation(projectDirectory)
        assertEquals(HttpStatusCode.Unauthorized, status(toolsOf(0).url.substringBefore("/conversations/"), toolsOf(0).headers.single().value))
    }
}
