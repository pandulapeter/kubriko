/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testFixtures

import kotlin.test.fail

/**
 * Polls [condition] every 2 ms and fails the test if it does not hold within [timeoutInMilliseconds].
 *
 * The fallback for asynchronous work that has no deterministic signal: prefer [awaitProcessed] or
 * [ManualKubriko.tickUntil].
 */
fun awaitCondition(
    timeoutInMilliseconds: Long = 5_000,
    condition: () -> Boolean,
) {
    val startTime = System.currentTimeMillis()
    while (!condition()) {
        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime > timeoutInMilliseconds) fail("Condition not met after $elapsedTime ms.")
        Thread.sleep(2)
    }
}
