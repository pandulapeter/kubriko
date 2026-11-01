/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.zip.ZipFile

/**
 * The sources of Kubriko's libraries at [version], read from their sources jars: Maven Local's, Gradle's cache, or
 * otherwise downloaded from Maven Central into [cacheDirectory]. Every call blocks.
 *
 * @param download Saves what is at a URL into a file, or fails with an [IOException].
 */
internal class KubrikoSources(
    private val version: String,
    private val cacheDirectory: File,
    private val home: File = File(System.getProperty("user.home")),
    private val download: (url: String, target: File) -> Unit = ::httpDownload,
) {

    /**
     * A declaration found in the sources: the file it is in, and the library that has it.
     */
    data class Found(
        val artifact: KubrikoArtifact,
        val path: String,
        val text: String,
    )

    /**
     * The source file that declares the class, interface, or object [name] (its simple name), searching the engine first.
     */
    fun find(name: String): Found? {
        val declaration = Regex("""(?m)^\s*(?:(?:public|internal|sealed|abstract|open|data|value|enum|annotation|fun|expect|actual)\s+)*(?:class|interface|object|typealias)\s+""" + Regex.escape(name) + """\b""")
        KubrikoArtifact.entries.forEach { artifact ->
            val jar = sourcesJarOf(artifact) ?: return@forEach
            ZipFile(jar).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".kt") }.forEach { entry ->
                    val text = zip.getInputStream(entry).bufferedReader().readText()
                    if (declaration.containsMatchIn(text)) return Found(artifact, entry.name, text)
                }
            }
        }
        return null
    }

    private fun sourcesJarOf(artifact: KubrikoArtifact): File? {
        val fileName = "${artifact.artifactId}-$version-sources.jar"
        val groupPath = KubrikoArtifact.GROUP.replace('.', '/')
        File(home, ".m2/repository/$groupPath/${artifact.artifactId}/$version/$fileName").takeIf { it.isFile }?.let { return it }
        File(home, ".gradle/caches/modules-2/files-2.1/${KubrikoArtifact.GROUP}/${artifact.artifactId}/$version").listFiles()
            ?.firstNotNullOfOrNull { File(it, fileName).takeIf(File::isFile) }?.let { return it }
        val cached = File(cacheDirectory, fileName)
        if (cached.isFile) return cached
        return try {
            cacheDirectory.mkdirs()
            val partial = File(cacheDirectory, "$fileName.download")
            download("$MAVEN_CENTRAL/$groupPath/${artifact.artifactId}/$version/$fileName", partial)
            partial.renameTo(cached)
            cached.takeIf { it.isFile }
        } catch (_: IOException) {
            null
        }
    }

    private companion object {
        const val MAVEN_CENTRAL = "https://repo1.maven.org/maven2"
        val timeout: Duration = Duration.ofSeconds(30)

        fun httpDownload(url: String, target: File) {
            val client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build()
            val response = try {
                client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofFile(target.toPath()))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("The download of $url was interrupted.")
            }
            if (response.statusCode() != 200) {
                target.delete()
                throw IOException("The download of $url failed (HTTP ${response.statusCode()}).")
            }
        }
    }
}
