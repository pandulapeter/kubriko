/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelWidthLimitsTest {

    @Test
    fun aWidthBelowTheMinimumIsTheMinimum() = assertEquals(160.dp, clampedPanelWidth(100.dp, availableWidth = 3_000.dp, panelsInRow = 1))

    @Test
    fun aWidthAboveTheMaximumIsTheMaximum() = assertEquals(640.dp, clampedPanelWidth(900.dp, availableWidth = 3_000.dp, panelsInRow = 1))

    @Test
    fun aPanelTakesAtMostHalfTheRow() = assertEquals(500.dp, clampedPanelWidth(600.dp, availableWidth = 1_000.dp, panelsInRow = 1))

    @Test
    fun twoPanelsShareHalfTheRow() = assertEquals(250.dp, clampedPanelWidth(600.dp, availableWidth = 1_000.dp, panelsInRow = 2))

    @Test
    fun aRowTooNarrowForTheMinimumStillGetsIt() = assertEquals(160.dp, clampedPanelWidth(300.dp, availableWidth = 200.dp, panelsInRow = 1))
}
