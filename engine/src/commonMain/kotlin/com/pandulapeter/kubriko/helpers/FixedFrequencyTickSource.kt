/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

internal class FixedFrequencyTickSource(
    ticksPerSecond: Int,
) : TickSource() {
    init {
        require(ticksPerSecond > 0) { "ticksPerSecond must be greater than 0." }
    }

    private val targetInterval = (1_000_000_000L / ticksPerSecond).nanoseconds
    private var job: Job? = null

    private val loopMutex = Mutex()

    override fun onStart() {
        job = scope.launch {
            loopMutex.withLock {
                emitTick(0)
                var lastTickTime = TimeSource.Monotonic.markNow()
                var nextTickStart = lastTickTime
                while (isActive) {
                    val remainingTime = targetInterval - nextTickStart.elapsedNow()
                    if (remainingTime.isPositive()) {
                        delay(remainingTime.inWholeMilliseconds.coerceAtLeast(1L))
                    }
                    val currentTime = TimeSource.Monotonic.markNow()
                    emitTick(lastTickTime.elapsedNow().inWholeMilliseconds.toInt())
                    lastTickTime = currentTime
                    nextTickStart += targetInterval
                    if ((targetInterval - nextTickStart.elapsedNow()).isNegative()) {
                        nextTickStart = currentTime
                    }
                }
            }
        }
    }

    override fun onStop() {
        job?.cancel()
        job = null
    }
}
