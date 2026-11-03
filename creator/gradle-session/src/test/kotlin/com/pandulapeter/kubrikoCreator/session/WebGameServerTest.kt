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

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.Socket
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebGameServerTest {
    private val directory = testDirectory()
    private val client = HttpClient(CIO)
    private val servers = mutableListOf<WebGameServer>()

    @AfterTest
    fun tearDown() {
        servers.forEach { it.close() }
        client.close()
    }

    @Test
    fun thePageIsServedWithTheScriptThatReloadsIt() = runBlocking {
        File(directory, "index.html").writeText("<html><body><canvas></canvas></body></html>")
        val server = start()
        assertTrue(server.address.toString().startsWith("http://127.0.0.1:"))
        val response = client.get(server.address.toString())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals(
            "<html><body><canvas></canvas><script>new EventSource(\"/kubriko-creator/events\").onmessage = () => location.reload();</script></body></html>",
            response.bodyAsText(),
        )
        assertTrue(client.get("${server.address}index.html").bodyAsText().contains("EventSource"))
    }

    @Test
    fun filesAreServedWithTheMediaTypesBrowsersNeed() = runBlocking {
        File(directory, "game.wasm").writeBytes(byteArrayOf(0, 97, 115, 109))
        File(directory, "game.js").writeText("console.log(1)")
        File(directory, "composeResources/files").mkdirs()
        File(directory, "composeResources/files/main.json").writeText("[]")
        val server = start()
        val wasm = client.get("${server.address}game.wasm")
        assertEquals("application/wasm", wasm.headers[HttpHeaders.ContentType])
        assertEquals("application/javascript", client.get("${server.address}game.js").headers[HttpHeaders.ContentType]?.substringBefore(';'))
        assertEquals("[]", client.get("${server.address}composeResources/files/main.json").bodyAsText())
    }

    @Test
    fun nothingOutsideTheFolderNorAFolderIsServed() = runBlocking {
        val served = File(directory, "served").apply { mkdirs() }
        File(served, "sub").mkdirs()
        File(directory, "secret.txt").writeText("secret")
        val server = start(served)
        val java = java.net.http.HttpClient.newHttpClient()
        listOf("%2E%2E/secret.txt", "..%2Fsecret.txt", "sub", "missing.js").forEach { path ->
            val response = java.send(HttpRequest.newBuilder(URI("${server.address}$path")).build(), HttpResponse.BodyHandlers.ofString())
            assertEquals(404, response.statusCode(), path)
        }
    }

    @Test
    fun aReloadReachesEveryPageListening() = runBlocking {
        val server = start()
        val connected = CompletableDeferred<Unit>()
        val events = async {
            client.prepareGet("${server.address.toString().removeSuffix("/")}${WebGameServer.EVENTS_PATH}").execute { response ->
                assertEquals("text/event-stream", response.headers[HttpHeaders.ContentType]?.substringBefore(';'))
                val channel = response.bodyAsChannel()
                assertEquals(": connected", channel.readUTF8Line())
                channel.readUTF8Line()
                connected.complete(Unit)
                channel.readUTF8Line()
            }
        }
        withTimeout(5_000) { connected.await() }
        server.reload()
        assertEquals("data: reload", withTimeout(5_000) { events.await() })
    }

    @Test
    fun theAddressNamesTheSceneAndTurnsTheLogsOn() = runBlocking {
        val server = start()
        assertEquals("${server.address}?kubriko.creator.logging=true", server.addressOf(firstScene = null).toString())
        assertEquals("${server.address}?kubriko.creator.logging=true&kubriko.creator.scene=levels%2F1+1", server.addressOf(firstScene = "levels/1 1").toString())
    }

    @Test
    fun aPageWithoutABodyGetsTheScriptAtItsEnd() {
        assertEquals("<p>x</p><script>new EventSource(\"/kubriko-creator/events\").onmessage = () => location.reload();</script>", WebGameServer.withReloadScript("<p>x</p>"))
    }

    @Test
    fun aRequestNamingAnotherHostIsRefused() = runBlocking {
        File(directory, "index.html").writeText("<body></body>")
        val server = start()
        fun statusFor(host: String) = Socket("127.0.0.1", server.address.port).use { socket ->
            socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n\r\n".toByteArray())
            socket.getInputStream().bufferedReader().readLine()
        }
        assertEquals("HTTP/1.1 403 Forbidden", statusFor("rebound.example:${server.address.port}"))
        assertEquals("HTTP/1.1 200 OK", statusFor("127.0.0.1:${server.address.port}"))
    }

    private suspend fun start(served: File = directory) = WebGameServer.start(served).also { servers += it }
}
