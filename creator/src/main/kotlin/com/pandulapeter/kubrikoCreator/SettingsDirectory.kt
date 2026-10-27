/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import java.io.File

/**
 * Kubriko Creator's settings folder, where each platform keeps application settings.
 */
internal fun settingsDirectory(
    operatingSystem: String = System.getProperty("os.name").orEmpty().lowercase(),
    home: String = System.getProperty("user.home"),
    appData: String? = System.getenv("APPDATA"),
    xdgConfigHome: String? = System.getenv("XDG_CONFIG_HOME"),
) = when {
    "mac" in operatingSystem -> File(home, "Library/Application Support/Kubriko Creator")
    "windows" in operatingSystem -> File(appData ?: "$home/AppData/Roaming", "Kubriko Creator")
    else -> File(xdgConfigHome ?: "$home/.config", "kubriko-creator")
}
