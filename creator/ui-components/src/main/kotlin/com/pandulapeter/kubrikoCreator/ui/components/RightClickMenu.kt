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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/**
 * The menu of [actions] that opens right where the element with [rightClickMenu] of [state] was right-clicked, so a row
 * can keep what is done to it out of sight until it is asked for. It is composed only while open, and placed by the
 * pointer, not by the layout it is in.
 */
@Composable
public fun RightClickMenu(
    state: RightClickMenuState,
    actions: List<ContextAction>,
) {
    val position = state.positionInWindow ?: return
    if (actions.isEmpty()) return
    Popup(
        popupPositionProvider = remember(position) { AtPointer(position) },
        onDismissRequest = state::close,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 160.dp, max = 320.dp).dialogEntrance(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                actions.forEach { action ->
                    val color = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    DropdownMenuItem(
                        text = { Text(text = action.label, color = color) },
                        leadingIcon = action.icon?.let { icon ->
                            { Icon(modifier = Modifier.size(16.dp), painter = painterResource(icon), contentDescription = null, tint = color) }
                        },
                        onClick = {
                            state.close()
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Puts the menu's corner at [position], moved back inside the window when it would leave it.
 */
private class AtPointer(
    private val position: Offset,
) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = position.x.roundToInt().let { if (it + popupContentSize.width > windowSize.width) it - popupContentSize.width else it }
        val y = position.y.roundToInt().let { if (it + popupContentSize.height > windowSize.height) it - popupContentSize.height else it }
        return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
    }
}
