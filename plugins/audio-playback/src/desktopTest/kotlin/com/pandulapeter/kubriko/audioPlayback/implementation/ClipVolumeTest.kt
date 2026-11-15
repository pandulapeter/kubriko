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

import kotlin.test.Test
import kotlin.test.assertEquals

class ClipVolumeTest {

    @Test
    fun fullVolumeIsTheRecordedGainAndHalfTheAmplitudeIsAboutSixDecibelsLess() {
        assertEquals(0f, gainInDecibelsOf(1f, minimum = -80f, maximum = 6f))
        assertEquals(-6.0206f, gainInDecibelsOf(0.5f, minimum = -80f, maximum = 6f), absoluteTolerance = 0.001f)
    }

    @Test
    fun silenceAndWhatTheControlCantReachStayWithinItsRange() {
        assertEquals(-80f, gainInDecibelsOf(0f, minimum = -80f, maximum = 6f))
        assertEquals(-40f, gainInDecibelsOf(0.0001f, minimum = -40f, maximum = 6f))
        assertEquals(0f, gainInDecibelsOf(1f, minimum = -80f, maximum = 0f))
    }
}
