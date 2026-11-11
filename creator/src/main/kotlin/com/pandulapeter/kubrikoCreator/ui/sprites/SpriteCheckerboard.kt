/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * The gray squares, [square] pixels across, that show through see-through pixels, in [bounds].
 */
internal fun DrawScope.drawCheckerboard(bounds: Rect, square: Float = CHECKERBOARD_SQUARE) {
    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
        drawRect(CHECKERBOARD_LIGHT, bounds.topLeft, bounds.size)
        var y = bounds.top
        var row = 0
        while (y < bounds.bottom) {
            var x = bounds.left + if (row % 2 == 0) 0f else square
            while (x < bounds.right) {
                drawRect(CHECKERBOARD_DARK, Offset(x, y), Size(square, square))
                x += square * 2
            }
            y += square
            row++
        }
    }
}

private const val CHECKERBOARD_SQUARE = 8f
internal val CHECKERBOARD_LIGHT = Color(0xFFCCCCCC)
internal val CHECKERBOARD_DARK = Color(0xFF999999)
