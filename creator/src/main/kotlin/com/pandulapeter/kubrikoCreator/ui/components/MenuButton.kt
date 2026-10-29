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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * An outlined button showing the chosen option, with a chevron, that opens a menu of [options].
 */
@Composable
internal fun <T> MenuButton(
    label: String,
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) = Box {
    var isExpanded by remember { mutableStateOf(false) }
    MenuButtonFace(
        label = label,
        onClick = { isExpanded = true },
    )
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(optionText(option)) },
                onClick = {
                    isExpanded = false
                    onSelected(option)
                },
            )
        }
    }
}
