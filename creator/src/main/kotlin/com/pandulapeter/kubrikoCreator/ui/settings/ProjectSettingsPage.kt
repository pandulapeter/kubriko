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
 * A page of Project settings, by the key its dialog knows it by, for opening the dialog on it.
 */
internal enum class ProjectSettingsPage(val key: String) {
    IDENTITY("identity"),
    GAME("game"),
    TARGETS("targets"),
    LIBRARIES("libraries"),
    SCREENPLAY("screenplay"),
}
