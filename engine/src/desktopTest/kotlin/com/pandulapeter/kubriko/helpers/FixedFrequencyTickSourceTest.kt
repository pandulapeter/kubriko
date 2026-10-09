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

import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedFrequencyTickSourceTest {

    private class DeltaRecordingManager(private val onTick: (tickCount: Int) -> Unit) : Manager() {
        val deltas = ArrayList<Int>()

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            deltas.add(deltaTimeInMilliseconds)
            onTick(deltas.size)
        }
    }

    /** Runs 100 ticks per second on virtual time for [virtualMilliseconds], returning the emitted deltas. */
    private fun recordDeltas(
        virtualMilliseconds: Long,
        onTick: (tickCount: Int, stall: (Long) -> Unit) -> Unit,
    ): List<Int> {
        val scheduler = TestCoroutineScheduler()
        var stallInNanoseconds = 0L
        val tickSource = FixedFrequencyTickSource(
            ticksPerSecond = 100,
            clock = TickClock { scheduler.currentTime * 1_000_000 + stallInNanoseconds },
        )
        val manager = DeltaRecordingManager { tickCount ->
            onTick(tickCount) { stallInNanoseconds += it * 1_000_000 }
        }
        val kubriko = KubrikoImpl(
            manager,
            tickSource = tickSource,
            isLoggingEnabled = false,
            instanceNameForLogging = null,
            dispatcher = StandardTestDispatcher(scheduler),
        )
        tickSource.start()
        scheduler.runCurrent()
        scheduler.advanceTimeBy(virtualMilliseconds)
        scheduler.runCurrent()
        kubriko.dispose()
        return manager.deltas
    }

    @Test
    fun firstTickHasNoDeltaAndTheRestMeasureTheElapsedTime() {
        assertEquals(listOf(0, 10, 10, 10, 10, 10), recordDeltas(virtualMilliseconds = 50) { _, _ -> })
    }

    @Test
    fun fallingBehindEmitsOneTickCarryingTheStallThenReSyncs() {
        val deltas = recordDeltas(virtualMilliseconds = 50) { tickCount, stall ->
            if (tickCount == 3) stall(55)
        }
        assertEquals(listOf(0, 10, 10, 55, 10, 10, 10), deltas)
    }
}
