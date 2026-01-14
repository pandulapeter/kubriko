/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KubrikoTheme(
    areResourcesLoaded: Boolean = true,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = !isKubrikoFontLoaded() || !areResourcesLoaded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KubrikoColors.brandPrimary)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
                strokeWidth = 3.dp,
                color = Color.White,
            )
        }
    }
    AnimatedVisibility(
        visible = isKubrikoFontLoaded() && areResourcesLoaded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
            typography = KubrikoTypography(),
            content = content,
        )
    }
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