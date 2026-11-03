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

import java.io.File

/**
 * Node.js's release list and downloads as [NodeRuntime] reads them, served from [directory]: one LTS release for macOS on
 * Apple silicon, whose archive holds an `npx` and a `node`. Packing the archive needs `tar`, so tests using it skip Windows.
 */
internal class TestNodeReleases(
    private val directory: File,
) {
    val tools = File(directory, "tools")
    val downloads = mutableListOf<String>()
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")

    /**
     * A runtime installing into [tools] for [platform], whose checksum list is what [sums] makes of the archive.
     */
    fun runtime(sums: (File) -> String = ::validSums, platform: String? = "darwin-aarch64"): NodeRuntime {
        val archive = archive()
        val installer = BinaryAgentInstaller(tools, isWindows = isWindows, download = { url, target, _ ->
            downloads += url
            archive.copyTo(target, overwrite = true)
        })
        return NodeRuntime(installer, File(tools, NodeRuntime.NODE_ID), platform) { url ->
            when {
                url.endsWith("index.json") -> INDEX
                url.endsWith("SHASUMS256.txt") -> sums(archive)
                else -> null
            }
        }
    }

    /**
     * The checksum list Node.js publishes for [archive], with another platform's line too.
     */
    fun validSums(archive: File) = "${BinaryAgentInstaller.sha256Of(archive)}  node-v24.11.1-darwin-arm64.tar.gz\nabc  node-v24.11.1-win-x64.zip"

    private fun archive(): File {
        val content = File(directory, "content/node-v24.11.1-darwin-arm64/bin").also { it.mkdirs() }
        File(content, "npx").writeText("#!/bin/sh")
        File(content, "node").writeText("#!/bin/sh")
        val archive = File(directory, "node.tar.gz")
        ProcessBuilder("tar", "-czf", archive.path, "-C", File(directory, "content").path, ".").start().waitFor()
        return archive
    }

    companion object {
        const val INDEX = """[{"version":"v25.2.0","lts":false},{"version":"v24.11.1","lts":"Krypton"},{"version":"v22.20.0","lts":"Jod"}]"""
    }
}
