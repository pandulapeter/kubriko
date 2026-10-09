/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ShaderTimeTest {

    private fun sixteenMillisecondStepAt(timeInMilliseconds: Long) =
        shaderTimeInSeconds(timeInMilliseconds + 16) - shaderTimeInSeconds(timeInMilliseconds)

    @Test
    fun noJumpAtTheOldHundredSecondWrap() {
        assertEquals(100f, shaderTimeInSeconds(100_000L))
        assertEquals(0.016f, sixteenMillisecondStepAt(100_000L), 1e-4f)
    }

    @Test
    fun wrapsOnceAnHour() {
        assertEquals(0f, shaderTimeInSeconds(3_600_000L))
        assertEquals(0.016f, shaderTimeInSeconds(3_600_016L), 1e-4f)
    }

    @Test
    fun sixteenMillisecondStepsStayDistinctUpToTheWrap() {
        assertEquals(0.016f, sixteenMillisecondStepAt(3_599_968L), 1e-3f)
    }

    @Test
    fun staysContinuousAfterDaysOfRuntime() {
        assertEquals(0.016f, sixteenMillisecondStepAt(300L * 3_600_000L + 1_234_567L), 1e-3f)
    }
}
