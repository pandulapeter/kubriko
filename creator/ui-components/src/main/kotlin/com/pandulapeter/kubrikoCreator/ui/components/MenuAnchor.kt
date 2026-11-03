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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Where a button that opens a [RightClickMenu] under itself is, so [open] can put the menu there.
 */
@Stable
public class MenuAnchor internal constructor(
    private val menu: RightClickMenuState,
) {
    private var coordinates: LayoutCoordinates? = null

    /**
     * Lets this anchor follow where the button is.
     */
    public val modifier: Modifier = Modifier.onGloballyPositioned { coordinates = it }

    /**
     * Opens the menu under the button.
     */
    public fun open() {
        coordinates?.let(menu::openBelow)
    }
}

/**
 * Remembers the [MenuAnchor] that opens [menu].
 */
@Composable
public fun rememberMenuAnchor(menu: RightClickMenuState): MenuAnchor = remember(menu) { MenuAnchor(menu) }
