package com.pandulapeter.kubriko.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun KubrikoTheme(
    content: @Composable() () -> Unit
) = MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
    typography = KubrikoTypography(),
    content = content
)

private val lightScheme = lightColorScheme(
    primary = KubrikoColors.primaryLight,
    onPrimary = KubrikoColors.onPrimaryLight,
    primaryContainer = KubrikoColors.primaryContainerLight,
    onPrimaryContainer = KubrikoColors.onPrimaryContainerLight,
    secondary = KubrikoColors.secondaryLight,
    onSecondary = KubrikoColors.onSecondaryLight,
    secondaryContainer = KubrikoColors.secondaryContainerLight,
    onSecondaryContainer = KubrikoColors.onSecondaryContainerLight,
    tertiary = KubrikoColors.tertiaryLight,
    onTertiary = KubrikoColors.onTertiaryLight,
    tertiaryContainer = KubrikoColors.tertiaryContainerLight,
    onTertiaryContainer = KubrikoColors.onTertiaryContainerLight,
    error = KubrikoColors.errorLight,
    onError = KubrikoColors.onErrorLight,
    errorContainer = KubrikoColors.errorContainerLight,
    onErrorContainer = KubrikoColors.onErrorContainerLight,
    background = KubrikoColors.backgroundLight,
    onBackground = KubrikoColors.onBackgroundLight,
    surface = KubrikoColors.surfaceLight,
    onSurface = KubrikoColors.onSurfaceLight,
    surfaceVariant = KubrikoColors.surfaceVariantLight,
    onSurfaceVariant = KubrikoColors.onSurfaceVariantLight,
    outline = KubrikoColors.outlineLight,
    outlineVariant = KubrikoColors.outlineVariantLight,
    scrim = KubrikoColors.scrimLight,
    inverseSurface = KubrikoColors.inverseSurfaceLight,
    inverseOnSurface = KubrikoColors.inverseOnSurfaceLight,
    inversePrimary = KubrikoColors.inversePrimaryLight,
    surfaceDim = KubrikoColors.surfaceDimLight,
    surfaceBright = KubrikoColors.surfaceBrightLight,
    surfaceContainerLowest = KubrikoColors.surfaceContainerLowestLight,
    surfaceContainerLow = KubrikoColors.surfaceContainerLowLight,
    surfaceContainer = KubrikoColors.surfaceContainerLight,
    surfaceContainerHigh = KubrikoColors.surfaceContainerHighLight,
    surfaceContainerHighest = KubrikoColors.surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = KubrikoColors.primaryDark,
    onPrimary = KubrikoColors.onPrimaryDark,
    primaryContainer = KubrikoColors.primaryContainerDark,
    onPrimaryContainer = KubrikoColors.onPrimaryContainerDark,
    secondary = KubrikoColors.secondaryDark,
    onSecondary = KubrikoColors.onSecondaryDark,
    secondaryContainer = KubrikoColors.secondaryContainerDark,
    onSecondaryContainer = KubrikoColors.onSecondaryContainerDark,
    tertiary = KubrikoColors.tertiaryDark,
    onTertiary = KubrikoColors.onTertiaryDark,
    tertiaryContainer = KubrikoColors.tertiaryContainerDark,
    onTertiaryContainer = KubrikoColors.onTertiaryContainerDark,
    error = KubrikoColors.errorDark,
    onError = KubrikoColors.onErrorDark,
    errorContainer = KubrikoColors.errorContainerDark,
    onErrorContainer = KubrikoColors.onErrorContainerDark,
    background = KubrikoColors.backgroundDark,
    onBackground = KubrikoColors.onBackgroundDark,
    surface = KubrikoColors.surfaceDark,
    onSurface = KubrikoColors.onSurfaceDark,
    surfaceVariant = KubrikoColors.surfaceVariantDark,
    onSurfaceVariant = KubrikoColors.onSurfaceVariantDark,
    outline = KubrikoColors.outlineDark,
    outlineVariant = KubrikoColors.outlineVariantDark,
    scrim = KubrikoColors.scrimDark,
    inverseSurface = KubrikoColors.inverseSurfaceDark,
    inverseOnSurface = KubrikoColors.inverseOnSurfaceDark,
    inversePrimary = KubrikoColors.inversePrimaryDark,
    surfaceDim = KubrikoColors.surfaceDimDark,
    surfaceBright = KubrikoColors.surfaceBrightDark,
    surfaceContainerLowest = KubrikoColors.surfaceContainerLowestDark,
    surfaceContainerLow = KubrikoColors.surfaceContainerLowDark,
    surfaceContainer = KubrikoColors.surfaceContainerDark,
    surfaceContainerHigh = KubrikoColors.surfaceContainerHighDark,
    surfaceContainerHighest = KubrikoColors.surfaceContainerHighestDark,
)