/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AngleRadiansExtensionsTest {

    private val samples = listOf(-12.5f, -PI.toFloat(), -1f, 0f, 0.5f, PI.toFloat(), 4f, 20f)

    @Test
    fun degConvertsWithoutWrapping() {
        assertEquals(-90f, (-PI / 2).toFloat().rad.deg.raw, absoluteTolerance = 1e-3f)
        assertEquals(720f, (4 * PI).toFloat().rad.deg.raw, absoluteTolerance = 1e-3f)
    }

    @Test
    fun sineAndCosineMatchKotlinMathForAnyTurn() {
        for (radians in samples) {
            assertEquals(kotlin.math.sin(radians), radians.rad.sin, absoluteTolerance = 1e-5f, "sin of $radians")
            assertEquals(kotlin.math.cos(radians), radians.rad.cos, absoluteTolerance = 1e-5f, "cos of $radians")
        }
    }

    @Test
    fun shortestDeltaTakesTheShortWayAcrossTheWrap() {
        assertEquals(20f, 350f.deg.rad.shortestDeltaTo(10f.deg.rad).deg.raw, absoluteTolerance = 1e-3f)
        assertEquals(-20f, 10f.deg.rad.shortestDeltaTo(350f.deg.rad).deg.raw, absoluteTolerance = 1e-3f)
        assertEquals(30f, (-720f).deg.rad.shortestDeltaTo(390f.deg.rad).deg.raw, absoluteTolerance = 1e-3f)
    }

    @Test
    fun shortestDeltaStaysWithinHalfATurn() {
        for (from in samples) {
            for (to in samples) {
                val delta = from.rad.shortestDeltaTo(to.rad).raw
                assertTrue(delta > -AngleRadians.Pi.raw - 1e-5f && delta <= AngleRadians.Pi.raw + 1e-5f, "delta from $from to $to: $delta")
                assertEquals(kotlin.math.sin(to), kotlin.math.sin(from + delta), absoluteTolerance = 1e-4f, "delta from $from to $to lands on $to")
                assertEquals(kotlin.math.cos(to), kotlin.math.cos(from + delta), absoluteTolerance = 1e-4f, "delta from $from to $to lands on $to")
            }
        }
    }

    @Test
    fun rotateTowardsStepsByAtMostTheMaximumDelta() {
        val rotated = 0f.deg.rad.rotateTowards(target = 90f.deg.rad, maxDelta = 10f.deg.rad)

        assertEquals(10f, rotated.deg.raw, absoluteTolerance = 1e-3f)
    }

    @Test
    fun rotateTowardsTurnsTheShortWay() {
        val rotated = 10f.deg.rad.rotateTowards(target = 300f.deg.rad, maxDelta = 15f.deg.rad)

        assertEquals(-5f, rotated.deg.raw, absoluteTolerance = 1e-3f)
    }

    @Test
    fun rotateTowardsLandsExactlyOnANearbyTarget() {
        val target = 95f.deg.rad

        assertEquals(target, 90f.deg.rad.rotateTowards(target = target, maxDelta = 10f.deg.rad))
    }
}
