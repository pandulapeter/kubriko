/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Runs [block], ignoring its failure, and a cancellation that isn't the caller's own (the ACP SDK fails a request with one
 * of its own when the agent stops).
 */
internal suspend fun runIgnoringFailure(block: suspend () -> Unit) = try {
    block()
} catch (_: CancellationException) {
    currentCoroutineContext().ensureActive()
} catch (_: Exception) {
}
