/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput.implementation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrollZoomFactorTest {

    @Test
    fun factorIsPositiveAndFiniteForLargeDeltas() {
        for (factor in listOf(scrollZoomFactor(10_000f, 0.05f), scrollZoomFactor(-10_000f, 0.05f))) {
            assertTrue(factor > 0f && factor.isFinite(), "$factor")
        }
        assertTrue(scrollZoomFactor(200f, 0.005f) > 0f)
    }

    @Test
    fun largeOppositeDeltasStillCancel() {
        assertEquals(1f, scrollZoomFactor(10_000f, 0.05f) * scrollZoomFactor(-10_000f, 0.05f), 1e-3f)
    }

    @Test
    fun oppositeDeltasCancel() {
        for (delta in listOf(0.5f, 1f, 3f, 100f)) {
            for (sensitivity in listOf(0.05f, 0.005f)) {
                assertEquals(1f, scrollZoomFactor(delta, sensitivity) * scrollZoomFactor(-delta, sensitivity), 1e-5f)
            }
        }
    }

    @Test
    fun scrollingDownZoomsOut() {
        assertTrue(scrollZoomFactor(1f, 0.05f) < 1f)
        assertTrue(scrollZoomFactor(-1f, 0.05f) > 1f)
    }

    @Test
    fun zeroAndNonFiniteDeltasDoNotZoom() {
        for (delta in listOf(0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(1f, scrollZoomFactor(delta, 0.05f))
        }
    }
}
