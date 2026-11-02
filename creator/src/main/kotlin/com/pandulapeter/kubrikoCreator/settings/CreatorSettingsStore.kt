/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.settings

import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.project.writeTextAtomically
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.math.abs

/**
 * Keeps [CreatorSettings] in [file]. A missing or unreadable file means the defaults, and an interface scale that isn't
 * offered the closest one that is.
 */
internal class CreatorSettingsStore(
    private val file: File,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): CreatorSettings = read()?.let { it.copy(interfaceScale = closestInterfaceScale(it.interfaceScale)) } ?: CreatorSettings()

    fun save(settings: CreatorSettings) {
        try {
            file.parentFile?.mkdirs()
            file.writeTextAtomically(json.encodeToString(CreatorSettings.serializer(), settings))
        } catch (_: IOException) {
            // The settings still apply until Kubriko Creator quits.
        }
    }

    private fun read() = try {
        if (file.isFile) json.decodeFromString(CreatorSettings.serializer(), file.readText()) else null
    } catch (_: IOException) {
        null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    companion object {

        fun default() = CreatorSettingsStore(File(settingsDirectory(), "settings.json"))

        fun closestInterfaceScale(scale: Float) = CreatorSettings.interfaceScales.minBy { abs(it - scale) }
    }
}
