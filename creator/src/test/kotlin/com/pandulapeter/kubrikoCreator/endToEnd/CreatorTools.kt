/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Kubriko Creator's MCP tools, called the way an outside AI client calls them, on the server [settings] describe.
 */
internal class CreatorTools(
    private val settings: McpSettings,
) : AutoCloseable {
    private val httpClient = HttpClient(CIO) {
        install(SSE)
        // A build behind start_game takes minutes the first time; RequestOptions bound each call instead.
        engine { requestTimeout = 0 }
    }
    private var client: Client? = null

    /**
     * Calls [tool] with [arguments], waiting at most [timeout] for its answer, and connecting first if needed.
     */
    fun call(tool: String, arguments: Map<String, Any?> = emptyMap(), timeout: Duration = DEFAULT_TIMEOUT): ToolResult = runBlocking {
        val result = connected().callTool(name = tool, arguments = arguments, options = RequestOptions(timeout = timeout))
        ToolResult(
            text = result.content.filterIsInstance<TextContent>().joinToString("\n") { it.text },
            isError = result.isError == true,
            png = result.content.filterIsInstance<ImageContent>().firstOrNull()?.let { Base64.getDecoder().decode(it.data) },
        )
    }

    /**
     * Waits until the server answers and a project is open in Kubriko Creator, which happens once its window has opened
     * the project it was started with.
     *
     * @param isAlive Whether Kubriko Creator still runs; it stops the wait early when it doesn't.
     */
    fun awaitOpenProject(timeout: Duration = OPENING_TIMEOUT, isAlive: () -> Boolean) = runBlocking {
        withTimeout(timeout) {
            var lastProblem = "The MCP server never answered."
            while (true) {
                check(isAlive()) { "Kubriko Creator quit before a project was open. $lastProblem" }
                val answer = try {
                    call(OPENING_PROBE, timeout = 10.seconds)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    client = null
                    lastProblem = "The MCP server didn't answer: ${exception.message}"
                    null
                }
                if (answer != null && !answer.isError) return@withTimeout
                if (answer != null) lastProblem = answer.text
                delay(POLLING_INTERVAL)
            }
        }
    }

    private suspend fun connected() = client ?: Client(Implementation(name = "kubriko-creator-end-to-end", version = "1.0.0")).also { created ->
        created.connect(StreamableHttpClientTransport(httpClient, settings.url) { header(HttpHeaders.Authorization, "Bearer ${settings.token}") })
        client = created
    }

    override fun close() {
        runBlocking { runCatching { client?.close() } }
        httpClient.close()
    }

    companion object {
        private const val OPENING_PROBE = "get_project_overview"
        private val POLLING_INTERVAL = 500.milliseconds
        val DEFAULT_TIMEOUT = 1.minutes
        val OPENING_TIMEOUT = 3.minutes
    }
}

/**
 * What a tool answered: its text (JSON for most tools), whether it was an error, and the image it returned, if any.
 */
internal class ToolResult(
    val text: String,
    val isError: Boolean,
    val png: ByteArray?,
) {
    /**
     * The text as a JSON object, for tools that answer with one.
     */
    val json: JsonObject get() = Json.parseToJsonElement(text) as JsonObject

    operator fun get(key: String): JsonElement? = json[key]
}
