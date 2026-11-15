/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberLimitsTest {

    @Test
    fun aFixedNumberAndBothEndsOfARandomOneMustBeWithin() {
        assertTrue(NumberValue.Fixed(1f) in NumberLimits.ZOOM)
        assertFalse(NumberValue.Fixed(0f) in NumberLimits.ZOOM)
        assertTrue(NumberValue.Random(80f, 120f) in NumberLimits.ZOOM)
        assertFalse(NumberValue.Random(0f, 120f) in NumberLimits.ZOOM)
        assertFalse(NumberValue.Random(50f, 101f) in NumberLimits.PERCENT)
        assertTrue(NumberValue.Fixed(-1000f) in NumberLimits.ANY)
    }

    @Test
    fun whatIsOnlyKnownWhileTheGameRunsIsAccepted() {
        assertTrue(NumberValue.Property("zoom") in NumberLimits.ZOOM)
        assertTrue(NumberValue.Edge(SceneEdge.LEFT, -50f) in NumberLimits.NON_NEGATIVE)
        assertTrue(NumberValue.BetweenEdges() in NumberLimits.NON_NEGATIVE)
    }

    @Test
    fun aMinimumAboveTheMaximumIsRefusedWhenBuilt() {
        assertFailsWith<IllegalArgumentException> { NumberLimits(minimum = 10f, maximum = 0f) }
    }

    @Test
    fun clampingGivesTheClosestNumberTaken() {
        assertEquals(1f, NumberLimits.ZOOM.clamp(0f))
        assertEquals(100f, NumberLimits.VOLUME.clamp(250f))
        assertEquals(42f, NumberLimits.PERCENT.clamp(42f))
        assertEquals(-5f, NumberLimits.ANY.clamp(-5f))
    }
}
