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

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A compact icon-only button, named by a tooltip and for screen readers by the same [label].
 */
@Composable
internal fun IconActionButton(
    icon: DrawableResource,
    label: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) = IconActionButton(
    icon = icon,
    label = stringResource(label),
    onClick = onClick,
    modifier = modifier,
    isEnabled = isEnabled,
    tint = tint,
)

/**
 * An [IconActionButton] whose [label] is put together at the call site.
 */
@Composable
internal fun IconActionButton(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) = WithTooltip(
    label = label,
    modifier = modifier,
) {
    IconButton(
        modifier = Modifier.size(32.dp),
        enabled = isEnabled,
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (isEnabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}
