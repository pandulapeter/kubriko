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

import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RenderWarmUpTest {

    private var now = 0L
    private val warmUp = RenderWarmUp(
        imperceptibleFrameWork = 1.milliseconds,
        maximumDuration = 5.seconds,
        clock = { now },
    )

    /** One frame that took [workInMilliseconds] and [frameInMilliseconds] of wall time in all. */
    private fun frame(workInMilliseconds: Long, frameInMilliseconds: Long = 16, hasDrawn: Boolean = true) {
        warmUp.measure { now += workInMilliseconds * 1_000_000 }
        now += (frameInMilliseconds - workInMilliseconds) * 1_000_000
        warmUp.commitFrame(hasDrawn)
    }

    @Test
    fun `a cost already imperceptible ends the wait after one window`() {
        repeat(RenderWarmUp.WINDOW_FRAMES - 1) { frame(workInMilliseconds = 0) }
        assertFalse(warmUp.isWarm.value)

        frame(workInMilliseconds = 0)

        assertTrue(warmUp.isWarm.value)
        assertFalse(warmUp.isMeasuring)
    }

    @Test
    fun `a falling cost keeps the wait up until it plateaus`() {
        var work = 12L
        var frames = 0
        while (!warmUp.isWarm.value && frames < 200) {
            frame(workInMilliseconds = work)
            frames++
            if (frames % 20 == 0 && work > 6) work -= 2
        }

        assertTrue(warmUp.isWarm.value)
        assertTrue(work <= 6, "The wait ended while the cost was still falling, at $work ms.")
    }

    @Test
    fun `frames that drew nothing neither count nor start the deadline`() {
        repeat(1_000) { frame(workInMilliseconds = 10, frameInMilliseconds = 100, hasDrawn = false) }
        assertFalse(warmUp.isWarm.value)

        repeat(RenderWarmUp.WINDOW_FRAMES) { frame(workInMilliseconds = 0) }

        assertTrue(warmUp.isWarm.value)
    }

    @Test
    fun `a cost that never settles ends the wait at the deadline`() {
        var frames = 0
        while (!warmUp.isWarm.value) {
            frame(workInMilliseconds = 10L + frames % 3 * 5, frameInMilliseconds = 100)
            frames++
        }

        assertEquals(51, frames)
    }

    @Test
    fun `measuring stops once warm`() {
        repeat(RenderWarmUp.WINDOW_FRAMES) { frame(workInMilliseconds = 0) }
        var hasRun = false

        val result = warmUp.measure { hasRun = true; 42 }
        warmUp.commitFrame(hasDrawn = true)

        assertTrue(hasRun)
        assertEquals(42, result)
        assertTrue(warmUp.isWarm.value)
    }

    @Test
    fun `measuring a frame allocates nothing`() {
        val bytes = measureAllocatedBytesPerRun {
            warmUp.measure { now += 20_000_000 }
            warmUp.commitFrame(hasDrawn = false)
        }

        assertTrue(warmUp.isMeasuring)
        assertTrue(bytes < 1, "Measuring a frame allocated $bytes bytes.")
    }
}
