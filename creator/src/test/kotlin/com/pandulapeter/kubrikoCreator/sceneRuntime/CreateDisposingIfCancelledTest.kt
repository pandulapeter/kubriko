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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateDisposingIfCancelledTest {
    private val disposed = mutableListOf<String>()

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
    fun whatIsCreatedAfterTheCallerWasCancelledIsDisposed() = runBlocking {
        val isCreating = CountDownLatch(1)
        val mayFinish = CountDownLatch(1)
        val creation = async(Dispatchers.Default) {
            createDisposingIfCancelled(
                Dispatchers.IO,
                create = {
                    isCreating.countDown()
                    mayFinish.await()
                    "view"
                },
                dispose = { synchronized(disposed) { disposed += it } },
            )
        }
        isCreating.await()
        creation.cancel()
        mayFinish.countDown()
        runCatching { creation.await() }
        assertEquals(listOf("view"), synchronized(disposed) { disposed.toList() })
    }
}
