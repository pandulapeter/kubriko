/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import com.pandulapeter.kubriko.types.TargetFrameRate
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameRateHintRequestsTest {

    @Test
    fun noRequestReleasesTheHint() {
        assertEquals(TargetFrameRate.DisplayDefault, combineFrameRateHints(emptyList()))
    }

    @Test
    fun aSingleLimitIsKept() {
        assertEquals(TargetFrameRate.Limit(30), combineFrameRateHints(listOf(TargetFrameRate.Limit(30))))
    }

    @Test
    fun theFastestLimitWins() {
        assertEquals(
            TargetFrameRate.Limit(120),
            combineFrameRateHints(listOf(TargetFrameRate.Limit(30), TargetFrameRate.Limit(120), TargetFrameRate.Limit(60))),
        )
    }

    @Test
    fun aRequestForTheNativeRateReleasesTheHint() {
        assertEquals(
            TargetFrameRate.DisplayDefault,
            combineFrameRateHints(listOf(TargetFrameRate.Limit(30), TargetFrameRate.DisplayDivider(2))),
        )
        assertEquals(
            TargetFrameRate.DisplayDefault,
            combineFrameRateHints(listOf(TargetFrameRate.Limit(30), TargetFrameRate.DisplayDefault)),
        )
    }
}
