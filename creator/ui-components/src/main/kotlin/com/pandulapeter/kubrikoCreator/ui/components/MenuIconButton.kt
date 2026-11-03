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
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * An icon button that opens a menu of [options].
 */
@Composable
public fun <T> MenuIconButton(
    icon: DrawableResource,
    label: StringResource,
    options: List<T>,
    optionText: @Composable (T) -> String,
    optionIcon: (T) -> DrawableResource?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
): Unit = Box(modifier) {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    IconActionButton(
        modifier = anchor.modifier,
        icon = icon,
        label = label,
        onClick = anchor::open,
    )
    RightClickMenu(
        state = menu,
        actions = options.map { option -> ContextAction(label = optionText(option), icon = optionIcon(option)) { onSelected(option) } },
    )
}
