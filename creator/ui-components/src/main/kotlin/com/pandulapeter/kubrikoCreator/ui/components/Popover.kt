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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * A small floating surface (in [MenuStyle]) holding [content], open while [state] is: where its element was right-clicked,
 * or under the button that opened it. It is composed only while open and placed in the window, not by the layout it is
 * declared in, so it takes no room there.
 */
@Composable
public fun Popover(
    state: RightClickMenuState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val position = state.positionInWindow ?: return
    Popup(
        popupPositionProvider = remember(position) { AtPosition(position) },
        onDismissRequest = state::close,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = modifier.dialogEntrance(),
            shape = MenuStyle.shape,
            color = MenuStyle.color,
            border = MenuStyle.border,
            shadowElevation = MenuStyle.elevation,
        ) {
            Column(content = content)
        }
    }
}

/**
 * Puts the popover's corner at [position], moved back inside the window when it would leave it.
 */
private class AtPosition(
    private val position: Offset,
) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = position.x.roundToInt().let { if (it + popupContentSize.width > windowSize.width) it - popupContentSize.width else it }
        val y = position.y.roundToInt().let { if (it + popupContentSize.height > windowSize.height) it - popupContentSize.height else it }
        return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
    }
}
