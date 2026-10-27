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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A compact icon-only button, named by a tooltip and for screen readers by the same [label].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IconActionButton(
    icon: DrawableResource,
    label: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) = TooltipArea(
    modifier = modifier,
    tooltip = { Tooltip(stringResource(label)) },
    delayMillis = 500,
    tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
) {
    IconButton(
        modifier = Modifier.size(32.dp),
        enabled = isEnabled,
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = if (isEnabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun Tooltip(
    text: String,
) = Surface(
    color = MaterialTheme.colorScheme.inverseSurface,
    shape = MaterialTheme.shapes.small,
) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.inverseOnSurface,
    )
}
