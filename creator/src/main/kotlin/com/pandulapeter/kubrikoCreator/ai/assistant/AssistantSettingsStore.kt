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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Keeps [AssistantSettings] in [file]. A missing or unreadable file means the defaults.
 */
internal class AssistantSettingsStore(
    private val file: File,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load() = try {
        if (file.isFile) json.decodeFromString(AssistantSettings.serializer(), file.readText()) else AssistantSettings()
    } catch (_: IOException) {
        AssistantSettings()
    } catch (_: SerializationException) {
        AssistantSettings()
    } catch (_: IllegalArgumentException) {
        AssistantSettings()
    }

    fun save(settings: AssistantSettings) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(AssistantSettings.serializer(), settings))
        } catch (_: IOException) {
            // The settings still apply until Kubriko Creator quits.
        }
    }

    companion object {

        fun default() = AssistantSettingsStore(File(settingsDirectory(), "assistant.json"))
    }
}
