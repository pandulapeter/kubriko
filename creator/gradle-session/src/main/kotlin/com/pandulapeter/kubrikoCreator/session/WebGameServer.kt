/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerTypeCodeGenerator
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

/**
 * Serves a game's web distribution from [directory] on this computer only, as a browser needs it (Wasm as
 * `application/wasm`, nothing cached), and tells the pages it served to load again on [reload], through an event stream
 * their `index.html` is given a script to listen to.
 */
public class WebGameServer private constructor(
    private val directory: File,
) : AutoCloseable {
    private val reloads = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val root = directory.canonicalFile
    private lateinit var server: EmbeddedServer<*, *>

    /**
     * Where the game is served, once [start] returns.
     */
    public lateinit var address: URI
        private set

    /**
     * The address that starts the game in [firstScene] (its first one when null), with the Log Blocks on, as the web entry
     * point reads it.
     */
    public fun addressOf(firstScene: String?): URI {
        val parameters = listOfNotNull(
            ManagerTypeCodeGenerator.LOGGING_PROPERTY to "true",
            firstScene?.let { ManagerTypeCodeGenerator.FIRST_SCENE_PROPERTY to it },
        )
        return URI("$address?" + parameters.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, Charsets.UTF_8)}" })
    }

    /**
     * Tells every page served that is still open to load again.
     */
    public fun reload() {
        reloads.tryEmit(Unit)
    }

    override fun close() {
        server.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
    }

    private suspend fun start() {
        server = embeddedServer(CIO, host = HOST, port = 0) {
            routing {
                get(EVENTS_PATH) {
                    call.response.cacheControl(CacheControl.NoStore(null))
                    call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                        reloads.onSubscription {
                            writeStringUtf8(": connected\n\n")
                            flush()
                        }.collect {
                            writeStringUtf8("data: reload\n\n")
                            flush()
                        }
                    }
                }
                get("{path...}") {
                    val file = fileOf(call.parameters.getAll("path").orEmpty())
                    call.response.cacheControl(CacheControl.NoStore(null))
                    when {
                        file == null -> call.respond(HttpStatusCode.NotFound)
                        file.name == PAGE -> call.respondText(withReloadScript(withContext(Dispatchers.IO) { file.readText() }), ContentType.Text.Html)
                        else -> call.respond(LocalFileContent(file, contentTypeOf(file)))
                    }
                }
            }
        }
        try {
            server.startSuspend(wait = false)
        } catch (exception: CancellationException) {
            // The CIO engine reports a port it can't bind as a cancellation.
            server.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
            throw IOException("The game's page couldn't be served: ${exception.message}")
        }
        address = URI("http://$HOST:${server.engine.resolvedConnectors().first().port}/")
    }

    /**
     * The file [segments] name under the served folder, the page for none, or null for anything else (a folder, or a path
     * leading out of it).
     */
    private fun fileOf(segments: List<String>): File? {
        val file = File(directory, segments.ifEmpty { listOf(PAGE) }.joinToString(File.separator)).canonicalFile
        return file.takeIf { it.isFile && it.toPath().startsWith(root.toPath()) }
    }

    private fun contentTypeOf(file: File) = when (file.extension.lowercase()) {
        "wasm" -> ContentType("application", "wasm")
        "js", "mjs" -> ContentType.Application.JavaScript
        else -> ContentType.defaultForFilePath(file.path)
    }

    public companion object {
        private const val HOST = "127.0.0.1"
        private const val PAGE = "index.html"

        /**
         * Where a served page listens for [reload].
         */
        public const val EVENTS_PATH: String = "/kubriko-creator/events"

        /**
         * Starts serving [directory] at a free port.
         *
         * @throws IOException If it can't be served.
         */
        public suspend fun start(directory: File): WebGameServer = WebGameServer(directory).apply { start() }

        /**
         * [page] (an `index.html`) loading itself again when Kubriko Creator says so.
         */
        internal fun withReloadScript(page: String): String {
            val script = "<script>new EventSource(\"$EVENTS_PATH\").onmessage = () => location.reload();</script>"
            val end = page.lastIndexOf("</body>", ignoreCase = true)
            return if (end < 0) page + script else page.substring(0, end) + script + page.substring(end)
        }
    }
}
