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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoColors
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

/**
 * Kubriko's theme with the Creator's own [CreatorTypography] in place of the shared one, light or dark as [theme] says.
 */
@Composable
public fun CreatorTheme(
    theme: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
): Unit = KubrikoTheme {
    MaterialTheme(
        colorScheme = when (theme) {
            ThemeChoice.SYSTEM -> MaterialTheme.colorScheme
            ThemeChoice.LIGHT -> LightScheme
            ThemeChoice.DARK -> DarkScheme
        },
        typography = CreatorTypography(),
        content = content,
    )
}

/**
 * The same colors [KubrikoTheme] picks from the system's setting.
 */
private val LightScheme = lightColorScheme(
    primary = KubrikoColors.brandPrimary,
    onPrimary = KubrikoColors.onBrandPrimary,
    primaryContainer = KubrikoColors.brandPrimary,
    onPrimaryContainer = KubrikoColors.onBrandPrimary,
    secondary = KubrikoColors.brandPrimary,
)

private val DarkScheme = darkColorScheme(
    primary = KubrikoColors.brandPrimary,
    onPrimary = KubrikoColors.onBrandPrimary,
    primaryContainer = KubrikoColors.brandPrimary,
    onPrimaryContainer = KubrikoColors.onBrandPrimary,
    secondary = KubrikoColors.brandSecondary,
)
