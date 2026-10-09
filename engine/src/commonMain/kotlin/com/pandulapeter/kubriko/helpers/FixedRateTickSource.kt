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

internal class FixedRateTickSource(
    private val intervalInMilliseconds: Long,
) : TickSource() {
    private var job: Job? = null

    init {
        require(intervalInMilliseconds > 0L) { "intervalInMilliseconds must be greater than 0." }
    }

    private val loopMutex = Mutex()

    override fun onStart() {
        job = scope.launch {
            loopMutex.withLock {
                emitTick(0)
                while (isActive) {
                    delay(intervalInMilliseconds)
                    emitTick(intervalInMilliseconds.toInt())
                }
            }
        }
    }

    override fun onStop() {
        job?.cancel()
        job = null
    }
}
