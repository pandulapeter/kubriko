/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import com.pandulapeter.kubrikoCreator.settingsDirectory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.Base64

/**
 * Keeps [McpSettings] in [file]. A missing or unreadable file means the defaults with a new random token.
 */
internal class McpSettingsStore(
    private val file: File,
    private val createToken: () -> String = ::randomToken,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): McpSettings = read()?.takeIf { it.token.isNotBlank() && it.port in McpSettings.portRange } ?: McpSettings(token = createToken()).also(::save)

    fun save(settings: McpSettings) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(McpSettings.serializer(), settings))
        } catch (_: IOException) {
            // The settings still apply until Kubriko Creator quits.
        }
    }

    fun newToken() = createToken()

    private fun read() = try {
        if (file.isFile) json.decodeFromString(McpSettings.serializer(), file.readText()) else null
    } catch (_: IOException) {
        null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    companion object {

        fun default() = McpSettingsStore(File(settingsDirectory(), "mcp.json"))

        private fun randomToken() = ByteArray(32).also(SecureRandom()::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    }
}
