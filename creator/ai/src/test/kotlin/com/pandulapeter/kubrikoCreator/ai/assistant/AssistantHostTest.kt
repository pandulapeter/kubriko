/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.testing.IdleTrackingDispatcher
import com.pandulapeter.kubrikoCreator.testing.awaitIdle
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class AssistantHostTest {
    private val mainExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainThread = IdleTrackingDispatcher(mainExecutor)
    private val io = IdleTrackingDispatcher(Dispatchers.IO)
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val isAiEnabled = MutableStateFlow(true)
    private val created = CopyOnWriteArrayList<CountingCloseable>()
    private val creationStarted = CompletableDeferred<Unit>()
    private val creationGate = CompletableDeferred<Unit>()
    private val host = AssistantHost(scope, io, isAiEnabled) {
        creationStarted.complete(Unit)
        creationGate.await()
        CountingCloseable().also { created += it }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainExecutor.close()
    }

    private suspend fun <T> onMain(block: suspend () -> T) = withContext(mainThread) { block() }

    private class CountingCloseable : AutoCloseable {
        val closes = AtomicInteger()

        override fun close() {
            closes.incrementAndGet()
        }
    }

    @Test
    fun getCreatesOnceAndSharesIt() = blockingTest {
        val first = scope.async { host.get() }
        val second = scope.async { host.get() }
        creationStarted.await()
        creationGate.complete(Unit)
        assertSame(first.await(), second.await())
        assertEquals(1, created.size)
    }

    @Test
    fun replaceWithAnExpectedOneLeavesANewerOneRunning() = blockingTest {
        creationGate.complete(Unit)
        val running = onMain { host.get() }
        onMain { host.replace(expected = CountingCloseable()) }
        assertEquals(0, running.closes.get())
        assertSame(running, host.current.value)
    }

    @Test
    fun getWaitsForThePendingSequence() = blockingTest {
        creationGate.complete(Unit)
        val first = onMain { host.get() }
        val stepGate = CompletableDeferred<Unit>()
        onMain {
            host.afterPending {
                stepGate.await()
                host.replace()
            }
        }
        val next = scope.async { host.get() }
        awaitIdle(mainThread, io)
        assertFalse(next.isCompleted)
        assertEquals(1, created.size)
        stepGate.complete(Unit)
        assertNotSame(first, next.await())
        assertEquals(1, first.closes.get())
    }

    @Test
    fun aFailingStepDoesntStopTheNext() = blockingTest {
        val ran = CompletableDeferred<Unit>()
        onMain {
            host.afterPending { error("x") }
            host.afterPending { ran.complete(Unit) }
        }
        ran.await()
    }

    @Test
    fun aCreationFinishingAfterTurnOffIsClosedAndRefused() = blockingTest {
        val getting = scope.async { runCatching { host.get() } }
        creationStarted.await()
        isAiEnabled.value = false
        creationGate.complete(Unit)
        assertIs<AgentClosedException>(getting.await().exceptionOrNull())
        assertEquals(1, created.single().closes.get())
        assertNull(host.current.value)
    }

    @Test
    fun aCreationFinishingAfterCloseIsClosedAndRefused() = blockingTest {
        val getting = scope.async { runCatching { host.get() } }
        creationStarted.await()
        withContext(Dispatchers.IO) { host.close() }
        creationGate.complete(Unit)
        assertIs<AgentClosedException>(getting.await().exceptionOrNull())
        assertEquals(1, created.single().closes.get())
        assertFailsWith<AgentClosedException> { onMain { host.get() } }
        assertEquals(1, created.size)
    }

    @Test
    fun closeOffTheMainThreadClosesTheCurrentOne() = blockingTest {
        creationGate.complete(Unit)
        val running = onMain { host.get() }
        withContext(Dispatchers.IO) { host.close() }
        assertEquals(1, running.closes.get())
        assertNull(host.current.value)
    }
}
