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

import com.pandulapeter.kubrikoCreator.foundation.HttpDownloads
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.IOException
import java.time.Duration

/**
 * A Node.js of Kubriko Creator's own, for users without one: the newest LTS release for this platform, downloaded,
 * checked against Node's own checksums, and unpacked by [installer], whose `npx` then runs npx agents. Every call blocks.
 *
 * @param fetch Reads what is at a URL as text, or returns null when it can't be reached.
 */
internal class NodeRuntime(
    private val installer: BinaryAgentInstaller,
    private val installedDirectory: File,
    private val platform: String? = AgentRegistry.platformKey(),
    private val fetch: (url: String) -> String? = ::download,
) {

    /**
     * The `npx` of the Node.js downloaded before, if there is one.
     */
    fun installedNpx(): File? = installedDirectory.listFiles()?.filter { File(it, INSTALLED_MARKER).isFile }?.maxByOrNull { it.lastModified() }
        ?.let { version -> version.walkTopDown().maxDepth(3).firstOrNull { it.isFile && it.name == npxName() } }

    /**
     * Downloads the newest LTS release of Node.js for this platform, unless that one is there already.
     *
     * @return Its `npx`.
     * @throws IOException If Node's releases can't be read, it has none for this platform, or the download fails.
     */
    fun install(onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> }): File {
        val nodePlatform = nodePlatformOf(platform) ?: throw IOException("Node.js has no build for this computer.")
        val version = latestLts(fetch(INDEX_URL) ?: throw IOException("Node.js's releases couldn't be read.")) ?: throw IOException("Node.js has no LTS release.")
        val folder = "node-$version-$nodePlatform"
        val archive = folder + if (nodePlatform.startsWith("win")) ".zip" else ".tar.gz"
        val sums = fetch("$DIST_URL/$version/SHASUMS256.txt") ?: throw IOException("Node.js's checksums couldn't be read.")
        val sha256 = checksumOf(sums, archive) ?: throw IOException("Node.js lists no checksum for $archive.")
        val npx = if (nodePlatform.startsWith("win")) "$folder/npx.cmd" else "$folder/bin/npx"
        val node = RegistryAgent(NODE_ID, "Node.js", version, AgentDistribution.Binary("$DIST_URL/$version/$archive", npx, sha256 = sha256))
        val program = installer.install(node, node.distribution as AgentDistribution.Binary, onProgress)
        if (!nodePlatform.startsWith("win")) File(program.parentFile, "node").setExecutable(true)
        return program
    }

    private fun npxName() = if (platform?.startsWith("windows") == true) "npx.cmd" else "npx"

    companion object {
        const val NODE_ID = "node"
        private const val INSTALLED_MARKER = ".installed"
        private const val DIST_URL = "https://nodejs.org/dist"
        private const val INDEX_URL = "$DIST_URL/index.json"
        private val json = Json { ignoreUnknownKeys = true }
        private val timeout = Duration.ofSeconds(15)

        /**
         * Node's name for the registry's [platform] (`darwin-aarch64` is `darwin-arm64`), or null for one it has no
         * build for.
         */
        internal fun nodePlatformOf(platform: String?) = when (platform) {
            "darwin-aarch64" -> "darwin-arm64"
            "darwin-x86_64" -> "darwin-x64"
            "linux-aarch64" -> "linux-arm64"
            "linux-x86_64" -> "linux-x64"
            "windows-aarch64" -> "win-arm64"
            "windows-x86_64" -> "win-x64"
            else -> null
        }

        /**
         * The newest LTS release in Node's release index ([index], newest first), like `v24.11.1`.
         */
        internal fun latestLts(index: String): String? = try {
            json.decodeFromString<List<Release>>(index).firstOrNull { (it.lts as? JsonPrimitive)?.content.let { lts -> lts != null && lts != "false" } }?.version
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

        /**
         * The checksum [sums] (a `SHASUMS256.txt`) gives [fileName].
         */
        internal fun checksumOf(sums: String, fileName: String) = sums.lineSequence().map { it.trim().split(Regex("\\s+")) }.firstOrNull { it.size == 2 && it[1] == fileName }?.first()

        private fun download(url: String) = HttpDownloads.text(url, timeout)
    }

    @Serializable
    private data class Release(
        @SerialName("version") val version: String,
        @SerialName("lts") val lts: JsonElement? = null,
    )
}
