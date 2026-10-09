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
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The thread-safety of [TickSource.start] and [TickSource.stop]: safe to call from any thread, with [TickSource.onStart]
 * and [TickSource.onStop] never overlapping, and the built-in coroutine sources never running two tick loops at once.
 */
class TickSourceLifecycleTest {

    private class TransitionRecordingTickSource : TickSource() {
        val transitions = CopyOnWriteArrayList<String>()
        val maximumConcurrency = AtomicInteger()
        private val concurrency = AtomicInteger()

        private fun record(transition: String) {
            maximumConcurrency.accumulateAndGet(concurrency.incrementAndGet(), ::maxOf)
            transitions.add(transition)
            Thread.yield()
            concurrency.decrementAndGet()
        }

        override fun onStart() = record("start")

        override fun onStop() = record("stop")
    }

    private class ConcurrencyRecordingManager : Manager() {
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
            maximumConcurrency.accumulateAndGet(concurrency.incrementAndGet(), ::maxOf)
            Thread.sleep(1)
            updates.incrementAndGet()
            concurrency.decrementAndGet()
        }
    }

    private fun runConcurrently(threadCount: Int = 8, action: (threadIndex: Int) -> Unit) {
        val startSignal = CountDownLatch(1)
        val threads = List(threadCount) { index ->
            thread {
                startSignal.await()
                action(index)
            }
        }
        startSignal.countDown()
        threads.forEach { it.join() }
    }

    private fun randomStartsAndStops(tickSource: TickSource) = runConcurrently { threadIndex ->
        val random = Random(threadIndex)
        repeat(1_000) {
            if (random.nextBoolean()) tickSource.start() else tickSource.stop()
        }
    }

    @Test
    fun concurrentStartsAndStopsApplyAlternatingNonOverlappingTransitions() {
        val tickSource = TransitionRecordingTickSource()
        val kubriko = Kubriko.newInstance(tickSource = tickSource)
        try {
            randomStartsAndStops(tickSource)
            tickSource.start()

            assertEquals(1, tickSource.maximumConcurrency.get())
            tickSource.transitions.forEachIndexed { index, transition ->
                assertEquals(if (index % 2 == 0) "start" else "stop", transition, "transition $index")
            }
            assertEquals("start", tickSource.transitions.last())
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun concurrentStartsInitializeOnceAndNeverTickBeforeInitialization() {
        val manager = ConcurrencyRecordingManager()
        val actor = CountingActor()
        val tickSource = TickSource.fixedRate(5)
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(actor)),
            manager,
            tickSource = tickSource,
        )
        try {
            runConcurrently { tickSource.start() }
            awaitCondition { manager.updates.get() > 0 }

            assertEquals(1, manager.initializations.get())
            assertEquals(1, actor.added.get())
            assertFalse(manager.wasUpdatedBeforeInitialization.get())
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun fixedRateRunsOneTickLoopAfterConcurrentStartsAndStops() = assertOneTickLoopAfterConcurrentStartsAndStops(TickSource.fixedRate(2))

    @Test
    fun fixedFrequencyRunsOneTickLoopAfterConcurrentStartsAndStops() = assertOneTickLoopAfterConcurrentStartsAndStops(TickSource.fixedFrequency(500))

    @Test
    fun fixedRateRestartedRepeatedlyNeverOverlapsTicks() = assertRestartsNeverOverlapTicks(TickSource.fixedRate(2))

    @Test
    fun fixedFrequencyRestartedRepeatedlyNeverOverlapsTicks() = assertRestartsNeverOverlapTicks(TickSource.fixedFrequency(500))

    private fun assertOneTickLoopAfterConcurrentStartsAndStops(tickSource: TickSource) {
        val manager = ConcurrencyRecordingManager()
        val kubriko = Kubriko.newInstance(manager, tickSource = tickSource)
        try {
            randomStartsAndStops(tickSource)
            tickSource.start()
            awaitCondition { manager.updates.get() >= 20 }

            tickSource.stop()
            awaitTicksToSettle(manager)

            assertEquals(1, manager.maximumConcurrency.get())
        } finally {
            kubriko.dispose()
        }
    }

    private fun assertRestartsNeverOverlapTicks(tickSource: TickSource) {
        val manager = ConcurrencyRecordingManager()
        val kubriko = Kubriko.newInstance(manager, tickSource = tickSource)
        try {
            tickSource.start()
            repeat(200) {
                tickSource.stop()
                tickSource.start()
            }
            awaitCondition { manager.updates.get() >= 20 }

            assertEquals(1, manager.maximumConcurrency.get())
        } finally {
            kubriko.dispose()
        }
    }

    /** Fails if a stopped source keeps ticking: a tick already past its check may finish, nothing may start after. */
    private fun awaitTicksToSettle(manager: ConcurrencyRecordingManager) {
        Thread.sleep(20)
        val updatesAfterStop = manager.updates.get()
        Thread.sleep(50)
        assertEquals(updatesAfterStop, manager.updates.get(), "the source kept ticking after stop()")
    }
}
