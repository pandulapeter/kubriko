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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * An icon in [accent]'s color on a rounded tile of its faint wash, which marks what a header or a hint is about.
 */
@Composable
internal fun AccentIcon(
    icon: DrawableResource,
    accent: Accent,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) = Box(
    modifier = modifier.size(size).background(accent.container, RoundedCornerShape(size * 0.3f)),
    contentAlignment = Alignment.Center,
) {
    Icon(
        modifier = Modifier.size(size * 0.65f),
        painter = painterResource(icon),
        contentDescription = null,
        tint = accent.color,
    )
}
