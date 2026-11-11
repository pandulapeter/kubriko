/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.context.ResultDelta
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class McpToolServerTest {
    private val token = "secret-token"
    private val httpClient = HttpClient(CIO) { install(SSE) }
    private var server: McpToolServer? = null

    @AfterTest
    fun tearDown() {
        server?.stop()
        httpClient.close()
    }

    private suspend fun TestScope.start(executor: CommandExecutor?): Int = start { executor }

    private suspend fun TestScope.start(
        token: String? = this@McpToolServerTest.token,
        onToolCalled: (String, JsonObject, Boolean) -> Unit = { _, _, _ -> },
        executor: () -> CommandExecutor?,
    ): Int {
        val started = McpToolServer(executor = executor, callDispatcher = Dispatchers.Default, version = "1.0.0", onToolCalled = onToolCalled)
        server = started
        return withContext(Dispatchers.IO) { started.start(port = 0, token = token) }
    }

    private suspend fun <T> connected(port: Int, path: String = "/mcp", token: String = this.token, block: suspend Client.() -> T): T = withContext(Dispatchers.IO) {
        val client = Client(Implementation(name = "test", version = "1.0.0"))
        client.connect(StreamableHttpClientTransport(httpClient, "http://127.0.0.1:$port$path") { header(HttpHeaders.Authorization, "Bearer $token") })
        try {
            client.block()
        } finally {
            client.close()
        }
    }

    @Test
    fun aServerStoppedWhileStartingStopsListening() = runBlocking(Dispatchers.IO) {
        withTimeout(10.seconds) {
            val port = ServerSocket(0).use { it.localPort }
            val isStarting = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()
            val started = McpToolServer(
                executor = { null },
                callDispatcher = Dispatchers.Default,
                version = "1.0.0",
                beforeEngineStart = {
                    isStarting.complete(Unit)
                    gate.await()
                },
            )
            server = started
            val starting = async { runCatching { started.start(port, token) } }
            isStarting.await()
            started.stop()
            gate.complete(Unit)
            assertIs<IOException>(starting.await().exceptionOrNull())
            ServerSocket(port, 1, InetAddress.getByName(McpToolServer.HOST)).close()
        }
    }

    @Test
    fun everyCommandIsListedAsAToolWithItsSchemaAndHints() = runTest {
        val port = start(executor = null)
        val tools = connected(port) { listTools().tools }
        assertEquals(CommandRegistry.editor.tools.map { it.name }, tools.map { it.name })
        val discard = tools.single { it.name == "discard_changes" }
        assertEquals(true, discard.annotations?.destructiveHint)
        assertEquals(false, discard.annotations?.readOnlyHint)
        assertEquals(true, tools.single { it.name == "read_scene" }.annotations?.readOnlyHint)
        assertEquals(JsonPrimitive(true), tools.single { it.name == "add_block" }.meta?.get("anthropic/alwaysLoad"))
        assertNull(tools.single { it.name == "delete_actor_type" }.meta?.get("anthropic/alwaysLoad"))
        assertEquals(listOf("paths"), discard.inputSchema.required)
        assertTrue("paths" in discard.inputSchema.properties.orEmpty())
    }

    @Test
    fun clientsAreAskedToChangeTheProjectOnlyThroughTheTools() = runTest {
        val port = start(executor = null)
        val instructions = connected(port) { serverInstructions }.orEmpty()
        assertContains(instructions, "Make every change through these tools")
        assertContains(instructions, "tell the user what is missing")
    }

    @Test
    fun toolCallsChangeTheOpenProjectAndCanBeUndone() = runTest {
        val context = commandContext()
        val executor = CommandExecutor(context, testScope())
        val port = start(executor)
        val result = connected(port) { callTool("create_actor_type", mapOf("name" to "Ball", "shape" to "rectangle")) }
        assertEquals(false, result.isError)
        assertEquals("""{"revision":1,"changed":"create_actor_type at Ball"}""", (result.content.single() as TextContent).text)
        assertEquals(listOf("Ball"), context.blockTypes!!.actorTypes.value.map { it.name })
        executor.step(isUndo = true)
        assertTrue(context.blockTypes!!.actorTypes.value.isEmpty())
    }

    @Test
    fun readToolsReturnJson() = runTest {
        val port = start(CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope()))
        val result = connected(port) { callTool("get_project_overview", emptyMap()) }
        val overview = Json.parseToJsonElement((result.content.single() as TextContent).text).jsonObject
        assertEquals(Json.parseToJsonElement("""["Ball"]"""), overview["actorTypes"])
    }

    @Test
    fun eachStepNamesTheClientThatMadeIt() = runTest {
        val executor = CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope())
        val started = start(executor)
        connected(started) { callTool("list_scenes", emptyMap()) }
        assertEquals("test", executor.aiSteps.value.single().clientName)
    }

    @Test
    fun rejectionsAreErrorsInPlainWordsAndEveryCallIsLogged() = runTest {
        val started = start(CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope()))
        val result = connected(started) { callTool("create_actor_type", mapOf("name" to "Ball")) }
        assertEquals(true, result.isError)
        assertEquals("There's already an Actor type or Manager type with that name.", (result.content.single() as TextContent).text)
        val logged = server!!.calls.value.single()
        assertEquals("create_actor_type", logged.toolName)
        assertEquals("""{"name":"Ball"}""", logged.arguments)
        assertTrue(logged.isError)
    }

    @Test
    fun everyCallIsHandedOverWithItsWholeInput() = runTest {
        val handedOver = mutableListOf<Triple<String, JsonObject, Boolean>>()
        val executor = CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope())
        val started = start(onToolCalled = { name, arguments, isError -> synchronized(handedOver) { handedOver += Triple(name, arguments, isError) } }) { executor }
        val description = "d".repeat(McpToolCall.PREVIEW_LENGTH * 2)
        connected(started) {
            callTool("create_actor_type", mapOf("name" to "Ball"))
            callTool("create_actor_type", mapOf("name" to "Paddle", "description" to description))
        }
        assertEquals(
            listOf(
                Triple("create_actor_type", buildJsonObject { put("name", "Ball") }, true),
                Triple("create_actor_type", buildJsonObject { put("name", "Paddle"); put("description", description) }, false),
            ),
            synchronized(handedOver) { handedOver.toList() },
        )
    }

    @Test
    fun aCallIsAnsweredEvenWhenWhatItIsHandedToThrows() = runTest {
        val executor = CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope())
        val started = start(onToolCalled = { _, _, _ -> error("The disk is full.") }) { executor }
        val result = connected(started) { callTool("list_scenes", emptyMap()) }
        assertEquals(false, result.isError)
    }

    @Test
    fun rejectionsReadAsTheirEnglishWithTheirArguments() = runTest {
        val started = start(CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope()))
        val result = connected(started) { callTool("read_actor_type", mapOf("actorType" to "Rock")) }
        assertEquals(true, result.isError)
        assertEquals("There's no Actor type called Rock.", (result.content.single() as TextContent).text)
    }

    @Test
    fun internalErrorsSayWhatWentWrongAndAreLogged() = runTest {
        val port = start { throw NoClassDefFoundError("com/example/Missing") }
        val result = connected(port) { callTool("list_scenes", emptyMap()) }
        assertEquals(true, result.isError)
        assertEquals(
            "Kubriko Creator ran into an internal error, so nothing was done: java.lang.NoClassDefFoundError: com/example/Missing. This is a problem in Kubriko Creator, not in the request.",
            (result.content.single() as TextContent).text,
        )
        assertTrue(server!!.calls.value.single().isError)
    }

    @Test
    fun theLogKeepsOnlyTheStartOfLongResults() {
        val call = McpToolCall.of("read_scene", "{}", "x".repeat(10_000), isError = false)
        assertEquals("x".repeat(McpToolCall.PREVIEW_LENGTH) + "…", call.result)
    }

    @Test
    fun withoutAnOpenProjectToolsSaySo() = runTest {
        val port = start(executor = null)
        val result = connected(port) { callTool("list_scenes", emptyMap()) }
        assertEquals(true, result.isError)
        assertEquals("No project is open in Kubriko Creator.", (result.content.single() as TextContent).text)
    }

    @Test
    fun requestsWithoutTheTokenAreRefused() = runTest {
        val port = start(executor = null)
        val statuses = withContext(Dispatchers.IO) {
            listOf(null, "Bearer wrong", "Bearer $token-and-more").map { authorization ->
                httpClient.post("http://127.0.0.1:$port/mcp") {
                    authorization?.let { header(HttpHeaders.Authorization, it) }
                    contentType(ContentType.Application.Json)
                    setBody("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")
                }.status
            }
        }
        assertEquals(List(3) { HttpStatusCode.Unauthorized }, statuses)
    }

    @Test
    fun clientsThatCantSendHeadersCanPassTheTokenInTheUrl() = runTest {
        val port = start(executor = null)
        val tools = withContext(Dispatchers.IO) {
            val client = Client(Implementation(name = "test", version = "1.0.0"))
            client.connect(StreamableHttpClientTransport(httpClient, "http://127.0.0.1:$port/mcp?token=$token"))
            try {
                client.listTools().tools
            } finally {
                client.close()
            }
        }
        assertEquals(CommandRegistry.editor.tools.size, tools.size)
    }

    @Test
    fun screenshotsComeBackAsImages() = runTest {
        val runner = FakeProjectRunner(step = GameStep.Done(console = emptyList(), png = byteArrayOf(1, 2, 3)))
        val port = start(CommandExecutor(commandContext(runner = runner), testScope()))
        val result = connected(port) { callTool("take_screenshot", emptyMap()) }
        val image = result.content.filterIsInstance<ImageContent>().single()
        assertEquals("image/png", image.mimeType)
        assertEquals(listOf<Byte>(1, 2, 3), Base64.getDecoder().decode(image.data).toList())
    }

    @Test
    fun stoppingFreesThePort() = runTest {
        val port = start(executor = null)
        server!!.stop()
        assertNull(runCatching { connected(port) { listTools() } }.getOrNull())
        assertEquals(port, withContext(Dispatchers.IO) { server!!.start(port = port, token = token) })
    }

    private suspend fun status(port: Int, path: String, token: String) = withContext(Dispatchers.IO) {
        httpClient.post("http://127.0.0.1:$port$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")
        }.status
    }

    @Test
    fun conversationsReachTheToolsOnlyWithTheirOwnTokenUntilClosed() = runTest {
        val port = start(executor = null)
        val first = server!!.openConversation()
        val second = server!!.openConversation()
        assertEquals("/mcp/conversations/${first.id}", first.path)
        assertEquals(CommandRegistry.editor.tools.size, connected(port, first.path, first.token) { listTools().tools.size })
        assertEquals(HttpStatusCode.Unauthorized, status(port, first.path, second.token))
        assertEquals(HttpStatusCode.Unauthorized, status(port, first.path, token))
        assertEquals(HttpStatusCode.Unauthorized, status(port, "/mcp", first.token))
        server!!.closeConversation(first.id)
        assertEquals(HttpStatusCode.Unauthorized, status(port, first.path, first.token))
        assertEquals(CommandRegistry.editor.tools.size, connected(port, second.path, second.token) { listTools().tools.size })
    }

    @Test
    fun withoutATokenOnlyConversationsAreServed() = runTest {
        val port = start(token = null, executor = { null })
        val conversation = server!!.openConversation()
        assertEquals(HttpStatusCode.Unauthorized, status(port, "/mcp", ""))
        val result = connected(port, conversation.path, conversation.token) { callTool("list_scenes", emptyMap()) }
        assertEquals("No project is open in Kubriko Creator.", (result.content.single() as TextContent).text)
    }

    @Test
    fun conversationsCallsAreStepsThatNameTheConversationAndTheClientsOwnId() = runTest {
        val executor = CommandExecutor(commandContext(), testScope())
        val port = start(executor)
        val conversation = server!!.openConversation()
        connected(port, conversation.path, conversation.token) {
            callTool("list_scenes", emptyMap(), meta = mapOf("claudecode/toolUseId" to "toolu_1"))
            callTool("list_scenes", emptyMap())
        }
        connected(port) { callTool("list_scenes", emptyMap()) }
        assertEquals(
            listOf(AiStepOrigin(conversation.id, "toolu_1"), AiStepOrigin(conversation.id), null),
            executor.aiSteps.value.map { it.origin },
        )
    }

    @Test
    fun aConversationRereadingSomethingGetsOnlyWhatChangedUnlessItAsksForAll() = runTest {
        val events = (1..6).map { EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("A line long enough to make the whole type worth not sending again: $it"))) }
        val context = commandContext(ActorTypeDefinition("Ball", events = events))
        val executor = CommandExecutor(context, testScope())
        val port = start(executor)
        val conversation = server!!.openConversation()
        val read = mapOf("actorType" to "Ball")
        connected(port, conversation.path, conversation.token) {
            assertContains(text(callTool("read_actor_type", read)), "appearance")
            assertEquals(ResultDelta.UNCHANGED, text(callTool("read_actor_type", read)))
            callTool("add_event", mapOf("actorType" to "Ball", "type" to "keyPressed"))
            assertEquals(
                """{"sinceYourLastRead":"Only what changed since you last read it in this conversation. If that result isn't in front of you (a subagent read it, or it was summarized), read again with full: true.","changed":{"events[6]":"6 When Spacebar is pressed"}}""",
                text(callTool("read_actor_type", read)),
            )
            assertContains(text(callTool("read_actor_type", read + ("full" to true))), "appearance")
            assertTrue(listTools().tools.single { it.name == "read_actor_type" }.inputSchema.properties.orEmpty().containsKey("full"))
            assertFalse(listTools().tools.single { it.name == "add_event" }.inputSchema.properties.orEmpty().containsKey("full"))
        }
        connected(port) { assertContains(text(callTool("read_actor_type", read)), "appearance") }
    }

    @Test
    fun forgettingAConversationsReadsAnswersInFullAgainAndKeepsItsTools() = runTest {
        val events = (1..6).map { EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("A line long enough to make the whole type worth not sending again: $it"))) }
        val executor = CommandExecutor(commandContext(ActorTypeDefinition("Ball", events = events)), testScope())
        val port = start(executor)
        val conversation = server!!.openConversation()
        val read = mapOf("actorType" to "Ball")
        connected(port, conversation.path, conversation.token) {
            assertContains(text(callTool("read_actor_type", read)), "appearance")
            assertEquals(ResultDelta.UNCHANGED, text(callTool("read_actor_type", read)))
        }
        server!!.forgetReads(conversation.id)
        connected(port, conversation.path, conversation.token) {
            assertContains(text(callTool("read_actor_type", read)), "appearance")
        }
    }

    private fun text(result: CallToolResult) = (result.content.first() as TextContent).text
}
