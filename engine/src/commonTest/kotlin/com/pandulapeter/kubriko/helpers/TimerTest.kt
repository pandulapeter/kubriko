/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

class TimerTest {

    @Test
    fun zeroDurationOneShotFiresOnFirstUpdate() {
        var fires = 0
        val timer = Timer(0) { fires++ }
        timer.update(16)
        assertEquals(1, fires)
        repeat(5) { timer.update(16) }
        assertEquals(1, fires)
    }

    @Test
    fun negativeDurationOneShotFiresOnce() {
        var fires = 0
        val timer = Timer(-5) { fires++ }
        timer.update(16)
        assertEquals(1, fires)
        repeat(5) { timer.update(16) }
        assertEquals(1, fires)
    }

    @Test
    fun repeatingTimerKeepsItsRate() {
        var fires = 0
        val timer = Timer(100, true) { fires++ }
        repeat(625) { timer.update(16) }
        assertEquals(100, fires)
    }

    @Test
    fun longStallFiresOnceAndKeepsPhase() {
        var fires = 0
        val timer = Timer(100, true) { fires++ }
        timer.update(1050)
        assertEquals(1, fires)
        assertEquals(50, timer.remainingTimeInMilliseconds)
    }

    @Test
    fun negativeDeltaIsIgnored() {
        val timer = Timer(100) {}
        timer.update(-50)
        assertEquals(100, timer.remainingTimeInMilliseconds)
    }

    @Test
    fun oneShotFiresOnce() {
        var fires = 0
        val timer = Timer(100) { fires++ }
        repeat(20) { timer.update(16) }
        assertEquals(1, fires)
        assertEquals(0, timer.remainingTimeInMilliseconds)
    }
}
