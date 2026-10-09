/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MusicManagerTest {

    private val musicManager = MusicManager.newInstance()
    private val kubriko: ManualKubriko = newManualKubriko(musicManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun volumeIsFullUntilChanged() {
        assertEquals(1f to 1f, musicManager.getVolume(URI))
    }

    @Test
    fun defaultVolumeAppliesToEveryUriWithoutItsOwnVolume() {
        musicManager.setVolume(URI, 0.2f, 0.3f)

        musicManager.setDefaultVolume(0.5f, 0.6f)

        assertEquals(0.5f to 0.6f, musicManager.getVolume(OTHER_URI))
        assertEquals(0.2f to 0.3f, musicManager.getVolume(URI))
    }

    @Test
    fun nothingLoadsOrPlaysBeforeTheFirstComposition() {
        musicManager.preload(URI)
        musicManager.play(URI)
        kubriko.tick(count = 5)

        assertFalse(musicManager.isPlaying(URI))
        assertEquals(0f, runBlocking { musicManager.getLoadingProgress(URI).first() })
    }

    @Test
    fun loadingProgressOfNoUrisIsComplete() {
        assertEquals(1f, runBlocking { musicManager.getLoadingProgress(emptyList()).first() })
    }

    private companion object {
        const val URI = "files/music/theme.mp3"
        const val OTHER_URI = "files/music/other.mp3"
    }
}
