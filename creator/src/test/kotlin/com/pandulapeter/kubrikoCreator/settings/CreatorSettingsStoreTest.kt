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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatorSettingsStoreTest {
    private val directory = createTempDirectory().toFile()
    private val file = File(directory, "settings/settings.json")
    private val store = CreatorSettingsStore(file)

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun theFirstLoadHasAiOnTheSystemThemeAndCampfiresScale() {
        assertEquals(CreatorSettings(isAiEnabled = true, theme = ThemeChoice.SYSTEM, interfaceScale = 0.85f, defaultProjectFolder = null), store.load())
    }

    @Test
    fun savedSettingsAreLoadedAgain() {
        val settings = CreatorSettings(isAiEnabled = false, theme = ThemeChoice.DARK, interfaceScale = 1.15f, defaultProjectFolder = File(directory, "games").absolutePath)
        store.save(settings)
        assertEquals(settings, CreatorSettingsStore(file).load())
    }

    @Test
    fun unreadableSettingsAreTheDefaults() {
        listOf("{ nope", """{"theme":"SEPIA"}""", """{"isAiEnabled":"maybe"}""").forEach { content ->
            file.parentFile.mkdirs()
            file.writeText(content)
            assertEquals(CreatorSettings(), store.load())
        }
    }

    @Test
    fun anInterfaceScaleNotOfferedBecomesTheClosestOne() {
        file.parentFile.mkdirs()
        file.writeText("""{"interfaceScale":0.98,"someLaterSetting":3}""")
        assertEquals(1f, store.load().interfaceScale)
    }
}
