/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KubrikoAvailabilityTest {
    private val mavenLocal = createTempDirectory().toFile()
    private val requests = AtomicInteger()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/maven2/") { exchange ->
            requests.incrementAndGet()
            val isFound = exchange.requestMethod == "HEAD" && exchange.requestURI.path == "/maven2/io/github/pandulapeter/kubriko/engine/0.8.0/engine-0.8.0.pom"
            exchange.sendResponseHeaders(if (isFound) 200 else 404, -1)
            exchange.close()
        }
        start()
    }
    private val mavenCentral = URI("http://127.0.0.1:${server.address.port}/maven2/")

    @AfterTest
    fun tearDown() {
        server.stop(0)
        mavenLocal.deleteRecursively()
    }

    @Test
    fun aDevelopmentBuildLooksInMavenLocal() {
        val availability = KubrikoAvailability(useMavenLocal = true, mavenLocal = mavenLocal, mavenCentral = mavenCentral)
        assertFalse(availability("0.8.1-creator"))
        File(mavenLocal, "io/github/pandulapeter/kubriko/engine/0.8.1-creator/engine-0.8.1-creator.pom").apply { parentFile.mkdirs() }.writeText("<project/>")
        assertTrue(availability("0.8.1-creator"))
        assertEquals(0, requests.get())
    }

    @Test
    fun aReleaseLooksInMavenCentralAndRemembersWhatItFound() {
        val availability = KubrikoAvailability(useMavenLocal = false, mavenLocal = mavenLocal, mavenCentral = mavenCentral)
        assertTrue(availability("0.8.0"))
        assertTrue(availability("0.8.0"))
        assertEquals(1, requests.get())
        assertFalse(availability("0.8.1"))
        assertFalse(availability("0.8.1"))
        assertEquals(3, requests.get())
    }

    @Test
    fun anUnreachableRepositoryHasNothing() {
        server.stop(0)
        assertFalse(KubrikoAvailability(useMavenLocal = false, mavenLocal = mavenLocal, mavenCentral = mavenCentral)("0.8.0"))
    }
}
