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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorSettingsControllerTest {
    private val directory = createTempDirectory().toFile()
    private val file = File(directory, "settings.json")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun changesShowAtOnceAndAreSaved() = runTest {
        val controller = CreatorSettingsController(CreatorSettingsStore(file), backgroundScope, StandardTestDispatcher(testScheduler))
        val folder = File(directory, "games")
        controller.setAiEnabled(false)
        controller.setTheme(ThemeChoice.LIGHT)
        controller.setInterfaceScale(1.3f)
        controller.setDefaultProjectFolder(folder)
        assertFalse(controller.isAiEnabled.value)
        val expected = CreatorSettings(isAiEnabled = false, theme = ThemeChoice.LIGHT, interfaceScale = 1.3f, defaultProjectFolder = folder.absolutePath)
        assertEquals(expected, controller.settings.value)
        runCurrent()
        assertEquals(expected, CreatorSettingsStore(file).load())
    }

    @Test
    fun turningAiBackOnAndForgettingTheFolderAreSavedToo() = runTest {
        CreatorSettingsStore(file).save(CreatorSettings(isAiEnabled = false, defaultProjectFolder = directory.absolutePath))
        val controller = CreatorSettingsController(CreatorSettingsStore(file), backgroundScope, StandardTestDispatcher(testScheduler))
        assertFalse(controller.isAiEnabled.value)
        controller.setAiEnabled(true)
        controller.setDefaultProjectFolder(null)
        runCurrent()
        val saved = CreatorSettingsStore(file).load()
        assertTrue(saved.isAiEnabled)
        assertNull(saved.defaultProjectFolder)
    }

    @Test
    fun aSettingChangedJustBeforeQuittingIsSaved() = runTest {
        val controller = CreatorSettingsController(CreatorSettingsStore(file), backgroundScope, StandardTestDispatcher(testScheduler))
        controller.setTheme(ThemeChoice.DARK)
        controller.awaitSaves()
        assertEquals(ThemeChoice.DARK, CreatorSettingsStore(file).load().theme)
    }

    @Test
    fun aScaleNotOfferedIsTheClosestOne() = runTest {
        val controller = CreatorSettingsController(CreatorSettingsStore(file), backgroundScope, StandardTestDispatcher(testScheduler))
        controller.setInterfaceScale(0.7f)
        assertEquals(0.75f, controller.settings.value.interfaceScale)
    }
}
