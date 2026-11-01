/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.settingsDirectory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Downloads and unpacks the agents the registry distributes as archives, one folder per agent and version under
 * [directory], so that each version is downloaded once. Installing a version removes the agent's older ones. Every call
 * blocks.
 *
 * @param download Saves what is at a URL into a file, reporting the bytes saved so far and the total, if known.
 * @param untar Unpacks a tar archive (compressed or not) into a folder with the system's `tar`, returning its exit code.
 */
internal class BinaryAgentInstaller(
    private val directory: File,
    private val isWindows: Boolean = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows"),
    private val download: (url: String, target: File, onProgress: (downloaded: Long, total: Long?) -> Unit) -> Unit = ::httpDownload,
    private val untar: (archive: File, into: File) -> Int = ::systemUntar,
) {

    /**
     * The program of [agent]'s [distribution], downloaded and unpacked first unless that version is already.
     *
     * @param onProgress Called as the download goes on, with the bytes saved so far and the total, if known.
     * @throws IOException If it couldn't be downloaded, didn't match its checksum, or couldn't be unpacked.
     */
    fun install(agent: RegistryAgent, distribution: AgentDistribution.Binary, onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> }): File {
        val agentDirectory = File(directory, safeName(agent.id))
        val versionDirectory = File(agentDirectory, safeName(agent.version))
        val program = File(versionDirectory, distribution.command).normalize()
        if (!program.isInside(versionDirectory)) throw IOException("The agent's program, ${distribution.command}, is outside its archive.")
        val marker = File(versionDirectory, INSTALLED_MARKER)
        if (marker.isFile && program.isFile) return program
        versionDirectory.deleteRecursively()
        if (!versionDirectory.mkdirs()) throw IOException("$versionDirectory couldn't be created.")
        val archive = File(agentDirectory, "${versionDirectory.name}.download")
        try {
            download(distribution.url, archive, onProgress)
            distribution.sha256?.let { expected ->
                if (!sha256Of(archive).equals(expected, ignoreCase = true)) throw IOException("The download of ${agent.name} doesn't match its checksum.")
            }
            unpack(archive, distribution.url, versionDirectory, program)
            if (!program.isFile) throw IOException("${distribution.command} isn't in the download of ${agent.name}.")
            if (!isWindows) program.setExecutable(true)
            marker.writeText(distribution.url)
        } catch (exception: IOException) {
            versionDirectory.deleteRecursively()
            throw exception
        } finally {
            archive.delete()
        }
        agentDirectory.listFiles()?.filter { it.isDirectory && it != versionDirectory }?.forEach { it.deleteRecursively() }
        return program
    }

    private fun unpack(archive: File, url: String, into: File, program: File) {
        val name = URI.create(url).path.substringAfterLast('/').lowercase()
        when {
            name.endsWith(".zip") -> unzip(archive, into)
            tarSuffixes.any { name.endsWith(it) } -> untar(archive, into).let { exitCode ->
                if (exitCode != 0) throw IOException("tar couldn't unpack $name (exit code $exitCode).")
            }
            else -> {
                program.parentFile.mkdirs()
                archive.copyTo(program, overwrite = true)
            }
        }
    }

    private fun unzip(archive: File, into: File) {
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val target = File(into, entry.name).normalize()
                if (!target.isInside(into)) throw IOException("The archive holds ${entry.name}, which is outside it.")
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
            }
        }
        if (!isWindows) applyZipPermissions(archive, into)
    }

    /**
     * Gives the files unpacked from [archive] the Unix permissions it stores, which [ZipInputStream] can't read: an agent
     * may start other programs of its archive (Google Antigravity does), which must stay executable.
     */
    private fun applyZipPermissions(archive: File, into: File) = FileSystems.newFileSystem(archive.toPath(), mapOf("enablePosixFileAttributes" to "true")).use { zip ->
        val root = zip.getPath("/")
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) }.forEach { source ->
                val target = File(into, root.relativize(source).toString()).normalize()
                if (target.isFile && target.isInside(into)) {
                    val permissions = try {
                        Files.getPosixFilePermissions(source)
                    } catch (_: UnsupportedOperationException) {
                        null
                    }
                    permissions?.let { Files.setPosixFilePermissions(target.toPath(), it + PosixFilePermission.OWNER_READ + PosixFilePermission.OWNER_WRITE) }
                }
            }
        }
    }

    private fun File.isInside(directory: File) = canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())

    companion object {
        private const val INSTALLED_MARKER = ".installed"
        private const val UNTAR_TIMEOUT_IN_MINUTES = 5L
        private const val PROGRESS_STEP_IN_BYTES = 256 * 1024L
        private val tarSuffixes = listOf(".tar.gz", ".tgz", ".tar.bz2", ".tbz2", ".tar.xz", ".txz", ".tar")
        private val unsafeCharacters = Regex("[^A-Za-z0-9._-]")
        private val timeout = Duration.ofSeconds(30)

        fun default() = BinaryAgentInstaller(File(settingsDirectory(), "tools/agents"))

        /**
         * [name] as a folder name: only letters, digits, dots, dashes, and underscores, and never `.` or `..`.
         */
        internal fun safeName(name: String) = name.replace(unsafeCharacters, "_").let { if (it.trim('.').isEmpty()) "_" else it }

        internal fun sha256Of(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun httpDownload(url: String, target: File, onProgress: (downloaded: Long, total: Long?) -> Unit) {
            val client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build()
            val response = try {
                client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("The download of $url was interrupted.")
            }
            response.body().use { input ->
                if (response.statusCode() != 200) throw IOException("The download of $url failed (HTTP ${response.statusCode()}).")
                val total = response.headers().firstValueAsLong("Content-Length").let { if (it.isPresent && it.asLong > 0) it.asLong else null }
                target.outputStream().buffered().use { output -> copyReporting(input, output, total, onProgress) }
            }
        }

        internal fun copyReporting(input: InputStream, output: OutputStream, total: Long?, onProgress: (downloaded: Long, total: Long?) -> Unit) {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var downloaded = 0L
            var reported = 0L
            onProgress(0, total)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                downloaded += read
                if (downloaded - reported >= PROGRESS_STEP_IN_BYTES) {
                    reported = downloaded
                    onProgress(downloaded, total)
                }
            }
            onProgress(downloaded, total)
        }

        private fun systemUntar(archive: File, into: File): Int {
            val process = ProcessBuilder("tar", "-xf", archive.absolutePath, "-C", into.absolutePath).redirectErrorStream(true).start()
            process.inputStream.readAllBytes()
            if (!process.waitFor(UNTAR_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly()
                return -1
            }
            return process.exitValue()
        }
    }
}
