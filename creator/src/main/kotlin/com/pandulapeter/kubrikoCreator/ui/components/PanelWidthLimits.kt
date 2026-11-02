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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * [width] kept between [MINIMUM_PANEL_WIDTH] and [MAXIMUM_PANEL_WIDTH], and within its share of half the [availableWidth],
 * which the [panelsInRow] side panels shown beside the editor split between them, so that together they never leave
 * the editor less than half the row (the minimum wins on a row too narrow even for that).
 */
internal fun clampedPanelWidth(width: Dp, availableWidth: Dp, panelsInRow: Int): Dp = width.coerceIn(
    MINIMUM_PANEL_WIDTH,
    maxOf(MINIMUM_PANEL_WIDTH, minOf(MAXIMUM_PANEL_WIDTH, availableWidth * 0.5f / panelsInRow.coerceAtLeast(1))),
)

private val MINIMUM_PANEL_WIDTH = 160.dp
private val MAXIMUM_PANEL_WIDTH = 640.dp
