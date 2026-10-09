/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridLinesTest {

    @Test
    fun lineIndicesAreFloored() {
        assertEquals(-1L, firstGridLineIndex(-10.5f, 32f))
        assertEquals(3L, lastGridLineIndex(100f, 32f))
        assertEquals(2L, firstGridLineIndex(64f, 32f))
    }

    @Test
    fun lineCountStaysFiniteBeyondFloatPrecision() {
        val count = lastGridLineIndex(33554500f, 1f) - firstGridLineIndex(33554432f, 1f)
        assertTrue(count in 0L..MAX_GRID_LINES_PER_AXIS)
    }

    @Test
    fun linesAreVisibleFromTheMinimumOnScreenSpacing() {
        assertTrue(isGridLineVisible(MIN_GRID_LINE_SPACING_PX, 1f))
        assertTrue(isGridLineVisible(MIN_GRID_LINE_SPACING_PX * 10, 0.1f))
        assertFalse(isGridLineVisible(MIN_GRID_LINE_SPACING_PX * 0.9f, 1f))
        assertFalse(isGridLineVisible(MIN_GRID_LINE_SPACING_PX * 5, 0.1f))
    }

    @Test
    fun denseGridsFallBackToMajorsAndThenToNothing() {
        assertEquals(1L, gridLineStep(MIN_GRID_LINE_SPACING_PX, 1f))
        assertEquals(10L, gridLineStep(MIN_GRID_LINE_SPACING_PX / 5, 1f))
        assertEquals(0L, gridLineStep(MIN_GRID_LINE_SPACING_PX / 50, 1f))
    }

    @Test
    fun majorOnlyLinesStartOnAMultipleOfTen() {
        assertEquals(-10L, alignGridLineIndex(-13L, 10L))
        assertEquals(20L, alignGridLineIndex(11L, 10L))
        assertEquals(20L, alignGridLineIndex(20L, 10L))
        assertEquals(7L, alignGridLineIndex(7L, 1L))
    }
}
