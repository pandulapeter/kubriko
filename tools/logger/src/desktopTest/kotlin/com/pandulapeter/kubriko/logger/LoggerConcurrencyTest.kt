/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.logger

import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoggerConcurrencyTest {

    @BeforeTest
    fun setUp() = Logger.clearLogs()

    @AfterTest
    fun tearDown() = Logger.clearLogs()

    @Test
    fun entriesLoggedFromSeveralThreadsAtOnceAreAllKept() {
        val start = CountDownLatch(1)
        val threads = List(THREAD_COUNT) { threadIndex ->
            thread {
                start.await()
                repeat(ENTRIES_PER_THREAD) { Logger.log("$threadIndex-$it") }
            }
        }

        start.countDown()
        threads.forEach { it.join() }

        assertEquals(THREAD_COUNT * ENTRIES_PER_THREAD, Logger.logs.value.map { it.message }.toSet().size)
    }

    private companion object {
        const val THREAD_COUNT = 4
        const val ENTRIES_PER_THREAD = 200
    }
}
