/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MetadataManagerTest {

    private val instance = newManualKubriko()
    private val metadataManager = instance.kubriko.get<MetadataManager>()

    @AfterTest
    fun disposeInstance() = instance.dispose()

    private fun ManualKubriko.pause() = kubriko.get<StateManager>().updateIsRunning(false)

    @Test
    fun totalRuntimeIsTheSumOfTheTickDeltas() {
        instance.tick(16, count = 5)
        instance.tick(20)

        assertEquals(100L, metadataManager.totalRuntimeInMilliseconds.value)
    }

    @Test
    fun activeRuntimeLeavesOutTheTimeSpentPaused() {
        instance.tick(16, count = 2)
        instance.pause()

        instance.tick(16, count = 3)

        assertEquals(32L, metadataManager.activeRuntimeInMilliseconds.value)
        assertEquals(80L, metadataManager.totalRuntimeInMilliseconds.value)
    }

    @Test
    fun fpsReflectsTheTickRate() {
        instance.tick(10, count = 30)

        assertEquals(100f, metadataManager.fps.value, absoluteTolerance = 1f)
    }

    @Test
    fun platformIsDesktopOnTheJvm() {
        assertIs<MetadataManager.Platform.Desktop>(metadataManager.platform)
    }
}
