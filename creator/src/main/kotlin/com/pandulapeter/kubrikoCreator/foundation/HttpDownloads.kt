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

import java.io.Closeable
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration

/**
 * Downloads over HTTP through one client shared by every caller, following redirects. Every call blocks.
 */
internal object HttpDownloads {
    private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(30)

    private val client by lazy {
        HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()
    }

    /**
     * Sends a GET to [url], waiting at most [timeout] for the response when given. Fails with an [IOException] when the
     * request fails, the thread is interrupted (which stays interrupted), or the status isn't 200; then a body that is a
     * stream is closed and one that is a file deleted.
     */
    fun <T> get(url: String, bodyHandler: HttpResponse.BodyHandler<T>, timeout: Duration? = null): HttpResponse<T> {
        val request = HttpRequest.newBuilder(URI.create(url)).GET().apply { if (timeout != null) timeout(timeout) }.build()
        val response = try {
            client.send(request, bodyHandler)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("The download of $url was interrupted.")
        }
        if (response.statusCode() != 200) {
            when (val body = response.body()) {
                is Closeable -> body.close()
                is Path -> body.toFile().delete()
            }
            throw IOException("The download of $url failed (HTTP ${response.statusCode()}).")
        }
        return response
    }

    /**
     * What is at [url] as text, or null when it can't be downloaded within [timeout].
     */
    fun text(url: String, timeout: Duration): String? = try {
        get(url, HttpResponse.BodyHandlers.ofString(), timeout).body()
    } catch (_: IOException) {
        null
    }
}
