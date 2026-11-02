/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.window

/**
 * Tells failures that keep coming, more than [maximumFailures] within [windowInMilliseconds], from occasional ones, so
 * that recovering from them can't loop.
 */
internal class RepeatedFailureGuard(
    private val maximumFailures: Int = 3,
    private val windowInMilliseconds: Long = 10_000,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val failureTimes = LongArray(maximumFailures + 1)
    private var count = 0
    private var next = 0

    /**
     * Records a failure, and says whether more than [maximumFailures] happened within the last [windowInMilliseconds].
     */
    fun shouldGiveUp(): Boolean {
        val now = currentTimeMillis()
        failureTimes[next] = now
        next = (next + 1) % failureTimes.size
        if (count < failureTimes.size) count++
        return count == failureTimes.size && now - failureTimes[next] <= windowInMilliseconds
    }
}
