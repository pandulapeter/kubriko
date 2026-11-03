/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsDirectoryTest {

    @Test
    fun eachPlatformKeepsSettingsInItsUsualPlace() {
        assertEquals(File("/Users/me/Library/Application Support/Kubriko Creator"), settingsDirectory(null, "mac os x", "/Users/me", null, null))
        assertEquals(File("C:/Users/me/AppData/Roaming", "Kubriko Creator"), settingsDirectory(null, "windows 11", "C:/Users/me", "C:/Users/me/AppData/Roaming", null))
        assertEquals(File("/home/me/.config/kubriko-creator"), settingsDirectory(null, "linux", "/home/me", null, null))
        assertEquals(File("/xdg/kubriko-creator"), settingsDirectory(null, "linux", "/home/me", null, "/xdg"))
    }

    @Test
    fun aFolderTheSystemPropertyNamesTakesThePlatformsPlace() {
        assertEquals(File("/runs/one/settings"), settingsDirectory("/runs/one/settings", "linux", "/home/me", null, "/xdg"))
        assertEquals(File("C:/runs/one"), settingsDirectory("C:/runs/one", "windows 11", "C:/Users/me", "C:/Users/me/AppData/Roaming", null))
        assertEquals(File("/home/me/.config/kubriko-creator"), settingsDirectory(" ", "linux", "/home/me", null, null))
    }
}
