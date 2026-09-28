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

    @Test
    fun radDoesNotWrap() {
        assertEquals((-PI / 2).toFloat(), (-90f).deg.rad.raw, absoluteTolerance = 1e-5f)
        assertEquals((4 * PI).toFloat(), 720f.deg.rad.raw, absoluteTolerance = 1e-5f)
        assertEquals((PI / 2).toFloat(), 90f.deg.rad.raw, absoluteTolerance = 1e-5f)
    }

    @Test
    fun roundTrip() {
        for (x in listOf(-720f, -90f, 0f, 45f, 400f)) {
            assertEquals(x, x.deg.rad.deg.raw, absoluteTolerance = 1e-3f)
        }
    }

    @Test
    fun sineAndCosineUseRadians() {
        assertEquals(1f, 90f.deg.sin, absoluteTolerance = 1e-5f)
        assertEquals(1f, 0f.deg.cos, absoluteTolerance = 1e-5f)
        assertEquals(-1f, 180f.deg.cos, absoluteTolerance = 1e-5f)
        assertEquals(-1f, (-90f).deg.sin, absoluteTolerance = 1e-5f)
    }
}
