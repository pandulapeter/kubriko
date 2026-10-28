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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A strip of [color] along the start edge, like the colored side of a puzzle piece, which tells what category a card is
 * in before its text is read.
 */
internal fun Modifier.accentEdge(color: Color) = drawBehind {
    drawRect(color = color, size = Size(ACCENT_EDGE_WIDTH.toPx(), size.height))
}

private val ACCENT_EDGE_WIDTH = 4.dp
