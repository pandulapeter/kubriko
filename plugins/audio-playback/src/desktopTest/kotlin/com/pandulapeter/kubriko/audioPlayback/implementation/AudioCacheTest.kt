/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AudioCacheTest {

    private class FakeLoader {
        val callCount = AtomicInteger()
        private val pending = ConcurrentHashMap<String, ConcurrentLinkedQueue<CompletableDeferred<Any?>>>()

        val load: suspend (String) -> Any? = { uri ->
            callCount.incrementAndGet()
            val result = CompletableDeferred<Any?>()
            pending.getOrPut(uri) { ConcurrentLinkedQueue() }.add(result)
            result.await()
        }

        fun complete(uri: String, value: Any?) {
            pending.getValue(uri).poll().complete(value)
        }

        fun fail(uri: String, exception: Throwable) {
            pending.getValue(uri).poll().completeExceptionally(exception)
        }

        fun hasPendingLoad(uri: String) = pending[uri]?.isNotEmpty() == true
    }

    @Test
    fun concurrentGetsShareOneLoad() = runTest {
        val loader = FakeLoader()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load) {} }
        val first = async { cache.get(URI) }
        val second = async { cache.get(URI) }
        testScheduler.runCurrent()
        val value = Any()
        loader.complete(URI, value)
        assertSame(value, first.await())
        assertSame(value, second.await())
        assertEquals(1, loader.callCount.get())
    }

    @Test
    fun getAfterPreloadJoinsTheSameLoad() = runTest {
        val loader = FakeLoader()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load) {} }
        cache.preload(URI)
        val result = async { cache.get(URI) }
        testScheduler.runCurrent()
        val value = Any()
        loader.complete(URI, value)
        assertSame(value, result.await())
        assertEquals(1, loader.callCount.get())
    }

    @Test
    fun preloadBeforeAttachWaitsForTheLoader() = runTest {
        val loader = FakeLoader()
        val cache = AudioCache()
        cache.preload(URI)
        testScheduler.runCurrent()
        assertTrue(cache.entries.first().containsKey(URI))
        assertNull(cache.entries.first()[URI])
        assertEquals(0, loader.callCount.get())
        cache.attach(backgroundScope, loader.load) {}
        testScheduler.runCurrent()
        assertEquals(1, loader.callCount.get())
        val value = Any()
        loader.complete(URI, value)
        testScheduler.runCurrent()
        assertSame(value, cache.entries.first()[URI])
    }

    @Test
    fun removeWhileLoadingDiscardsTheLateResult() = runTest {
        val loader = FakeLoader()
        val discarded = mutableListOf<Any>()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load) { discarded.add(it) } }
        val waiting = async { cache.get(URI) }
        testScheduler.runCurrent()
        assertNull(cache.remove(URI))
        assertFalse(cache.entries.first().containsKey(URI))
        val value = Any()
        loader.complete(URI, value)
        assertNull(waiting.await())
        assertEquals(listOf(value), discarded)
        assertFalse(cache.entries.first().containsKey(URI))
    }

    @Test
    fun clearReturnsLoadedValuesAndDiscardsLoadsInFlight() = runTest {
        val loader = FakeLoader()
        val discarded = mutableListOf<Any>()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load) { discarded.add(it) } }
        val first = Any()
        val second = Any()
        val third = Any()
        cache.preload("a")
        cache.preload("b")
        cache.preload("c")
        testScheduler.runCurrent()
        loader.complete("a", first)
        loader.complete("b", second)
        testScheduler.runCurrent()
        assertEquals(setOf(first, second), cache.clear().toSet())
        assertTrue(cache.entries.first().isEmpty())
        loader.complete("c", third)
        testScheduler.runCurrent()
        assertEquals(listOf(third), discarded)
        assertTrue(cache.entries.first().isEmpty())
    }

    @Test
    fun abandonedLoadDoesNotOverwriteANewerOne() = runTest {
        val loader = FakeLoader()
        val discarded = mutableListOf<Any>()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load) { discarded.add(it) } }
        cache.preload(URI)
        testScheduler.runCurrent()
        cache.remove(URI)
        val fresh = async { cache.get(URI) }
        testScheduler.runCurrent()
        assertEquals(2, loader.callCount.get())
        val stale = Any()
        val current = Any()
        loader.complete(URI, stale)
        loader.complete(URI, current)
        assertSame(current, fresh.await())
        assertSame(current, cache.loaded(URI))
        assertEquals(listOf(stale), discarded)
    }

    @Test
    fun everyValueIsHandedOutExactlyOnceWhenClearRacesACompletingLoad() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            repeat(1000) {
                val loader = FakeLoader()
                val discardedCount = AtomicInteger()
                val cache = AudioCache().apply { attach(scope, loader.load) { discardedCount.incrementAndGet() } }
                val load = scope.async { cache.get(URI) }
                while (!loader.hasPendingLoad(URI)) Thread.yield()
                val value = Any()
                val start = CountDownLatch(1)
                var clearedCount = 0
                val completer = thread { start.await(); loader.complete(URI, value) }
                val clearer = thread { start.await(); clearedCount = cache.clear().count { it === value } }
                start.countDown()
                completer.join()
                clearer.join()
                runBlocking { load.await() }
                assertEquals(1, clearedCount + discardedCount.get())
                assertTrue(runBlocking { cache.entries.first() }.isEmpty())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun loadReturningNullSettlesTheEntry() = runTest {
        val loader = FakeLoader()
        val failedUris = mutableListOf<String>()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load, onFailed = { failedUris.add(it) }) {} }
        val result = async { cache.get(URI) }
        testScheduler.runCurrent()
        loader.complete(URI, null)
        assertNull(result.await())
        assertNotNull(cache.entries.first()[URI])
        assertNull(cache.loaded(URI))
        assertEquals(listOf(URI), failedUris)
    }

    @Test
    fun loadThrowingSettlesTheEntryWithoutFailingTheScope() = runTest {
        val loader = FakeLoader()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val cache = AudioCache().apply { attach(scope, loader.load) {} }
        cache.preload(URI)
        testScheduler.runCurrent()
        loader.fail(URI, IOException())
        testScheduler.runCurrent()
        assertTrue(scope.isActive)
        assertNotNull(cache.entries.first()[URI])
        assertNull(cache.loaded(URI))
        scope.cancel()
    }

    @Test
    fun failedEntryIsRetriedWhileStillCountingAsSettled() = runTest {
        val loader = FakeLoader()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load) {} }
        cache.preload(URI)
        testScheduler.runCurrent()
        loader.complete(URI, null)
        testScheduler.runCurrent()
        val first = async { cache.get(URI) }
        val second = async { cache.get(URI) }
        testScheduler.runCurrent()
        assertEquals(2, loader.callCount.get())
        assertNotNull(cache.entries.first()[URI])
        val value = Any()
        loader.complete(URI, value)
        assertSame(value, first.await())
        assertSame(value, second.await())
        assertSame(value, cache.loaded(URI))
    }

    @Test
    fun cancelledLoadDoesNotSettleTheEntry() = runTest {
        val loader = FakeLoader()
        val failedUris = mutableListOf<String>()
        val cache = AudioCache().apply { attach(backgroundScope, loader.load, onFailed = { failedUris.add(it) }) {} }
        cache.preload(URI)
        testScheduler.runCurrent()
        loader.fail(URI, CancellationException())
        testScheduler.runCurrent()
        assertTrue(cache.entries.first().containsKey(URI))
        assertNull(cache.entries.first()[URI])
        assertTrue(failedUris.isEmpty())
    }

    private companion object {
        const val URI = "uri"
    }
}
