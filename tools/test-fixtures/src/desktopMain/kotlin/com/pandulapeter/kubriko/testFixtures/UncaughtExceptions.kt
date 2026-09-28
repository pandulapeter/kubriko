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

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Runs [block] with a default uncaught-exception handler that records every exception into the list passed to it,
 * then restores the previous handler.
 *
 * Do not use it inside `runTest`: `kotlinx-coroutines-test` captures coroutine exceptions before the default
 * handler sees them.
 */
fun <T> recordingUncaughtExceptions(block: (recorded: List<Throwable>) -> T): T {
    val recorded = CopyOnWriteArrayList<Throwable>()
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> recorded.add(throwable) }
    try {
        return block(recorded)
    } finally {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }
}
