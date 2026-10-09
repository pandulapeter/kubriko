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

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class AngleDegreesExtensionsTest {

    private val samples = listOf(-720f, -90f, 0f, 45f, 90f, 180f, 359f, 400f, 720f)

    @Test
    fun radConvertsWithoutWrapping() {
        assertEquals((-PI / 2).toFloat(), (-90f).deg.rad.raw, absoluteTolerance = 1e-5f)
        assertEquals((PI / 2).toFloat(), 90f.deg.rad.raw, absoluteTolerance = 1e-5f)
        assertEquals((4 * PI).toFloat(), 720f.deg.rad.raw, absoluteTolerance = 1e-5f)
    }

    @Test
    fun degreesSurviveARoundTripThroughRadians() {
        for (degrees in samples) {
            assertEquals(degrees, degrees.deg.rad.deg.raw, absoluteTolerance = 1e-3f, "round trip of $degrees°")
        }
    }

    @Test
    fun sineAndCosineTreatTheValueAsDegrees() {
        for (degrees in samples) {
            val radians = (degrees * PI / 180).toFloat()
            assertEquals(kotlin.math.sin(radians), degrees.deg.sin, absoluteTolerance = 1e-5f, "sin of $degrees°")
            assertEquals(kotlin.math.cos(radians), degrees.deg.cos, absoluteTolerance = 1e-5f, "cos of $degrees°")
        }
    }
}
