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

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import java.io.File

/**
 * Keeps [AssistantSettings] in [file]. A missing or unreadable file means the defaults, and a save that
 * can't be written leaves the settings applying until Kubriko Creator quits.
 */
public class AssistantSettingsStore(
    file: File,
) {
    private val store = JsonFileStore(file, AssistantSettings.serializer())

    internal fun load() = store.read().valueOrNull() ?: AssistantSettings()

    internal fun save(settings: AssistantSettings) {
        store.write(settings)
    }

    public companion object {

        /**
         * The store keeping the assistant's settings in Creator's settings folder.
         */
        public fun default(): AssistantSettingsStore = AssistantSettingsStore(File(settingsDirectory(), "assistant.json"))
    }
}
