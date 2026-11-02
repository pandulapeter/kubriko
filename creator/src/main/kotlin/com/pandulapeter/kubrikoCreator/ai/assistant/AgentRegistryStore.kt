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
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import java.time.Duration

/**
 * The registry's agents for this platform: fetched with [fetch] and kept in [cacheFile], so that the list works offline.
 * Every call blocks.
 *
 * @param fetch Downloads the registry, or returns null when it can't be reached.
 */
internal class AgentRegistryStore(
    private val cacheFile: File,
    private val platform: String? = AgentRegistry.platformKey(),
    private val fetch: () -> String? = ::download,
) {

    /**
     * The agents of the registry as last fetched, or Claude Agent alone before the first fetch.
     */
    fun cached(): List<RegistryAgent> = try {
        if (cacheFile.isFile) AgentRegistry.parse(cacheFile.readText(), platform) else listOf(RegistryAgent.claude)
    } catch (_: IOException) {
        listOf(RegistryAgent.claude)
    } catch (_: SerializationException) {
        listOf(RegistryAgent.claude)
    } catch (_: IllegalArgumentException) {
        listOf(RegistryAgent.claude)
    }

    /**
     * Fetches the registry and keeps it, returning its agents, or null when it can't be reached or read, in which case
     * the cached list stays.
     */
    fun refresh(): List<RegistryAgent>? {
        val text = fetch() ?: return null
        val agents = try {
            AgentRegistry.parse(text, platform)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeTextAtomically(text)
        } catch (_: IOException) {
            // The list still applies until Kubriko Creator quits.
        }
        return agents
    }

    companion object {
        private val timeout = Duration.ofSeconds(15)

        fun default() = AgentRegistryStore(File(settingsDirectory(), "agent-registry.json"))

        private fun download() = HttpDownloads.text(AgentRegistry.URL, timeout)
    }
}
