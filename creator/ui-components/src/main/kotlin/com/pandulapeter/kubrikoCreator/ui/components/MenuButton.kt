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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable

/**
 * A button showing [label] that opens a menu of [options], each shown by [optionText], calling [onSelected] with the one
 * chosen. The option shown by [label] is marked as the current one.
 */
@Composable
public fun <T> MenuButton(
    label: String,
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
): Unit = Box {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    Box(anchor.modifier) {
        MenuButtonFace(
            label = label,
            onClick = anchor::open,
        )
    }
    RightClickMenu(
        state = menu,
        actions = options.map { option ->
            val text = optionText(option)
            ContextAction(label = text, isCurrent = text == label) { onSelected(option) }
        },
    )
}
