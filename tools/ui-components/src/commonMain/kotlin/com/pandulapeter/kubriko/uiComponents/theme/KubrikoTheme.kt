/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun KubrikoTheme(
    content: @Composable () -> Unit
) = AnimatedVisibility(
    visible = isKubrikoFontLoaded(),
    enter = fadeIn(),
    exit = fadeOut(),
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
        typography = KubrikoTypography(),
        content = content,
    )
}

private val lightScheme = lightColorScheme(
    primary = KubrikoColors.brandPrimary,
    onPrimary = KubrikoColors.onBrandPrimary,
    primaryContainer = KubrikoColors.brandPrimary,
    onPrimaryContainer = KubrikoColors.onBrandPrimary,
    secondary = KubrikoColors.brandPrimary,
)

private val darkScheme = darkColorScheme(
    primary = KubrikoColors.brandPrimary,
    onPrimary = KubrikoColors.onBrandPrimary,
    primaryContainer = KubrikoColors.brandPrimary,
    onPrimaryContainer = KubrikoColors.onBrandPrimary,
    secondary = KubrikoColors.brandSecondary,
)