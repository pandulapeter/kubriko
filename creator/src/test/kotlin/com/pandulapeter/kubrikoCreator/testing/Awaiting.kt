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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Waits until [condition] holds, polling it every 10 ms: the replacement for a fixed sleep before an assertion.
 *
 * It waits in real time even when called from `runTest`, whose virtual clock would otherwise run the whole [timeout] out in a few
 * milliseconds.
 *
 * @param description What is waited for, named in the failure's message.
 * @throws AssertionError When [condition] still doesn't hold after [timeout]. Not a cancellation, so that a wait in a launched
 * coroutine fails its test instead of only ending that coroutine.
 */
internal suspend fun awaitTrue(
    timeout: Duration = 5.seconds,
    description: String? = null,
    condition: () -> Boolean,
) {
    val isMet = withContext(Dispatchers.Default) {
        withTimeoutOrNull(timeout) { while (!condition()) delay(10) } != null
    }
    if (!isMet) throw AssertionError("Waited $timeout for ${description ?: "a condition"} in vain.")
}
