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

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.McpServer
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionUpdate
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.testing.blockingTest
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val launcher = FakeAgentLauncher()
    private val assistants = mutableListOf<Assistant>()
    private val httpClient = HttpClient(CIO) { install(SSE) }
    private val profile = AgentProfile(sessionMeta = { JsonPrimitive(it) }, alwaysUsesHttpTools = true, keepsOffFiles = true, usesProjectFolder = true, usesAnthropicApiKey = true)
    private val agentFolders = createTempDirectory().toFile()
    private val assistant = assistant(profile)

    private fun assistant(
        profile: AgentProfile,
        launcher: FakeAgentLauncher = this.launcher,
        beforeToolServerStart: suspend () -> Unit = {},
    ) = Assistant(
        agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO),
        toolServer = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0", beforeEngineStart = beforeToolServerStart),
        ioDispatcher = Dispatchers.IO,
        profile = profile,
        systemPrompt = { projectContext -> projectContext ?: SYSTEM_PROMPT },
        npx = NPX,
        agentFolders = agentFolders,
    ).also { assistants += it }
    private val projectDirectory = File("/projects/breakout")

    @AfterTest
    fun tearDown() {
        assistants.forEach { it.close() }
        agentFolders.deleteRecursively()
        launcher.close()
        httpClient.close()
        scope.cancel()
    }

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
    fun eachConversationReachesTheToolsAtAnAddressOfItsOwn() = blockingTest {
        assistant.newConversation(projectDirectory)
        assistant.newConversation(projectDirectory)
        val parameters = launcher.agents.single().newSessions
        assertEquals(projectDirectory.absolutePath, parameters[0].cwd)
        assertEquals(JsonPrimitive(SYSTEM_PROMPT), parameters[0]._meta)
        val first = toolsOf(0)
        val second = toolsOf(1)
        assertEquals(McpToolServer.SERVER_NAME, first.name)
        assertEquals(first.url.substringBefore("/mcp/"), second.url.substringBefore("/mcp/"))
        assertEquals(CommandRegistry.editor.tools.size, toolCount(first))
        assertEquals(CommandRegistry.editor.tools.size, toolCount(second))
        assertEquals(HttpStatusCode.Unauthorized, status(first.url, second.headers.single().value))
    }

    @Test
    fun closingWhileTheToolServerStartsFailsTheConversationAsClosed() = blockingTest {
        val isStarting = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val assistant = assistant(
            profile,
            beforeToolServerStart = {
                isStarting.complete(Unit)
                gate.await()
            },
        )
        val starting = async { runCatching { assistant.newConversation(projectDirectory) } }
        isStarting.await()
        assistant.close()
        gate.complete(Unit)
        assertIs<AgentClosedException>(starting.await().exceptionOrNull())
    }

    @Test
    fun aConversationIsStartedWithWhatItIsToldAboutTheProject() = blockingTest {
        assistant.newConversation(projectDirectory, projectContext = "Project breakout.")
        assertEquals(JsonPrimitive("Project breakout."), launcher.agents.single().newSessions.single()._meta)
    }

    @Test
    fun closingAConversationTurnsItsToolsOff() = blockingTest {
        val conversation = assistant.newConversation(projectDirectory)
        val tools = toolsOf(0)
        assistant.closeConversation(conversation)
        assertEquals(HttpStatusCode.Unauthorized, status(tools.url, tools.headers.single().value))
    }

    @Test
    fun outsideClientsCantUseTheAssistantsServer() = blockingTest {
        assistant.newConversation(projectDirectory)
        assertEquals(HttpStatusCode.Unauthorized, status(toolsOf(0).url.substringBefore("/conversations/"), toolsOf(0).headers.single().value))
    }

    @Test
    fun anAgentWithoutAProfileGetsTheSystemPromptAheadOfItsFirstPromptOnly() = blockingTest {
        val conversation = assistant(AgentProfile.fallback).newConversation(projectDirectory)
        conversation.prompt(listOf(ContentBlock.Text("hello")))
        conversation.prompt(listOf(ContentBlock.Text("hello")))
        val agent = launcher.agents.single()
        assertNull(agent.newSessions.single()._meta)
        assertEquals(listOf(SYSTEM_PROMPT), agent.leadingTexts)
    }

    @Test
    fun anAgentAdvertisingAnAppendedSystemPromptGetsItWithItsFileToolsOffThroughEveryConversationsMeta() = blockingTest {
        val advertising = buildJsonObject { putJsonObject("antigravity") { putJsonObject("systemPrompt") { put("append", true) } } }
        val launcher = FakeAgentLauncher(fakeAgent = { FakeAgent(capabilitiesMeta = advertising) })
        val assistant = assistant(AgentProfile.fallback, launcher)
        val conversation = assistant.newConversation(projectDirectory, rules = "Ask first.")
        conversation.prompt(listOf(ContentBlock.Text("hello")))
        assistant.resumeConversation(projectDirectory, "earlier")
        val agent = launcher.agents.single()
        assertEquals(antigravityMeta("$SYSTEM_PROMPT\n\nAsk first."), agent.newSessions.single()._meta)
        assertEquals(antigravityMeta(SYSTEM_PROMPT), agent.loadedSessionParameters.single()._meta)
        assertEquals(emptyList(), agent.leadingTexts)
        launcher.close()
    }

    @Test
    fun anAntigravityAgentNotAdvertisingAppendingHearsTheSystemPromptFirstWithItsFileToolsStillOff() = blockingTest {
        val extension = buildJsonObject { putJsonObject("antigravity") { put("tier", true) } }
        val launcher = FakeAgentLauncher(fakeAgent = { FakeAgent(capabilitiesMeta = extension) })
        val conversation = assistant(AgentProfile.fallback, launcher).newConversation(projectDirectory)
        conversation.prompt(listOf(ContentBlock.Text("hello")))
        val agent = launcher.agents.single()
        assertEquals(antigravityMeta(null), agent.newSessions.single()._meta)
        assertEquals(listOf(SYSTEM_PROMPT), agent.leadingTexts)
        launcher.close()
    }

    private fun antigravityMeta(appendedSystemPrompt: String?) = buildJsonObject {
        putJsonObject("antigravity") {
            appendedSystemPrompt?.let { putJsonObject("systemPrompt") { put("append", it) } }
            putJsonArray("disabledTools") { listOf("edit_file", "create_file", "run_command").forEach { add(it) } }
        }
    }

    @Test
    fun anAgentWithoutHttpMcpReachesTheToolsThroughMcpRemote() = blockingTest {
        assistant(AgentProfile.fallback).newConversation(projectDirectory)
        val tools = launcher.agents.single().newSessions.single().mcpServers.single() as McpServer.Stdio
        assertEquals(McpToolServer.SERVER_NAME, tools.name)
        assertEquals(NPX.path, tools.command)
        assertTrue(tools.args.any { it.startsWith("http://") && "/conversations/" in it })
        assertTrue(tools.env.single().value.startsWith("Bearer "))
        assertFalse(tools.args.any { it.startsWith("Bearer ") })
    }

    @Test
    fun anAgentAdvertisingHttpMcpGetsTheToolsOverHttp() = blockingTest {
        val httpLauncher = FakeAgentLauncher({ FakeAgent(supportsHttpMcp = true) })
        try {
            assistant(AgentProfile.fallback, httpLauncher).newConversation(projectDirectory)
            assertIs<McpServer.Http>(httpLauncher.agents.single().newSessions.single().mcpServers.single())
        } finally {
            httpLauncher.close()
        }
    }

    @Test
    fun anAgentWithoutAProfileAsksTheUserBeforeChangingFilesItself() = blockingTest {
        val asked = mutableListOf<PermissionRequest>()
        val allow = PermissionHandler { toolCall, options ->
            asked += PermissionRequest(toolCall, options)
            RequestPermissionOutcome.Selected(options.first { it.kind == PermissionOptionKind.ALLOW_ONCE }.optionId)
        }
        val conversation = assistant(AgentProfile.fallback).newConversation(projectDirectory, permissions = allow)
        assertEquals("allow", conversation.reply("edit"))
        assertTrue(asked.single().isOutsideCreator)
        assertEquals("reject", assistant(AgentProfile.fallback).newConversation(projectDirectory).reply("edit"))
    }

    @Test
    fun anAgentWithoutAProfileWorksInAFolderOfItsOwnRatherThanTheProjects() = blockingTest {
        assistant(AgentProfile.fallback).newConversation(projectDirectory)
        val folder = File(launcher.agents.single().newSessions.single().cwd)
        assertTrue(folder.isDirectory)
        assertEquals(agentFolders.canonicalFile, folder.parentFile.canonicalFile)
        assertTrue(folder.name.startsWith("breakout-"))
    }

    private suspend fun AgentConversation.reply(text: String) = buildString {
        prompt(listOf(ContentBlock.Text(text))) { update -> ((update as? SessionUpdate.AgentMessageChunk)?.content as? ContentBlock.Text)?.let { append(it.text) } }
    }

    private companion object {
        const val SYSTEM_PROMPT = "prompt"
        val NPX = File("/usr/local/bin/npx")
    }
}
