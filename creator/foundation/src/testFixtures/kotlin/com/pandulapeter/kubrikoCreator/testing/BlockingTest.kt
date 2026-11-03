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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs [block] in [context], failing once [timeout] passes, for tests whose code runs on real threads and real pipes.
 * Tests on virtual time use `runTest`.
 */
public fun blockingTest(
    timeout: Duration = 10.seconds,
    context: CoroutineContext = Dispatchers.IO,
    block: suspend CoroutineScope.() -> Unit,
): Unit = runBlocking(context) { withTimeout(timeout) { block() } }
