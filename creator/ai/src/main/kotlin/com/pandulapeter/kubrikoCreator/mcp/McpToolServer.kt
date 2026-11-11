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

import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.foundation.englishWith
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
 * @param beforeEngineStart Runs right before the engine starts listening; tests use it to stop the server meanwhile.
 * @param onToolCalled Called after every tool call with its whole input, which [calls] keeps only the start of; the eval
 *   set's live run records real sessions' calls with it. What it throws is ignored, since the call has been made either way.
 */
public class McpToolServer(
    private val executor: () -> CommandExecutor?,
    private val callDispatcher: CoroutineDispatcher,
    private val version: String,
    private val registry: CommandRegistry = CommandRegistry.editor,
    private val beforeEngineStart: suspend () -> Unit = {},
    private val onToolCalled: (toolName: String, arguments: JsonObject, isError: Boolean) -> Unit = { _, _, _ -> },
) {
    private val _calls = MutableStateFlow(emptyList<McpToolCall>())

    /**
     * The tool calls made since Kubriko Creator started, oldest first.
     */
    internal val calls = _calls.asStateFlow()
    private val lifecycle = Any()
    private var generation = 0
    private var server: EmbeddedServer<*, *>? = null
    private val conversationTokens = ConcurrentHashMap<String, ByteArray>()
    private val conversationReads = ConversationReads()

    /**
     * An address of the tools for one conversation of the assistant.
     *
     * @param path Where the conversation reaches the tools, on the server's port.
     */
    internal data class Conversation(
        val id: String,
        val path: String,
        val token: String,
    )

    /**
     * Starts serving on [port] (0 picks a free one), replacing the server that ran before.
     *
     * @param token What outside clients need at [PATH], or null if only the assistant's conversations are served.
     * @return The port the server listens on.
     * @throws java.io.IOException If the port can't be used, or [stop] was called while the server started.
     */
    internal suspend fun start(port: Int, token: String?): Int {
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
            mcpStreamableHttp(path = "$CONVERSATIONS_PATH/{$CONVERSATION_PARAMETER}") { createServer(conversationId = call.parameters[CONVERSATION_PARAMETER]) }
        }
        val own = synchronized(lifecycle) {
            server = started
            ++generation
        }
        try {
            beforeEngineStart()
            started.startSuspend(wait = false)
        } catch (exception: CancellationException) {
            // The CIO engine reports a port it can't bind, and starting after a stop(), as a cancellation.
            val isStopped = isStoppedSince(own)
            stop()
            currentCoroutineContext().ensureActive()
            if (isStopped) throw stoppedWhileStarting()
            throw generateSequence<Throwable>(exception) { it.cause }.filterIsInstance<IOException>().firstOrNull() ?: exception
        }
        if (isStoppedSince(own)) {
            started.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
            throw stoppedWhileStarting()
        }
        return started.engine.resolvedConnectors().first().port
    }

    private fun isStoppedSince(generation: Int) = synchronized(lifecycle) { this.generation != generation }

    private fun stoppedWhileStarting() = IOException("The server was stopped while it started.")

    /**
     * Stops serving, from any thread; a [start] running meanwhile then stops the server it starts as well.
     */
    internal fun stop() {
        val stopped = synchronized(lifecycle) {
            generation++
            server.also { server = null }
        }
        stopped?.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
    }

    /**
     * Opens an address of the tools for one conversation of the assistant, valid until [closeConversation].
     */
    internal fun openConversation(): Conversation {
        val id = UUID.randomUUID().toString()
        val token = randomToken()
        conversationTokens[id] = token.toByteArray()
        return Conversation(id = id, path = "$CONVERSATIONS_PATH/$id", token = token)
    }

    /**
     * Turns the tools of the conversation with [id] off.
     */
    internal fun closeConversation(id: String) {
        conversationTokens.remove(id)
        conversationReads.forget(id)
    }

    /**
     * Forgets what the conversation with [id] read, so that it is answered in full again; its tools stay on.
     */
    internal fun forgetReads(id: String) = conversationReads.forget(id)

    private fun isAuthorized(authorization: String?, queryToken: String?, expectedToken: ByteArray) =
        MessageDigest.isEqual(authorization.orEmpty().toByteArray(), "Bearer ".toByteArray() + expectedToken) ||
            MessageDigest.isEqual(queryToken.orEmpty().toByteArray(), expectedToken)

    private fun createServer(conversationId: String? = null) = Server(
        serverInfo = Implementation(name = SERVER_NAME, version = version),
        options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        instructions = INSTRUCTIONS,
    ) {
        val server = this
        registry.tools.forEach { tool ->
            val isRemembered = conversationId != null && tool.readsProject
            val properties = tool.inputSchema["properties"]?.jsonObject
            addTool(
                name = tool.name,
                description = tool.description,
                inputSchema = ToolSchema(
                    properties = if (isRemembered) JsonObject(properties.orEmpty() + (FULL_PARAMETER to fullSchema)) else properties,
                    required = tool.inputSchema["required"]?.jsonArray?.map { it.jsonPrimitive.content },
                ),
                toolAnnotations = ToolAnnotations(readOnlyHint = tool.isReadOnly, destructiveHint = tool.isDestructive, openWorldHint = false),
                meta = alwaysLoadedMeta.takeIf { tool.isAlwaysLoaded },
            ) { request ->
                val origin = conversationId?.let { AiStepOrigin(conversationId = it, toolCallId = (request.meta?.json?.get(TOOL_USE_ID_META) as? JsonPrimitive)?.contentOrNull) }
                val clientName = server.sessions[sessionId]?.clientVersion?.name
                val arguments = request.arguments ?: JsonObject(emptyMap())
                if (isRemembered) {
                    runTool(tool.name, JsonObject(arguments - FULL_PARAMETER), origin, clientName, isFull = (arguments[FULL_PARAMETER] as? JsonPrimitive)?.booleanOrNull == true)
                } else {
                    runTool(tool.name, arguments, origin, clientName)
                }
            }
        }
    }

    /**
     * @param isFull Whether a conversation's read returns the whole result, even if it has read it before; null for a
     *   call whose result isn't remembered.
     */
    private suspend fun runTool(toolName: String, arguments: JsonObject, origin: AiStepOrigin?, clientName: String?, isFull: Boolean? = null): CallToolResult {
        val outcome = try {
            withContext(callDispatcher) { executor()?.call(toolName, arguments, origin, clientName) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            // Left to the SDK, the client would only see the message, which for a missing class is just its name.
            return result(toolName, arguments, INTERNAL_ERROR.format(throwable.toString()), isError = true)
        }
        val (result, isError) = when (outcome) {
            null -> NO_PROJECT to true
            is CommandOutcome.Success -> when (val result = remembered(outcome, toolName, arguments, origin, isFull)) {
                JsonNull -> DONE
                is JsonPrimitive if result.isString -> result.content
                else -> result.toString()
            } to false
            is CommandOutcome.Rejected -> outcome.rejection.reason.englishWith(outcome.rejection.arguments) to true
        }
        return result(toolName, arguments, result, isError, image = (outcome as? CommandOutcome.Success)?.image)
    }

    private fun remembered(outcome: CommandOutcome.Success, toolName: String, arguments: JsonObject, origin: AiStepOrigin?, isFull: Boolean?) =
        if (origin == null || isFull == null || outcome.image != null) {
            outcome.result
        } else {
            conversationReads.answer(origin.conversationId, toolName, arguments, outcome.result, isFull)
        }

    private fun result(toolName: String, arguments: JsonObject, text: String, isError: Boolean, image: ByteArray? = null): CallToolResult {
        _calls.update { (it + McpToolCall.of(toolName, arguments.toString(), text, isError)).takeLast(LOG_SIZE) }
        runCatching { onToolCalled(toolName, arguments, isError) }
        return CallToolResult(
            content = listOfNotNull(TextContent(text), image?.let { ImageContent(data = Base64.getEncoder().encodeToString(it), mimeType = "image/png") }),
            isError = isError,
        )
    }

    internal companion object {
        internal const val HOST = "127.0.0.1"
        internal const val PATH = "/mcp"
        internal const val CONVERSATIONS_PATH = "$PATH/conversations"
        private const val CONVERSATION_PARAMETER = "conversation"

        /**
         * The `_meta` that keeps a tool's schema loaded in a client that otherwise loads them only when the model looks
         * one up (Claude Code's tool search, which reads `anthropic/alwaysLoad`), so the tools nearly every conversation
         * uses cost no extra round trip.
         */
        private val alwaysLoadedMeta = buildJsonObject { put("anthropic/alwaysLoad", true) }

        /**
         * Where Claude Code puts the ID of its tool use in a call's `_meta`, which is also the ID of the ACP tool call.
         */
        private const val TOOL_USE_ID_META = "claudecode/toolUseId"

        /**
         * The parameter a conversation's read tools take on top of their own, to get the whole result again.
         */
        private const val FULL_PARAMETER = "full"
        private val fullSchema = buildJsonObject {
            put("type", "boolean")
            put("description", "Whether to return the whole result even if this conversation has read it before, rather than what changed since. Pass it whenever you don't have the earlier result, for example after a subagent read it.")
        }
        internal const val TOKEN_PARAMETER = "token"
        internal const val SERVER_NAME = "kubriko-creator"

        /**
         * The `mcp-remote` that clients without HTTP MCP reach the tools through: an exact version, bumped deliberately
         * after reading its changes, since it runs with the token in its environment.
         */
        internal const val MCP_REMOTE = "mcp-remote@0.15.2"
        private const val LOG_SIZE = 200
        private const val INTERNAL_ERROR = "Kubriko Creator ran into an internal error, so nothing was done: %1\$s. This is a problem in Kubriko Creator, not in the request."
        private const val NO_PROJECT = "No project is open in Kubriko Creator."
        private const val DONE = "Done."
        internal val INSTRUCTIONS = """
            Kubriko Creator edits games made with Kubriko, a 2D game engine for Kotlin. Actor types are built from Events
            (when something happens) and Blocks (what happens then), and Kubriko Creator generates Kotlin from them.
            Scenes place Actors of those types. Start with get_project_digest and get_block_catalog. Every change shows
            up in Kubriko Creator right away and can be undone there. Changes reach the game through a build: try them with
            start_game, press_keys, take_screenshot, and stop_game, and use read_console for what the game logged. Every call
            costs a round trip, so play a sequence of moves with one press_keys (steps, with screenshotWidth to see the
            result), and change several Blocks of an Event with one replace_blocks. Places are written Type#event.block,
            with indexes from 0. Make
            every change through these tools, never by editing the project's files in other ways: Kubriko Creator owns the
            generated code, and the user can't see or undo such edits in it. When a request needs something no Event or
            Block offers, a Kotlin Block can run a short snippet inside an Event; when even that isn't enough, do what the
            tools allow and tell the user what is missing, rather than writing Kotlin files. Keep the project organized as it
            grows, as part of every change rather than something to be asked for, so the user never inherits a mess: Actor
            types, Manager types, Scenes, pages, sprites, sounds, and music go in folders named for what they are for in the
            game (enemies, levels/world-1, ui) once a group holds more than a handful (list_resources shows the sprites',
            sounds', and music's). Put each new thing in its folder,
            making the folder first (create_folder); split a folder that grows past a dozen or so; move what is misplaced
            (move_to_folder, move_sprite_to_folder, rename_scene, move_page, and, for audio, rename_resource; a whole folder with move_folder),
            which changes no type's name, while what refers to a moved Scene or file follows it; and name things like their
            neighbors. When a group you work in is cluttered, tidy it in the same plan, saying what moves where.
        """.trimIndent().replace("\n", " ")
    }
}
