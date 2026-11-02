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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Reads the [ACP Registry](https://agentclientprotocol.com/registry)'s list of agents into the ones this platform can run,
 * Claude Agent first (pinned to the version checked with Kubriko Creator, whatever the registry lists), then the rest by
 * name.
 */
internal object AgentRegistry {
    const val URL = "https://cdn.agentclientprotocol.com/registry/v1/latest/registry.json"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param platform This platform's key in the registry's binary distributions ([platformKey]).
     * @throws kotlinx.serialization.SerializationException If [text] isn't the registry's format.
     */
    fun parse(text: String, platform: String?): List<RegistryAgent> {
        val others = json.decodeFromString(Registry.serializer(), text).agents
            .mapNotNull(::entryOf)
            .filter { it.id != RegistryAgent.claude.id }
            .mapNotNull { entry -> entry.distribution.forPlatform(platform)?.let { RegistryAgent(entry.id, entry.name, entry.version, it) } }
            .sortedBy { it.name.lowercase() }
        return listOf(RegistryAgent.claude) + others
    }

    /** An entry this version can't read is left out rather than losing the others. */
    private fun entryOf(element: JsonElement) = try {
        json.decodeFromJsonElement(Entry.serializer(), element)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /**
     * The registry's name for the platform of [operatingSystem] on [architecture], as `os.name` and `os.arch` say, or
     * null for one it has no builds for.
     */
    fun platformKey(
        operatingSystem: String = System.getProperty("os.name").orEmpty(),
        architecture: String = System.getProperty("os.arch").orEmpty(),
    ): String? {
        val system = operatingSystem.lowercase().let {
            when {
                "mac" in it || "darwin" in it -> "darwin"
                "windows" in it -> "windows"
                "linux" in it -> "linux"
                else -> return null
            }
        }
        val processor = when (architecture.lowercase()) {
            "aarch64", "arm64" -> "aarch64"
            "x86_64", "amd64" -> "x86_64"
            else -> return null
        }
        return "$system-$processor"
    }

    /**
     * npx first, since it needs nothing Kubriko Creator has to download, then a build of this platform, then uvx.
     */
    private fun Distribution.forPlatform(platform: String?): AgentDistribution? =
        npx?.let { AgentDistribution.Npx(it.packageName, it.args, it.env) }
            ?: platform?.let { binary[it] }?.let { AgentDistribution.Binary(it.archive, it.cmd, it.args, it.env, it.sha256) }
            ?: uvx?.let { AgentDistribution.Uvx(it.packageName, it.args, it.env) }

    @Serializable
    private data class Registry(
        @SerialName("agents") val agents: List<JsonElement> = emptyList(),
    )

    @Serializable
    private data class Entry(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("version") val version: String,
        @SerialName("distribution") val distribution: Distribution,
    )

    @Serializable
    private data class Distribution(
        @SerialName("npx") val npx: Package? = null,
        @SerialName("uvx") val uvx: Package? = null,
        @SerialName("binary") val binary: Map<String, Archive> = emptyMap(),
    )

    @Serializable
    private data class Package(
        @SerialName("package") val packageName: String,
        @SerialName("args") val args: List<String> = emptyList(),
        @SerialName("env") val env: Map<String, String> = emptyMap(),
    )

    @Serializable
    private data class Archive(
        @SerialName("archive") val archive: String,
        @SerialName("cmd") val cmd: String,
        @SerialName("args") val args: List<String> = emptyList(),
        @SerialName("env") val env: Map<String, String> = emptyMap(),
        @SerialName("sha256") val sha256: String? = null,
    )
}
