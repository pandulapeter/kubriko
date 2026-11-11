/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioFolderIndexTest {
    @Test
    fun theFoldersAreThoseOfTheLastRefreshAndTheChangesSince() {
        val composeResources = File(testDirectory(), "src/commonMain/composeResources")
        File(composeResources, "files/sounds/enemies").mkdirs()
        File(composeResources, "files/scenes/levels").mkdirs()
        File(composeResources.parentFile.parentFile, "desktopMain/composeResources/files/desktop").mkdirs()
        val index = AudioFolderIndex { composeResources }
        assertEquals(setOf("desktop", "sounds", "sounds/enemies"), index.folders())
        index.refresh()
        File(composeResources, "files/outside").mkdirs()
        assertEquals(setOf("desktop", "sounds", "sounds/enemies"), index.folders())
        index.onCreated("music/boss")
        index.onDeleted("sounds")
        assertEquals(setOf("desktop", "music", "music/boss"), index.folders())
        index.refresh()
        assertEquals(setOf("desktop", "outside", "sounds", "sounds/enemies"), index.folders())
    }
}
