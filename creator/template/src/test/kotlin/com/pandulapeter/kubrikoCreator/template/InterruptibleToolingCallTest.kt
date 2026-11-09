/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CompletableDeferred
import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class InterruptibleToolingCallTest {

    @Test
    fun aResultIsReturned() {
        assertEquals("done", awaitInterruptibly<String>(cancel = {}) { handler -> thread { handler.onComplete("done") } })
    }

    @Test
    fun aFailureIsThrownAsItIs() {
        val failure = GradleConnectionException("Could not install Gradle distribution.")
        assertSame(failure, assertFailsWith<GradleConnectionException> { awaitInterruptibly<Unit>(cancel = {}) { handler -> handler.onFailure(failure) } })
    }

    @Test
    fun anInterruptCancelsTheOperationAndReturnsOnlyOnceGradleStoppedIt() = blockingTest {
        val events = Collections.synchronizedList(mutableListOf<String>())
        val started = CountDownLatch(1)
        val finished = CompletableDeferred<Throwable>()
        lateinit var operation: ResultHandler<Unit>
        val waiting = thread {
            try {
                awaitInterruptibly<Unit>(
                    cancel = {
                        events += "cancelled"
                        thread {
                            events += "stopped"
                            operation.onFailure(BuildCancelledException("Cancelled."))
                        }
                    },
                ) { handler ->
                    operation = handler
                    started.countDown()
                }
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
}
