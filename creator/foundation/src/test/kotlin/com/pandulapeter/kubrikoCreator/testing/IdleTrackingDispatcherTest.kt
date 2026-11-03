/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.Pipe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class IdleTrackingDispatcherTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val latch = CountDownLatch(1)
    private val pipes = mutableListOf<Pipe>()

    @AfterTest
    fun tearDown() {
        latch.countDown()
        pipes.forEach { it.sink().close() }
        scope.cancel()
    }

    @Test
    fun aDispatcherIsIdleOnlyOnceItsQueuedWorkRan() = blockingTest {
        val dispatcher = IdleTrackingDispatcher(Dispatchers.Default)
        scope.launch(dispatcher) { latch.await() }
        assertFailsWith<AssertionError> { dispatcher.awaitIdle(timeout = 200.milliseconds) }
        latch.countDown()
        dispatcher.awaitIdle()
    }

    @Test
    fun aCoroutineSuspendedOnAFlowCountsAsIdle() = blockingTest {
        val dispatcher = IdleTrackingDispatcher(Dispatchers.Default)
        val flow = MutableStateFlow(false)
        val collector = scope.launch(dispatcher) { flow.first { it } }
        dispatcher.awaitIdle()
        flow.value = true
        collector.join()
    }

    @Test
    fun aBlockingReadOfAParkedStreamCountsAsIdle() = blockingTest {
        val dispatcher = IdleTrackingDispatcher(Dispatchers.IO)
        readOneByteFrom(dispatcher) { it.parked() }
        dispatcher.awaitIdle()
        readOneByteFrom(dispatcher) { it }
        assertFailsWith<AssertionError> { dispatcher.awaitIdle(timeout = 200.milliseconds) }
    }

    @Test
    fun awaitTrueWaitsInRealTimeUnderRunTest() = runTest {
        val isSet = AtomicBoolean()
        thread {
            Thread.sleep(100)
            isSet.set(true)
        }
        awaitTrue(timeout = 2.seconds) { isSet.get() }
    }

    @Test
    fun workHandedToAnotherDispatcherKeepsBothBusy() = blockingTest {
        val first = IdleTrackingDispatcher(Dispatchers.Default)
        val second = IdleTrackingDispatcher(Dispatchers.IO)
        scope.launch(first) { withContext(second) { latch.await() } }
        assertFailsWith<AssertionError> { awaitIdle(first, second, timeout = 200.milliseconds) }
        latch.countDown()
        awaitIdle(first, second)
    }

    private fun readOneByteFrom(dispatcher: IdleTrackingDispatcher, wrap: (InputStream) -> InputStream) {
        val pipe = Pipe.open().also { pipes += it }
        val stream = wrap(Channels.newInputStream(pipe.source()))
        scope.launch(dispatcher) { stream.read() }
    }
}
