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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundPassTest {

    /** Sums the numbers captured on submission into [result], waiting for [gate] while it is set. */
    private class SumPass : BackgroundPass() {
        private var captured = IntArray(0)
        var result = 0
        var runs = 0
        var gate: CompletableDeferred<Unit>? = null

        fun submit(numbers: IntArray) {
            if (isBusy) return
            captured = numbers.copyOf()
            submit()
        }

        override suspend fun run() {
            runs++
            gate?.await()
            result = captured.sum()
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val pass = SumPass().also { it.start(scope, dispatcher) }

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun `a submitted pass is collected once it has run`() {
        pass.submit(intArrayOf(1, 2, 3))
        assertTrue(pass.isBusy)
        assertFalse(pass.collect())

        dispatcher.scheduler.runCurrent()

        assertTrue(pass.collect())
        assertEquals(6, pass.result)
        assertFalse(pass.isBusy)
        assertFalse(pass.collect())
    }

    @Test
    fun `nothing is submitted while a pass is in flight`() {
        val gate = CompletableDeferred<Unit>()
        pass.gate = gate
        pass.submit(intArrayOf(1))
        dispatcher.scheduler.runCurrent()

        pass.submit(intArrayOf(100))
        gate.complete(Unit)
        dispatcher.scheduler.runCurrent()

        assertTrue(pass.collect())
        assertEquals(1, pass.result)
        assertEquals(1, pass.runs)
    }

    @Test
    fun `an abandoned pass is dropped on arrival and frees the worker`() {
        pass.submit(intArrayOf(1, 2))
        pass.abandon()
        dispatcher.scheduler.runCurrent()

        assertFalse(pass.collect())
        assertFalse(pass.isBusy)
        pass.submit(intArrayOf(5))
        dispatcher.scheduler.runCurrent()
        assertTrue(pass.collect())
        assertEquals(5, pass.result)
    }

    @Test
    fun `nothing runs before the worker is started`() {
        val unstarted = SumPass()
        unstarted.submit(intArrayOf(1))
        dispatcher.scheduler.runCurrent()

        assertFalse(unstarted.collect())
        assertEquals(0, unstarted.runs)
    }
}
