/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.Line
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CachedSoundTest {

    @Test
    fun soundWhoseFormatNoClipAcceptsIsAFailedLoad() {
        val cachedSound = CachedSound.create(
            audioData = ByteArray(9 * 64),
            audioFormat = AudioFormat(AudioFormat.Encoding.ULAW, 8000f, 8, 9, 9, 8000f, false),
            maxSimultaneousStreams = 3,
        )

        if (AudioSystem.isLineSupported(Line.Info(Clip::class.java))) {
            assertNull(cachedSound)
        } else {
            assertNotNull(cachedSound)
            assertNull(cachedSound.getAvailableClip())
        }
    }
}
