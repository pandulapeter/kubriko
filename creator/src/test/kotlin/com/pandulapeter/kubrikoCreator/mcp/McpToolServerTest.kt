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
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
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
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    private suspend fun TestScope.start(executor: () -> CommandExecutor?): Int {
        val started = McpToolServer(executor = executor, callDispatcher = Dispatchers.Default, version = "1.0.0")
        server = started
        return withContext(Dispatchers.IO) { started.start(port = 0, token = token) }
    }

    private suspend fun <T> connected(port: Int, block: suspend Client.() -> T): T = withContext(Dispatchers.IO) {
        val client = Client(Implementation(name = "test", version = "1.0.0"))
        client.connect(StreamableHttpClientTransport(httpClient, "http://127.0.0.1:$port/mcp") { header(HttpHeaders.Authorization, "Bearer $token") })
        try {
            client.block()
        } finally {
            client.close()
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
        assertEquals("Done.", (result.content.single() as TextContent).text)
        assertEquals(listOf("Ball"), context.actorTypes!!.actorTypes.value.map { it.name })
        executor.step(isUndo = true)
        assertTrue(context.actorTypes.actorTypes.value.isEmpty())
    }

    @Test
    fun readToolsReturnJson() = runTest {
        val port = start(CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope()))
        val result = connected(port) { callTool("get_project_overview", emptyMap()) }
        val overview = Json.parseToJsonElement((result.content.single() as TextContent).text).jsonObject
        assertEquals(Json.parseToJsonElement("""["Ball"]"""), overview["actorTypes"])
    }

    @Test
    fun rejectionsAreErrorsInPlainWordsAndEveryCallIsLogged() = runTest {
        val started = start(CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope()))
        val result = connected(started) { callTool("create_actor_type", mapOf("name" to "Ball")) }
        assertEquals(true, result.isError)
        assertEquals("There's already an Actor type with that name.", (result.content.single() as TextContent).text)
        val logged = server!!.calls.value.single()
        assertEquals("create_actor_type", logged.toolName)
        assertEquals("""{"name":"Ball"}""", logged.arguments)
        assertTrue(logged.isError)
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
}
