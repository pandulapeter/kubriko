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

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.restrictToOwner
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import java.io.File
import java.io.IOException

/**
 * Keeps [McpSettings] in [file]. A missing or unreadable file means the defaults with a new random token; a file
 * that can't be decoded is kept as `mcp.json.bak`, readable by its owner only, so its token can be recovered by hand. A save
 * that can't be written leaves the settings applying until Kubriko Creator quits.
 */
internal class McpSettingsStore(
    private val file: File,
    private val createToken: () -> String = ::randomToken,
) {
    private val store = JsonFileStore(file, McpSettings.serializer(), isPrivate = true)

    fun load(): McpSettings {
        narrowPermissions()
        return store.read().valueOrNull()?.takeIf { it.token.isNotBlank() && it.port in McpSettings.portRange } ?: McpSettings(token = createToken()).also(::save)
    }

    fun save(settings: McpSettings) {
        narrowPermissions()
        store.write(settings)
    }

    /**
     * Narrows a file an older Kubriko Creator wrote with the usual permissions, which a write keeps, to its owner.
     */
    private fun narrowPermissions() {
        try {
            if (file.isFile) file.restrictToOwner()
        } catch (_: IOException) {
            // The token stays as readable as it was.
        }
    }

    fun newToken() = createToken()

    companion object {

        fun default() = McpSettingsStore(File(settingsDirectory(), "mcp.json"))
    }
}
