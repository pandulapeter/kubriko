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
import java.io.File
import java.io.IOException

/**
 * Keeps [McpSettings] in [file]. A missing or undecodable file means the defaults with a new random token; a file
 * that can't be decoded is kept as `mcp.json.bak`, readable by its owner only, so its token can be recovered by hand. A file
 * that can't be read at all (locked by another program, say) is left alone, so its token works again once it can be read,
 * and the new token applies until Kubriko Creator quits, as does a save that can't be written.
 */
public class McpSettingsStore(
    private val file: File,
    readText: (File) -> String = File::readText,
    private val createToken: () -> String = ::randomToken,
) {
    private val store = JsonFileStore(file, McpSettings.serializer(), isPrivate = true, readText = readText)

    /**
     * The settings in the file, or the defaults with a new token, saved unless the file couldn't be read at all.
     */
    internal fun load(): McpSettings {
        narrowPermissions()
        var loaded: McpSettings? = null
        store.update { current ->
            val valid = current?.takeIf { it.token.isNotBlank() && it.port in McpSettings.portRange }
            loaded = valid ?: McpSettings(token = createToken())
            if (valid == null) loaded else null
        }
        return loaded ?: McpSettings(token = createToken())
    }

    internal fun save(settings: McpSettings) {
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

    internal fun newToken() = createToken()

    public companion object {

        /**
         * The store keeping the settings in Creator's settings folder.
         */
        public fun default(): McpSettingsStore = McpSettingsStore(File(settingsDirectory(), "mcp.json"))
    }
}
