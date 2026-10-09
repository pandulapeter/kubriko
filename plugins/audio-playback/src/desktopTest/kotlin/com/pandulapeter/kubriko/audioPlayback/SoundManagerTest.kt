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

class SoundManagerTest {

    private val soundManager = SoundManager.newInstance()
    private val kubriko: ManualKubriko = newManualKubriko(soundManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun nothingLoadsBeforeTheFirstComposition() {
        soundManager.preload(URI)
        soundManager.play(URI)
        kubriko.tick(count = 5)

        assertEquals(0f, runBlocking { soundManager.getLoadingProgress(listOf(URI)).first() })
    }

    @Test
    fun loadingProgressOfNoUrisIsComplete() {
        assertEquals(1f, runBlocking { soundManager.getLoadingProgress(emptyList()).first() })
    }

    private companion object {
        const val URI = "files/sounds/click.wav"
    }
}
