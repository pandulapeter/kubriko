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
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TickSourceLifecycleTest {

    private class ConcurrencyRecordingManager(
        private val updateDurationInMilliseconds: Long = 0,
    ) : Manager() {
        val initializations = AtomicInteger()
        val updates = AtomicInteger()
        val maximumConcurrency = AtomicInteger()
        val wasUpdatedBeforeInitialization = AtomicBoolean(false)
        private val concurrency = AtomicInteger()

        override fun onInitialize(kubriko: Kubriko) {
            initializations.incrementAndGet()
        }

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            if (initializations.get() == 0) {
                wasUpdatedBeforeInitialization.set(true)
            }
            val current = concurrency.incrementAndGet()
            maximumConcurrency.accumulateAndGet(current, ::maxOf)
            if (updateDurationInMilliseconds > 0) {
                Thread.sleep(updateDurationInMilliseconds)
            }
            updates.incrementAndGet()
            concurrency.decrementAndGet()
        }
    }

    @Test
    fun concurrentStartStopLeavesOneLoopWithFixedRate() = assertConcurrentStartStopLeavesOneLoop(TickSource.fixedRate(5))

    @Test
    fun concurrentStartStopLeavesOneLoopWithFixedFrequency() = assertConcurrentStartStopLeavesOneLoop(TickSource.fixedFrequency(200))

    @Test
    fun stopThenStartDoesNotOverlapTicksWithFixedRate() = assertStopThenStartDoesNotOverlapTicks(TickSource.fixedRate(5))

    @Test
    fun stopThenStartDoesNotOverlapTicksWithFixedFrequency() = assertStopThenStartDoesNotOverlapTicks(TickSource.fixedFrequency(200))

    @Test
    fun concurrentStartsInitializeOnce() {
        val manager = ConcurrencyRecordingManager()
        val actorAdditions = AtomicInteger()
        val actor = object : Actor {
            override fun onAdded(kubriko: Kubriko) {
                actorAdditions.incrementAndGet()
            }
        }
        val tickSource = TickSource.fixedRate(5)
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(actor)),
            manager,
            tickSource = tickSource,
        )
        val startSignal = CountDownLatch(1)
        val threads = List(8) {
            thread {
                startSignal.await()
                tickSource.start()
            }
        }
        startSignal.countDown()
        threads.forEach { it.join() }
        Thread.sleep(100)
        assertEquals(1, manager.initializations.get())
        assertEquals(1, actorAdditions.get())
        assertTrue(manager.updates.get() > 0)
        assertFalse(manager.wasUpdatedBeforeInitialization.get())
        kubriko.dispose()
    }

    private fun assertConcurrentStartStopLeavesOneLoop(tickSource: TickSource) {
        val manager = ConcurrencyRecordingManager()
        val kubriko = Kubriko.newInstance(manager, tickSource = tickSource)
        val startSignal = CountDownLatch(1)
        val threads = List(8) { index ->
            thread {
                val random = Random(index)
                startSignal.await()
                repeat(1000) {
                    if (random.nextBoolean()) tickSource.start() else tickSource.stop()
                }
            }
        }
        startSignal.countDown()
        threads.forEach { it.join() }
        tickSource.start()
        Thread.sleep(500)
        assertEquals(1, manager.maximumConcurrency.get())
        tickSource.stop()
        Thread.sleep(100)
        val updates = manager.updates.get()
        Thread.sleep(300)
        assertEquals(updates, manager.updates.get())
        kubriko.dispose()
    }

    private fun assertStopThenStartDoesNotOverlapTicks(tickSource: TickSource) {
        val manager = ConcurrencyRecordingManager(updateDurationInMilliseconds = 20)
        val kubriko = Kubriko.newInstance(manager, tickSource = tickSource)
        tickSource.start()
        repeat(200) {
            tickSource.stop()
            tickSource.start()
        }
        Thread.sleep(200)
        assertTrue(manager.updates.get() > 0)
        assertEquals(1, manager.maximumConcurrency.get())
        tickSource.stop()
        kubriko.dispose()
    }
}
