/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpDownloadsTest {
    private val executor = Executors.newCachedThreadPool { runnable -> Thread(runnable).apply { isDaemon = true } }
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        executor = this@HttpDownloadsTest.executor
        createContext("/ok") { it.respond(200, "hello") }
        createContext("/missing") { it.respond(404, "nope") }
        createContext("/broken") { it.respond(500, "broken") }
        createContext("/moved") { exchange ->
            exchange.responseHeaders.add("Location", "/ok")
            exchange.respond(302, "")
        }
        createContext("/slow") { exchange ->
            Thread.sleep(2_000)
            exchange.respond(200, "late")
        }
        start()
    }

    private fun url(path: String) = "http://${server.address.hostString}:${server.address.port}$path"

    private fun HttpExchange.respond(status: Int, body: String) {
        val bytes = body.toByteArray()
        sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    @AfterTest
    fun tearDown() {
        server.stop(0)
        executor.shutdownNow()
    }

    @Test
    fun textReturnsTheBody() {
        assertEquals("hello", HttpDownloads.text(url("/ok"), TIMEOUT))
    }

    @Test
    fun textIsNullOnAnError() {
        assertNull(HttpDownloads.text(url("/missing"), TIMEOUT))
        val closed = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        val closedUrl = "http://${closed.address.hostString}:${closed.address.port}/ok"
        closed.stop(0)
        assertNull(HttpDownloads.text(closedUrl, TIMEOUT))
    }

    @Test
    fun getThrowsWithTheStatus() {
        val target = File(testDirectory(), "download")
        val exception = assertFailsWith<IOException> { HttpDownloads.get(url("/broken"), HttpResponse.BodyHandlers.ofFile(target.toPath())) }
        assertTrue("HTTP 500" in exception.message.orEmpty())
        assertFalse(target.exists())
    }

    @Test
    fun getFollowsRedirects() {
        assertEquals("hello", HttpDownloads.get(url("/moved"), HttpResponse.BodyHandlers.ofString()).body())
    }

    @Test
    fun anInterruptedDownloadKeepsTheInterrupt() {
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<IOException> { HttpDownloads.get(url("/slow"), HttpResponse.BodyHandlers.ofString()) }
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted()
        }
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
