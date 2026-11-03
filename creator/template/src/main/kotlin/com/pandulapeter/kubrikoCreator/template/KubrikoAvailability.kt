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

import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Whether a Kubriko version can be downloaded from where the projects of this Kubriko Creator look for it: [mavenLocal]
 * for a development build ([useMavenLocal]), Maven Central otherwise. Kubriko Creator never points a project at one that
 * can't, since nothing would build; a version found is remembered, and one not found (or not reachable) is asked again.
 */
public class KubrikoAvailability(
    private val useMavenLocal: Boolean,
    private val mavenLocal: File = defaultMavenLocal(),
    private val mavenCentral: URI = URI("https://repo.maven.apache.org/maven2/"),
) : (String) -> Boolean {
    private val available = ConcurrentHashMap.newKeySet<String>()
    private val client by lazy { HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build() }

    /**
     * Whether [version] can be downloaded. Asking Maven Central blocks for up to a few seconds, so it is called off the main
     * thread.
     */
    override fun invoke(version: String): Boolean = version in available || isFound(version).also { if (it) available += version }

    private fun isFound(version: String): Boolean {
        val path = "${KubrikoArtifact.GROUP.replace('.', '/')}/${KubrikoArtifact.ENGINE.artifactId}/$version/${KubrikoArtifact.ENGINE.artifactId}-$version.pom"
        if (useMavenLocal) return File(mavenLocal, path).isFile
        return try {
            val request = HttpRequest.newBuilder(mavenCentral.resolve(path)).method("HEAD", HttpRequest.BodyPublishers.noBody()).timeout(TIMEOUT).build()
            client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200
        } catch (_: IOException) {
            false
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    public companion object {
        private val TIMEOUT = Duration.ofSeconds(5)

        /**
         * Where the projects of [configuration] look for Kubriko.
         */
        public fun of(configuration: CreatorConfiguration): KubrikoAvailability = KubrikoAvailability(configuration.useMavenLocal)

        /**
         * The Maven Local repository Gradle's `mavenLocal()` reads, unless a Maven `settings.xml` moves it.
         */
        private fun defaultMavenLocal() = System.getProperty("maven.repo.local")?.let(::File) ?: File(System.getProperty("user.home"), ".m2/repository")
    }
}
