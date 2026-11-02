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

import kotlin.test.Test
import kotlin.test.assertEquals

class CreatorWindowExceptionHandlerTest {
    private val reported = mutableListOf<Throwable>()
    private val fallenBack = mutableListOf<Throwable>()
    private var recoveries = 0
    private val handler = CreatorWindowExceptionHandler(
        report = { reported += it },
        onRecover = { recoveries++ },
        guard = RepeatedFailureGuard(currentTimeMillis = { 0L }),
        fallback = { _, throwable -> fallenBack += throwable },
    )

    @Test
    fun failuresAreReportedAndRebuiltUntilTheyKeepComing() {
        val failures = (1..4).map<Int, Throwable> { IllegalStateException("Failure $it.") }
        failures.forEach { handler.handle(null, it) }
        assertEquals(failures.take(3), reported)
        assertEquals(3, recoveries)
        assertEquals(listOf(failures.last()), fallenBack)
    }
}
