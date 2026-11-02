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

class RepeatedFailureGuardTest {
    private var now = 0L
    private val guard = RepeatedFailureGuard(currentTimeMillis = { now })

    @Test
    fun threeFailuresWithinTenSecondsNeverGiveUp() {
        assertEquals(listOf(false, false, false), (1..3).map { now += 1_000; guard.shouldGiveUp() })
    }

    @Test
    fun theFourthFailureWithinTenSecondsGivesUp() {
        repeat(3) { now += 1_000; guard.shouldGiveUp() }
        now += 1_000
        assertEquals(true, guard.shouldGiveUp())
    }

    @Test
    fun fourFailuresSpreadOverMoreThanTenSecondsDoNotGiveUp() {
        assertEquals(listOf(false, false, false, false, false), (1..5).map { now += 3_500; guard.shouldGiveUp() })
    }
}
