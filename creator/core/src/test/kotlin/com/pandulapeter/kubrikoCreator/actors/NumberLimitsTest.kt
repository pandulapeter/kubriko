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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberLimitsTest {

    @Test
    fun aFixedNumberAndBothEndsOfARandomOneMustBeWithin() {
        assertTrue(NumberLimits.ZOOM.accept(NumberValue.Fixed(1f)))
        assertFalse(NumberLimits.ZOOM.accept(NumberValue.Fixed(0f)))
        assertTrue(NumberLimits.ZOOM.accept(NumberValue.Random(80f, 120f)))
        assertFalse(NumberLimits.ZOOM.accept(NumberValue.Random(0f, 120f)))
        assertFalse(NumberLimits.PERCENT.accept(NumberValue.Random(50f, 101f)))
        assertTrue(NumberLimits.ANY.accept(NumberValue.Fixed(-1000f)))
    }

    @Test
    fun whatIsOnlyKnownWhileTheGameRunsIsAccepted() {
        assertTrue(NumberLimits.ZOOM.accept(NumberValue.Property("zoom")))
        assertTrue(NumberLimits.NON_NEGATIVE.accept(NumberValue.Edge(SceneEdge.LEFT, -50f)))
        assertTrue(NumberLimits.NON_NEGATIVE.accept(NumberValue.BetweenEdges()))
    }

    @Test
    fun clampingGivesTheClosestNumberTaken() {
        assertEquals(1f, NumberLimits.ZOOM.clamp(0f))
        assertEquals(100f, NumberLimits.VOLUME.clamp(250f))
        assertEquals(42f, NumberLimits.PERCENT.clamp(42f))
        assertEquals(-5f, NumberLimits.ANY.clamp(-5f))
    }
}
