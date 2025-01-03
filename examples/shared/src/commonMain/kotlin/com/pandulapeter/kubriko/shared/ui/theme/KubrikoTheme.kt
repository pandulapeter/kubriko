package com.pandulapeter.kubriko.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun KubrikoTheme(
    content: @Composable () -> Unit
) = MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
    typography = KubrikoTypography(),
    content = content
)

private val lightScheme = lightColorScheme(
    primary = KubrikoColors.brandDark,
    onPrimary = KubrikoColors.onBrandDark,
    primaryContainer =KubrikoColors.brandDark,
    onPrimaryContainer = KubrikoColors.onBrandDark,
    secondary = KubrikoColors.brandDark,
)

private val darkScheme = darkColorScheme(
    primary = KubrikoColors.brandDark,
    onPrimary = KubrikoColors.onBrandDark,
    primaryContainer =KubrikoColors.brandDark,
    onPrimaryContainer = KubrikoColors.onBrandDark,
    secondary = KubrikoColors.brandLight,
)