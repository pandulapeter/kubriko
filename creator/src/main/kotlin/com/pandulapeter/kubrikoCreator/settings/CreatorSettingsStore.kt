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

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs

/**
 * Keeps [CreatorSettings] in [file]. A missing or unreadable file means the defaults, and an interface scale that isn't
 * offered the closest one that is. A
 * save that can't be written leaves the settings applying until Kubriko Creator quits.
 */
internal class CreatorSettingsStore(
    file: File,
) {
    private val store = JsonFileStore(
        file = file,
        serializer = CreatorSettings.serializer(),
        json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
    )

    fun load(): CreatorSettings = store.read().valueOrNull()?.let { it.copy(interfaceScale = closestInterfaceScale(it.interfaceScale)) } ?: CreatorSettings()

    fun save(settings: CreatorSettings) {
        store.write(settings)
    }

    companion object {

        fun default() = CreatorSettingsStore(File(settingsDirectory(), "settings.json"))

        fun closestInterfaceScale(scale: Float) = CreatorSettings.interfaceScales.minBy { abs(it - scale) }
    }
}
