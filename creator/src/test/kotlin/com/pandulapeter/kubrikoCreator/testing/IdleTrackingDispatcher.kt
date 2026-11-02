/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import kotlinx.coroutines.CoroutineDispatcher
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs everything on [delegate] while counting what is queued or running on it, so that a test can wait until it is idle before
 * asserting that nothing happened.
 *
 * Idle means no coroutine queued or running on it. A coroutine suspended on a flow or a delay is idle, which is what a negative
 * assertion wants: nothing is going to happen without a new event. So is one blocked in [parkedWhile].
 */
internal class IdleTrackingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    private val pending = AtomicInteger()

    val isIdle get() = pending.get() == 0

    override fun isDispatchNeeded(context: CoroutineContext) = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        pending.incrementAndGet()
        delegate.dispatch(
            context,
            Runnable {
                val previous = current.get()
                current.set(this)
                try {
                    block.run()
                } finally {
                    current.set(previous)
                    pending.decrementAndGet()
                }
            },
        )
    }

    /**
     * Returns once nothing is queued or running on this dispatcher, on two polls in a row: a read that just returned from
     * [parkedWhile] is counted again only once its `finally` runs.
     */
    suspend fun awaitIdle(timeout: Duration = 5.seconds) = awaitAllIdle(listOf(this), timeout)

    fun park() = pending.decrementAndGet()

    fun unpark() = pending.incrementAndGet()

    companion object {
        /**
         * The tracked dispatcher whose block runs on this thread, if any.
         */
        val current = ThreadLocal<IdleTrackingDispatcher?>()
    }
}

/**
 * Runs [block], a blocking wait on something only a test or a fake can end, without counting it as work of the tracked dispatcher
 * running it. The code under test reads the fakes' pipes with blocking calls, so without this a tracked dispatcher reading from a
 * live fake would never go idle.
 */
internal fun <T> parkedWhile(block: () -> T): T {
    val dispatcher = IdleTrackingDispatcher.current.get() ?: return block()
    dispatcher.park()
    try {
        return block()
    } finally {
        dispatcher.unpark()
    }
}

/**
 * This stream, with every read and [InputStream.available] call going through [parkedWhile].
 */
internal fun InputStream.parked(): InputStream = ParkedInputStream(this)

/**
 * Returns once every one of [dispatchers] is idle at the same time, on two polls in a row, since one can hand work to another.
 */
internal suspend fun awaitIdle(
    vararg dispatchers: IdleTrackingDispatcher,
    timeout: Duration = 5.seconds,
) = awaitAllIdle(dispatchers.toList(), timeout)

private suspend fun awaitAllIdle(dispatchers: List<IdleTrackingDispatcher>, timeout: Duration) {
    var idlePolls = 0
    awaitTrue(timeout, "the dispatchers to be idle") {
        if (dispatchers.all { it.isIdle }) idlePolls++ else idlePolls = 0
        idlePolls >= 2
    }
}

private class ParkedInputStream(
    delegate: InputStream,
) : FilterInputStream(delegate) {
    override fun read() = parkedWhile { super.read() }

    override fun read(b: ByteArray, off: Int, len: Int) = parkedWhile { super.read(b, off, len) }

    override fun available() = parkedWhile { super.available() }
}
