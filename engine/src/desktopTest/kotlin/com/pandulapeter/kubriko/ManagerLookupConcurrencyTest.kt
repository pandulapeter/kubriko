/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.manager.Manager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ManagerLookupConcurrencyTest {

    private class M1 : Manager()
    private class M2 : Manager()
    private class M3 : Manager()
    private class M4 : Manager()
    private class M5 : Manager()
    private class M6 : Manager()
    private class M7 : Manager()
    private class M8 : Manager()

    @Test
    fun concurrentFirstLookupsAllResolve() = repeat(200) {
        val managers = listOf(M1(), M2(), M3(), M4(), M5(), M6(), M7(), M8())
        val kubriko = Kubriko.newInstance(*managers.toTypedArray(), tickSource = TickSource.manual())
        val failures = CopyOnWriteArrayList<Throwable>()
        val startSignal = CountDownLatch(1)
        val threads = List(8) { threadIndex ->
            thread {
                startSignal.await()
                try {
                    repeat(100) {
                        for (i in managers.indices) {
                            val manager = managers[(i + threadIndex) % managers.size]
                            @Suppress("UNCHECKED_CAST")
                            assertSame(manager, kubriko.get(manager::class as KClass<Manager>))
                        }
                    }
                } catch (throwable: Throwable) {
                    failures.add(throwable)
                }
            }
        }
        startSignal.countDown()
        threads.forEach { it.join() }
        assertTrue(failures.isEmpty(), failures.firstOrNull()?.toString())
        kubriko.dispose()
    }
}
