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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedRateTickSourceTest {

    private class DeltaRecordingManager : Manager() {
        val deltas = ArrayList<Int>()

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            deltas.add(deltaTimeInMilliseconds)
        }
    }

    private val scheduler = TestCoroutineScheduler()
    private val manager = DeltaRecordingManager()
    private val tickSource = TickSource.fixedRate(intervalInMilliseconds = 10)
    private val kubriko = KubrikoImpl(
        manager,
        tickSource = tickSource,
        isLoggingEnabled = false,
        instanceNameForLogging = null,
        dispatcher = StandardTestDispatcher(scheduler),
    )

    @AfterTest
    fun disposeInstance() = kubriko.dispose()

    private fun advanceVirtualTimeBy(milliseconds: Long) {
        scheduler.advanceTimeBy(milliseconds)
        scheduler.runCurrent()
    }

    @Test
    fun noTicksAreEmittedBeforeStart() {
        advanceVirtualTimeBy(50)

        assertEquals(emptyList(), manager.deltas)
    }

    @Test
    fun firstTickHasNoDeltaAndTheRestCarryTheInterval() {
        tickSource.start()
        scheduler.runCurrent()

        advanceVirtualTimeBy(30)

        assertEquals(listOf(0, 10, 10, 10), manager.deltas)
    }

    @Test
    fun stopEndsTheTicks() {
        tickSource.start()
        advanceVirtualTimeBy(20)
        val deltasBeforeStop = manager.deltas.toList()

        tickSource.stop()
        advanceVirtualTimeBy(50)

        assertEquals(deltasBeforeStop, manager.deltas)
    }
}
