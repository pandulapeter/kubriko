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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.CoroutineScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TickSourceTest {

    private class RecordingTickSource : TickSource() {
        val events = mutableListOf<String>()
        var initializedWith: Kubriko? = null
        var scopeDuringInitialization: CoroutineScope? = null

        fun emit(deltaTimeInMilliseconds: Int) = emitTick(deltaTimeInMilliseconds)

        override fun onInitialize(kubriko: Kubriko) {
            initializedWith = kubriko
            scopeDuringInitialization = scope
        }

        override fun onStart() {
            events.add("start")
        }

        override fun onStop() {
            events.add("stop")
        }

        override fun onDispose() {
            events.add("dispose")
        }
    }

    private class DeltaRecordingManager : Manager() {
        val deltas = mutableListOf<Int>()
        var isInitializedByStart = false

        override fun onInitialize(kubriko: Kubriko) {
            isInitializedByStart = true
        }

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            deltas.add(deltaTimeInMilliseconds)
        }
    }

    private val manager = DeltaRecordingManager()
    private val tickSource = RecordingTickSource()
    private val kubriko = Kubriko.newInstance(manager, tickSource = tickSource)

    @AfterTest
    fun disposeInstance() = kubriko.dispose()

    @Test
    fun onInitializeReceivesTheInstanceAndItsScopeBeforeStart() {
        assertSame(kubriko, tickSource.initializedWith)
        assertSame<Any?>(kubriko, tickSource.scopeDuringInitialization)
        assertEquals(emptyList(), tickSource.events)
    }

    @Test
    fun startInitializesTheInstance() {
        tickSource.start()

        assertTrue(manager.isInitializedByStart)
    }

    @Test
    fun ticksEmittedBeforeStartAreIgnored() {
        tickSource.emit(16)
        tickSource.start()

        tickSource.emit(20)

        assertEquals(listOf(20), manager.deltas)
    }

    @Test
    fun ticksEmittedAfterStopAreIgnoredUntilTheNextStart() {
        tickSource.start()
        tickSource.stop()

        tickSource.emit(16)
        tickSource.start()
        tickSource.emit(20)

        assertEquals(listOf(20), manager.deltas)
    }

    @Test
    fun repeatedStartAndStopCallsAreAppliedOnce() {
        tickSource.stop()
        tickSource.start()
        tickSource.start()
        tickSource.stop()
        tickSource.stop()

        assertEquals(listOf("start", "stop"), tickSource.events)
    }

    @Test
    fun disposingTheInstanceStopsAndThenDisposesTheTickSource() {
        tickSource.start()

        kubriko.dispose()
        tickSource.emit(16)

        assertEquals(listOf("start", "stop", "dispose"), tickSource.events)
        assertEquals(emptyList(), manager.deltas)
    }

    @Test
    fun manualTickSourceAdvancesOnlyOnTick() {
        val manualManager = DeltaRecordingManager()
        val manualTickSource = TickSource.manual()
        val manualKubriko = Kubriko.newInstance(manualManager, tickSource = manualTickSource)
        try {
            manualTickSource.tick(16)
            manualTickSource.start()
            assertEquals(emptyList(), manualManager.deltas)

            manualTickSource.tick(16)
            manualTickSource.tick(33)

            assertEquals(listOf(16, 33), manualManager.deltas)
        } finally {
            manualKubriko.dispose()
        }
    }
}
