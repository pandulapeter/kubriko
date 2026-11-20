/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneGridTest {
    private val grid = SceneGrid(size = 16f, isSnappingByDefault = true)

    @Test
    fun valuesGoToTheNearestLine() {
        assertEquals(16f, grid.snap(9f))
        assertEquals(0f, grid.snap(7f))
        assertEquals(-32f, grid.snap(-25f))
        assertEquals(Offset(48f, -16f), grid.snap(Offset(50f, -10f)))
    }

    @Test
    fun aViewZoomedFarOutDrawsEverySecondFourthAndSoOnLine() {
        assertEquals(16f, grid.drawnSpacing(pixelsPerUnit = 1f, minimumSpacingInPixels = 8f))
        assertEquals(32f, grid.drawnSpacing(pixelsPerUnit = 0.4f, minimumSpacingInPixels = 8f))
        assertEquals(128f, grid.drawnSpacing(pixelsPerUnit = 0.1f, minimumSpacingInPixels = 8f))
    }
}
