/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.GradleConnectionException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class ToolingOutcomeTest {
    private val events = Collections.synchronizedList(mutableListOf<String>())

    /**
     * An outcome whose operation, once cancelled, stops on a thread of its own.
     */
    private fun stoppingOutcome(): ToolingOutcome<Unit> {
        lateinit var outcome: ToolingOutcome<Unit>
        outcome = ToolingOutcome(
            cancel = {
                events += "cancelled"
                thread {
                    events += "stopped"
                    outcome.onFailure(BuildCancelledException("Cancelled."))
                }
            },
        )
        return outcome
    }

    @Test
    fun aResultIsReturned() = blockingTest {
        val outcome = ToolingOutcome<String>(cancel = {})
        thread { outcome.onComplete("done") }
        assertEquals("done", outcome.awaitBlocking())
        assertEquals("done", outcome.await())
    }

    @Test
    fun aFailureIsThrownAsItIs() = blockingTest {
        val failure = GradleConnectionException("Could not install Gradle distribution.")
        val outcome = ToolingOutcome<Unit>(cancel = {}).apply { onFailure(failure) }
        assertSame(failure, assertFailsWith<GradleConnectionException> { outcome.awaitBlocking() })
        assertSame(failure, assertFailsWith<GradleConnectionException> { outcome.await() })
    }

    @Test
    fun anInterruptCancelsTheOperationAndReturnsOnlyOnceGradleStoppedIt() = blockingTest {
        val outcome = stoppingOutcome()
        val started = CountDownLatch(1)
        val finished = CompletableDeferred<Throwable>()
        val waiting = thread {
            started.countDown()
            try {
                outcome.awaitBlocking()
            } catch (exception: Throwable) {
                events += "returned"
                finished.complete(exception)
            }
        }
        started.await()
        waiting.interrupt()
        assertEquals(InterruptedException::class, finished.await()::class)
        assertEquals(listOf("cancelled", "stopped", "returned"), events.toList())
    }

    @Test
    fun aCancelledCoroutineCancelsTheOperationAndReturnsOnlyOnceGradleStoppedIt() = blockingTest {
        val outcome = stoppingOutcome()
        val waiting = async(start = CoroutineStart.UNDISPATCHED) {
            try {
                outcome.await()
            } catch (exception: CancellationException) {
                events += "returned"
                throw exception
            }
        }
        waiting.cancelAndJoin()
        assertEquals(listOf("cancelled", "stopped", "returned"), events.toList())
    }

    @Test
    fun anOperationThatNeverStopsIsWaitedForOnlyUntilTheStopTimeout() {
        val outcome = ToolingOutcome<Unit>(cancel = {}, stopTimeout = 50.milliseconds)
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> { outcome.awaitBlocking() }
            assertFalse(outcome.hasEnded)
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun aCancelledCoroutineWaitsForAnOperationThatNeverStopsOnlyUntilTheStopTimeout() = blockingTest {
        val outcome = ToolingOutcome<Unit>(cancel = {}, stopTimeout = 50.milliseconds)
        async(start = CoroutineStart.UNDISPATCHED) { outcome.await() }.cancelAndJoin()
        assertFalse(outcome.hasEnded)
    }
}
