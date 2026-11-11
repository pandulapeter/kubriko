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

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Opens Creator settings on a page, for a part of the UI that points to a setting it no longer holds itself.
 */
internal fun interface OpenCreatorSettings {
    fun open(page: CreatorSettingsPage)
}

/**
 * How the window opens Creator settings, provided once by the app's content.
 */
internal val LocalOpenCreatorSettings = staticCompositionLocalOf { OpenCreatorSettings {} }
