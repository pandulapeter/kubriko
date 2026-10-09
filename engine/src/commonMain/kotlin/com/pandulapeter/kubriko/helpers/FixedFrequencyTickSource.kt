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

internal class FixedFrequencyTickSource(
    ticksPerSecond: Int,
    private val clock: TickClock = TickClock.Monotonic,
) : TickSource() {
    init {
        require(ticksPerSecond > 0) { "ticksPerSecond must be greater than 0." }
    }

    private val targetIntervalInNanoseconds = 1_000_000_000L / ticksPerSecond
    private var job: Job? = null

    private val loopMutex = Mutex()

    override fun onStart() {
        job = scope.launch {
            loopMutex.withLock {
                emitTick(0)
                var lastTickTime = clock.nowInNanoseconds()
                var nextTickStart = lastTickTime
                while (isActive) {
                    val remainingTime = targetIntervalInNanoseconds - (clock.nowInNanoseconds() - nextTickStart)
                    if (remainingTime > 0L) {
                        delay((remainingTime / NANOSECONDS_PER_MILLISECOND).coerceAtLeast(1L))
                    }
                    val currentTime = clock.nowInNanoseconds()
                    val elapsedInNanoseconds = currentTime - lastTickTime
                    if (elapsedInNanoseconds > targetIntervalInNanoseconds + MAXIMUM_TICK_GAP_IN_NANOSECONDS) {
                        // The process was suspended: restart the timeline like the first tick does.
                        emitTick(0)
                        nextTickStart = currentTime
                    } else {
                        emitTick((elapsedInNanoseconds / NANOSECONDS_PER_MILLISECOND).toInt())
                        nextTickStart += targetIntervalInNanoseconds
                        if (targetIntervalInNanoseconds - (clock.nowInNanoseconds() - nextTickStart) < 0L) {
                            nextTickStart = currentTime
                        }
                    }
                    lastTickTime = currentTime
                }
            }
        }
    }

    override fun onStop() {
        job?.cancel()
        job = null
    }
}

private const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
private const val MAXIMUM_TICK_GAP_IN_NANOSECONDS = 2_000_000_000L
