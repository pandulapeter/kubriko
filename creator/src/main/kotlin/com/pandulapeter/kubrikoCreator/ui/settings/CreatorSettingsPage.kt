/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

/**
 * The pages of Creator settings, by the key [CreatorSettingsDialog] lists them under, so that a page can be opened directly.
 */
internal enum class CreatorSettingsPage(
    val key: String,
) {
    APPEARANCE("appearance"),
    EDITOR("editor"),
    AI("ai"),

    /**
     * The assistant's agent and the configuration it runs as, shown only while AI features are on.
     */
    ASSISTANT("assistant"),

    /**
     * The AI connection's port, token, and commands, shown only while AI features are on.
     */
    AI_CONNECTION("aiConnection"),
}
