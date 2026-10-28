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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the gray checkerboard that shows through translucent colors drawn on top.
 */
internal fun Modifier.checkerboard(squareSize: Dp = 6.dp) = drawBehind {
    val square = squareSize.toPx()
    drawRect(CHECKERBOARD_LIGHT)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else square
        while (x < size.width) {
            drawRect(CHECKERBOARD_DARK, topLeft = Offset(x, y), size = Size(minOf(square, size.width - x), minOf(square, size.height - y)))
            x += square * 2
        }
        y += square
        row++
    }
}

private val CHECKERBOARD_LIGHT = Color(0xFFCCCCCC)
private val CHECKERBOARD_DARK = Color(0xFF999999)
