/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {
    private val mainThread = Executors.newSingleThreadExecutor { Thread(it, "main-thread") }.asCoroutineDispatcher()
    private val ioThread = Executors.newSingleThreadExecutor { Thread(it, "io-thread") }.asCoroutineDispatcher()

    @AfterTest
    fun tearDown() {
        mainThread.close()
        ioThread.close()
    }

    @Test
    fun shutdownClosesTheAiPartsOnIoAndWaitsForTheProjectsWrites() = blockingTest {
        val threads = CopyOnWriteArrayList<String>()
        val written = CompletableDeferred<Unit>()
        val isSettingSaved = CompletableDeferred<Unit>()
        val shutdown = launch(mainThread) {
            shutDownSideBySide(
                timeoutInMilliseconds = 5_000,
                ioDispatcher = ioThread,
                offMainThread = listOf({ threads += Thread.currentThread().name }, { threads += Thread.currentThread().name }),
                onMainThread = listOf({ written.await() }),
                last = { isSettingSaved.complete(Unit) },
            )
        }
        assertFalse(shutdown.isCompleted)
        assertFalse(isSettingSaved.isCompleted)
        written.complete(Unit)
        shutdown.join()
        assertTrue(isSettingSaved.isCompleted)
        assertEquals(2, threads.size)
        assertTrue(threads.all { it.startsWith("io-thread") }, threads.toString())
    }

    @Test
    fun aCloseThatThrowsDoesNotSkipTheOthers() = runTest {
        val closed = mutableListOf<String>()
        shutDownSideBySide(
            timeoutInMilliseconds = 5_000,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            offMainThread = listOf({ throw IOException("The server didn't stop.") }, { closed += "assistant" }),
            onMainThread = listOf({ throw IllegalStateException("Broken.") }, { closed += "project" }),
            last = { closed += "settings" },
        )
        assertEquals(setOf("assistant", "project", "settings"), closed.toSet())
    }

    @Test
    fun shutdownGivesUpAfterTheTimeout() = runTest {
        var isLastRun = false
        shutDownSideBySide(
            timeoutInMilliseconds = 1_000,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            offMainThread = emptyList(),
            onMainThread = listOf({ awaitCancellation() }),
            last = { isLastRun = true },
        )
        assertEquals(1_000, currentTime)
        assertFalse(isLastRun)
    }
}
