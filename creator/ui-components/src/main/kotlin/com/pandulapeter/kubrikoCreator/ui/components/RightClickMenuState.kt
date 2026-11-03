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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Where in the window a [RightClickMenu] was opened (by a right-click, or by a button through [openBelow]), or null while
 * it is closed.
 */
@Stable
public class RightClickMenuState {
    internal var positionInWindow by mutableStateOf<Offset?>(null)
    internal var coordinates: LayoutCoordinates? = null

    /**
     * Whether the menu is open.
     */
    public val isOpen: Boolean get() = positionInWindow != null

    /**
     * Opens the menu under the element [coordinates] belongs to, at its start, as a button's menu opens.
     */
    public fun openBelow(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) positionInWindow = coordinates.localToWindow(Offset(0f, coordinates.size.height.toFloat()))
    }

    internal fun close() {
        positionInWindow = null
    }
}

/**
 * Remembers a closed [RightClickMenuState].
 */
@Composable
public fun rememberRightClickMenuState(): RightClickMenuState = remember { RightClickMenuState() }

/**
 * Opens [state]'s [RightClickMenu] where this element is clicked with the secondary mouse button, unless something inside
 * it (a text field, with its own copy and paste menu) already took that click.
 */
public fun Modifier.rightClickMenu(state: RightClickMenuState): Modifier = onGloballyPositioned { state.coordinates = it }
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    val change = event.changes.firstOrNull()?.takeUnless { it.isConsumed } ?: continue
                    val coordinates = state.coordinates?.takeIf { it.isAttached } ?: continue
                    state.positionInWindow = coordinates.localToWindow(change.position)
                    change.consume()
                }
            }
        }
    }
