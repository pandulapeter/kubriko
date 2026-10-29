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

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_mcp_internal
import kubriko.creator.generated.resources.error_mcp_no_project
import kubriko.creator.generated.resources.mcp_tool_done
import org.jetbrains.compose.resources.getString
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Serves the command registry as MCP tools over Streamable HTTP on localhost, so that AI clients (Claude Code, for
 * example) can work on the open project with the same validated commands the user has. Every request needs the
 * token, as a bearer token or, for clients that can't send headers, as the [TOKEN_PARAMETER] query parameter.
 *
 * The assistant's conversations each reach the tools at an address of their own ([openConversation]), with a token of
 * their own that stops working once the conversation is closed.
 *
 * @param executor The command executor of the open project, or null if none is open.
 * @param callDispatcher Where tool calls run: the main thread, which the command history is confined to.
 */
internal class McpToolServer(
    private val executor: () -> CommandExecutor?,
    private val callDispatcher: CoroutineDispatcher,
    private val version: String,
    private val registry: CommandRegistry = CommandRegistry.editor,
) {
    private val _calls = MutableStateFlow(emptyList<McpToolCall>())

    /**
     * The tool calls made since Kubriko Creator started, oldest first.
     */
    val calls = _calls.asStateFlow()
    private var server: EmbeddedServer<*, *>? = null
    private val conversationTokens = ConcurrentHashMap<String, ByteArray>()

    /**
     * An address of the tools for one conversation of the assistant.
     *
     * @param path Where the conversation reaches the tools, on the server's port.
     */
    data class Conversation(
        val id: String,
        val path: String,
        val token: String,
    )

    /**
     * Starts serving on [port] (0 picks a free one), replacing the server that ran before.
     *
     * @param token What outside clients need at [PATH], or null if only the assistant's conversations are served.
     * @return The port the server listens on.
     * @throws java.io.IOException If the port can't be used.
     */
    suspend fun start(port: Int, token: String?): Int {
        stop()
        // Binding first keeps a taken port from failing inside the engine's own coroutines, which only report it as uncaught.
        if (port != 0) ServerSocket(port, 1, InetAddress.getByName(HOST)).close()
        val expectedToken = token?.toByteArray()
        val started = embeddedServer(CIO, host = HOST, port = port) {
            intercept(ApplicationCallPipeline.Plugins) {
                val path = call.request.path()
                val expected = if (path.startsWith("$CONVERSATIONS_PATH/")) {
                    conversationTokens[path.removePrefix("$CONVERSATIONS_PATH/").substringBefore('/')]
                } else {
                    expectedToken
                }
                if (expected == null || !isAuthorized(call.request.headers[HttpHeaders.Authorization], call.request.queryParameters[TOKEN_PARAMETER], expected)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    finish()
                }
            }
            if (token != null) mcpStreamableHttp(path = PATH) { createServer() }
            mcpStreamableHttp(path = "$CONVERSATIONS_PATH/{$CONVERSATION_PARAMETER}") { createServer() }
        }
        server = started
        try {
            started.startSuspend(wait = false)
        } catch (exception: CancellationException) {
            // The CIO engine reports a port it can't bind as a cancellation caused by the BindException.
            currentCoroutineContext().ensureActive()
            stop()
            throw generateSequence<Throwable>(exception) { it.cause }.filterIsInstance<IOException>().firstOrNull() ?: exception
        }
        return started.engine.resolvedConnectors().first().port
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
        server = null
    }

    /**
     * Opens an address of the tools for one conversation of the assistant, valid until [closeConversation].
     */
    fun openConversation(): Conversation {
        val id = UUID.randomUUID().toString()
        val token = randomToken()
        conversationTokens[id] = token.toByteArray()
        return Conversation(id = id, path = "$CONVERSATIONS_PATH/$id", token = token)
    }

    /**
     * Turns the tools of the conversation with [id] off.
     */
    fun closeConversation(id: String) {
        conversationTokens.remove(id)
    }

    private fun isAuthorized(authorization: String?, queryToken: String?, expectedToken: ByteArray) =
        MessageDigest.isEqual(authorization.orEmpty().toByteArray(), "Bearer ".toByteArray() + expectedToken) ||
            MessageDigest.isEqual(queryToken.orEmpty().toByteArray(), expectedToken)

    private fun createServer() = Server(
        serverInfo = Implementation(name = SERVER_NAME, version = version),
        options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        instructions = INSTRUCTIONS,
    ) {
        registry.tools.forEach { tool ->
            addTool(
                name = tool.name,
                description = tool.description,
                inputSchema = ToolSchema(
                    properties = tool.inputSchema["properties"]?.jsonObject,
                    required = tool.inputSchema["required"]?.jsonArray?.map { it.jsonPrimitive.content },
                ),
                toolAnnotations = ToolAnnotations(readOnlyHint = tool.isReadOnly, destructiveHint = tool.isDestructive, openWorldHint = false),
            ) { request ->
                runTool(tool.name, request.arguments ?: JsonObject(emptyMap()))
            }
        }
    }

    private suspend fun runTool(toolName: String, arguments: JsonObject): CallToolResult {
        val outcome = try {
            withContext(callDispatcher) { executor()?.call(toolName, arguments) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            // Left to the SDK, the client would only see the message, which for a missing class is just its name.
            return result(toolName, arguments, getString(Res.string.error_mcp_internal, throwable.toString()), isError = true)
        }
        val (result, isError) = when (outcome) {
            null -> getString(Res.string.error_mcp_no_project) to true
            is CommandOutcome.Success -> (if (outcome.result == JsonNull) getString(Res.string.mcp_tool_done) else outcome.result.toString()) to false
            is CommandOutcome.Rejected -> getString(outcome.rejection.reason, *outcome.rejection.arguments.toTypedArray()) to true
        }
        return result(toolName, arguments, result, isError, image = (outcome as? CommandOutcome.Success)?.image)
    }

    private fun result(toolName: String, arguments: JsonObject, text: String, isError: Boolean, image: ByteArray? = null): CallToolResult {
        _calls.update { (it + McpToolCall(toolName, arguments.toString(), text, isError)).takeLast(LOG_SIZE) }
        return CallToolResult(
            content = listOfNotNull(TextContent(text), image?.let { ImageContent(data = Base64.getEncoder().encodeToString(it), mimeType = "image/png") }),
            isError = isError,
        )
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PATH = "/mcp"
        const val CONVERSATIONS_PATH = "$PATH/conversations"
        private const val CONVERSATION_PARAMETER = "conversation"
        const val TOKEN_PARAMETER = "token"
        const val SERVER_NAME = "kubriko-creator"
        private const val LOG_SIZE = 200
        val INSTRUCTIONS = """
            Kubriko Creator edits games made with Kubriko, a 2D game engine for Kotlin. Actor types are built from Events
            (when something happens) and Blocks (what happens then), and Kubriko Creator generates Kotlin from them.
            Scenes place Actors of those types. Start with get_project_overview and get_block_catalog. Every change shows
            up in Kubriko Creator right away and can be undone there. Changes reach the game through a build: try them with
            start_game, press_keys, take_screenshot, and stop_game, and use read_console for what the game logged. Make
            every change through these tools, never by editing the project's files in other ways: Kubriko Creator owns the
            generated code, and the user can't see or undo such edits in it. When a request needs something no Event or
            Block offers, a Kotlin Block can run a short snippet inside an Event; when even that isn't enough, do what the
            tools allow and tell the user what is missing, rather than writing Kotlin files.
        """.trimIndent().replace("\n", " ")
    }
}
