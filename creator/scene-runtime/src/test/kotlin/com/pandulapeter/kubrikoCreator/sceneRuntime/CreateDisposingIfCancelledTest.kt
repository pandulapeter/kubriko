/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateDisposingIfCancelledTest {
    private val disposed: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Test
    fun whatIsCreatedIsReturned() = runTest {
        assertEquals("view", createDisposingIfCancelled(StandardTestDispatcher(testScheduler), create = { "view" }, dispose = { disposed += it }))
        assertTrue(disposed.isEmpty())
    }

    @Test
    fun whatTheCreationThrowsReachesTheCaller() = runTest {
        assertFailsWith<IllegalStateException> {
            createDisposingIfCancelled<String>(StandardTestDispatcher(testScheduler), create = { error("Broken.") }, dispose = { disposed += it })
        }
    }

    @Test
    fun whatIsCreatedAfterTheCallerWasCancelledIsDisposedBeforeTheCancellationGoesOn(): Unit = runBlocking {
        val creation = cancelledWhileCreating { mayFinish ->
            createDisposingIfCancelled(Dispatchers.IO, create = { mayFinish().let { "view" } }, dispose = { disposed += it })
        }
        assertFailsWith<CancellationException> { creation.await() }
        assertEquals(listOf("view"), disposed.toList())
    }

    @Test
    fun aDisposalThatThrowsLeavesTheCancellationAsItIs(): Unit = runBlocking {
        val creation = cancelledWhileCreating { mayFinish ->
            createDisposingIfCancelled(
                Dispatchers.IO,
                create = { mayFinish().let { "view" } },
                dispose = {
                    disposed += it
                    error("Broken.")
                },
            )
        }
        assertFailsWith<CancellationException> { creation.await() }
        assertEquals(listOf("view"), disposed.toList())
    }

    @Test
    fun eachValueIsDisposedWhateverTheOthersThrow() {
        disposeEach(listOf("a", "b", "c")) {
            disposed += it
            if (it == "a") throw AssertionError()
            if (it == "b") error("Broken.")
        }
        assertEquals(listOf("a", "b", "c"), disposed.toList())
        assertFailsWith<StackOverflowError> { disposeEach(listOf("d")) { throw StackOverflowError() } }
    }

    @Test
    fun eachValueIsCreatedInOrder() = runTest {
        assertEquals(
            listOf("0", null, "2"),
            createEachDisposingIfCancelled(StandardTestDispatcher(testScheduler), 3, create = { index -> index.toString().takeIf { index != 1 } }, dispose = { disposed += it }),
        )
    }

    @Test
    fun creatingEachStopsWhenTheCallerIsCancelledAndDisposesWhatItCreatedInTheDisposalScope(): Unit = runBlocking {
        val executor = Executors.newSingleThreadExecutor { Thread(it, "disposing") }
        try {
            val created: MutableList<Int> = Collections.synchronizedList(ArrayList())
            val isDisposed = CountDownLatch(1)
            var disposingThread: String? = null
            val creation = cancelledWhileCreating { mayFinish ->
                createEachDisposingIfCancelled(
                    Dispatchers.IO,
                    count = 3,
                    create = { index ->
                        created += index
                        if (index == 0) mayFinish()
                        index.toString()
                    },
                    dispose = {
                        disposed += it
                        disposingThread = Thread.currentThread().name
                        isDisposed.countDown()
                    },
                    disposalScope = CoroutineScope(executor.asCoroutineDispatcher()),
                )
            }
            assertFailsWith<CancellationException> { creation.await() }
            assertTrue(isDisposed.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS))
            assertEquals(listOf(0), created.toList())
            assertEquals(listOf("0"), disposed.toList())
            assertTrue(disposingThread?.startsWith("disposing") == true, "disposed on $disposingThread")
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun creatingEachDisposesWhatItCreatedWhenOneThrows(): Unit = runBlocking {
        assertFailsWith<IllegalStateException> {
            createEachDisposingIfCancelled(Dispatchers.IO, count = 3, create = { index -> if (index == 2) error("Broken.") else index.toString() }, dispose = { disposed += it })
        }
        assertEquals(listOf("0", "1"), disposed.toList())
    }

    @Test
    fun aVirtualMachineErrorLeavesWhatWasCreatedAlone(): Unit = runBlocking {
        assertFailsWith<StackOverflowError> {
            createEachDisposingIfCancelled(Dispatchers.IO, count = 2, create = { index -> if (index == 1) throw StackOverflowError() else "0" }, dispose = { disposed += it })
        }
        assertTrue(disposed.isEmpty())
    }

    /**
     * Starts [create], handing it a function that waits until the creation is cancelled, and cancels it once it waits.
     */
    private fun <T> CoroutineScope.cancelledWhileCreating(create: suspend (mayFinish: () -> Unit) -> T) = run {
        val isCreating = CountDownLatch(1)
        val mayFinish = CountDownLatch(1)
        val creation = async(Dispatchers.Default) {
            create {
                isCreating.countDown()
                mayFinish.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
            }
        }
        try {
            assertTrue(isCreating.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS))
            creation.cancel()
        } finally {
            mayFinish.countDown()
        }
        creation
    }

    private companion object {
        const val TIMEOUT_IN_SECONDS = 5L
    }
}
