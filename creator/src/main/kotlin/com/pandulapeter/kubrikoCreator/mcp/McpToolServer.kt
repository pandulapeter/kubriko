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
import kubriko.creator.generated.resources.error_mcp_no_project
import kubriko.creator.generated.resources.mcp_tool_done
import org.jetbrains.compose.resources.getString
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.Base64

/**
 * Serves the command registry as MCP tools over Streamable HTTP on localhost, so that AI clients (Claude Code, for
 * example) can work on the open project with the same validated commands the user has. Every request needs the
 * token, as a bearer token or, for clients that can't send headers, as the [TOKEN_PARAMETER] query parameter.
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

    /**
     * Starts serving on [port] (0 picks a free one), replacing the server that ran before.
     *
     * @return The port the server listens on.
     * @throws java.io.IOException If the port can't be used.
     */
    suspend fun start(port: Int, token: String): Int {
        stop()
        // Binding first keeps a taken port from failing inside the engine's own coroutines, which only report it as uncaught.
        if (port != 0) ServerSocket(port, 1, InetAddress.getByName(HOST)).close()
        val expectedAuthorization = "Bearer $token".toByteArray()
        val expectedToken = token.toByteArray()
        val started = embeddedServer(CIO, host = HOST, port = port) {
            intercept(ApplicationCallPipeline.Plugins) {
                val authorization = call.request.headers[HttpHeaders.Authorization].orEmpty().toByteArray()
                val queryToken = call.request.queryParameters[TOKEN_PARAMETER].orEmpty().toByteArray()
                if (!MessageDigest.isEqual(authorization, expectedAuthorization) && !MessageDigest.isEqual(queryToken, expectedToken)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    finish()
                }
            }
            mcpStreamableHttp(path = PATH) { createServer() }
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
        val outcome = withContext(callDispatcher) { executor()?.call(toolName, arguments) }
        val image = (outcome as? CommandOutcome.Success)?.image
        val (result, isError) = when (outcome) {
            null -> getString(Res.string.error_mcp_no_project) to true
            is CommandOutcome.Success -> (if (outcome.result == JsonNull) getString(Res.string.mcp_tool_done) else outcome.result.toString()) to false
            is CommandOutcome.Rejected -> getString(outcome.rejection.reason, *outcome.rejection.arguments.toTypedArray()) to true
        }
        _calls.update { (it + McpToolCall(toolName, arguments.toString(), result, isError)).takeLast(LOG_SIZE) }
        return CallToolResult(
            content = listOfNotNull(TextContent(result), image?.let { ImageContent(data = Base64.getEncoder().encodeToString(it), mimeType = "image/png") }),
            isError = isError,
        )
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PATH = "/mcp"
        const val TOKEN_PARAMETER = "token"
        const val SERVER_NAME = "kubriko-creator"
        private const val LOG_SIZE = 200
        private val INSTRUCTIONS = """
            Kubriko Creator edits games made with Kubriko, a 2D game engine for Kotlin. Actor types are built from Events
            (when something happens) and Blocks (what happens then), and Kubriko Creator generates Kotlin from them.
            Scenes place Actors of those types. Start with get_project_overview and get_block_catalog. Every change shows
            up in Kubriko Creator right away and can be undone there. Changes reach the game through a build: use play to
            try them, and read_console for what the game logged.
        """.trimIndent().replace("\n", " ")
    }
}
