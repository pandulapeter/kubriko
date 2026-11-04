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

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fades this scrolling content into [color], the surface it is on, at its top or bottom while there is more of it beyond
 * that edge, over about two rows of a menu, so that a list its height cuts off says so even when the cut falls between
 * rows. Goes before the modifier that scrolls it, so that it covers the visible part. It is painted over the content
 * rather than masking it, which every renderer draws alike.
 */
@Composable
public fun Modifier.scrollEdgeFades(state: ScrollableState, color: Color, length: Dp = 48.dp): Modifier {
    val hasMoreAbove = state.canScrollBackward
    val hasMoreBelow = state.canScrollForward
    return drawWithContent {
        drawContent()
        val fade = length.toPx().coerceAtMost(size.height / 3)
        if (hasMoreAbove) {
            drawRect(
                brush = Brush.verticalGradient(0f to color, 1f to color.copy(alpha = 0f), startY = 0f, endY = fade),
                size = Size(size.width, fade),
            )
        }
        if (hasMoreBelow) {
            drawRect(
                brush = Brush.verticalGradient(0f to color.copy(alpha = 0f), 1f to color, startY = size.height - fade, endY = size.height),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
            )
        }
    }
}
