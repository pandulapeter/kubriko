/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

/**
 * Kubriko's theme with the Creator's own colors, [CreatorTypography], and desktop-sized corners, light or dark as [theme]
 * says ([ThemeChoice.SYSTEM] following the operating system as Kubriko's theme does). Controls take only the room they
 * draw, without Material's 48dp touch targets, which a desktop app driven by a mouse doesn't need and which left a
 * control's height of empty space around every button, chip, and switch.
 */
@Composable
public fun CreatorTheme(
    theme: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
): Unit = KubrikoTheme {
    val isDark = when (theme) {
        ThemeChoice.SYSTEM -> MaterialTheme.colorScheme.surface.luminance() < 0.5f
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (isDark) CreatorDarkColors else CreatorLightColors,
        typography = CreatorTypography(),
        shapes = CreatorShapes,
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
            content = content,
        )
    }
}

/**
 * Corners that suit a desktop window: menus, fields, and cards barely rounded, dialogs only slightly more.
 */
private val CreatorShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
