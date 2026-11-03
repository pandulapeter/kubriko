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

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Lets this element reach [gutter] past both sides of the padding its parent gives it and fill that width, so a row's
 * background and press feedback span the whole pane while its content (padded by [gutter] again) stays lined up with its
 * neighbours.
 */
public fun Modifier.fullBleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = gutter.roundToPx() * 2
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else Constraints.Infinity
    val placeable = measurable.measure(constraints.copy(minWidth = if (constraints.hasBoundedWidth) width else constraints.minWidth, maxWidth = width))
    layout((placeable.width - extra).coerceIn(constraints.minWidth, constraints.maxWidth), placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}
